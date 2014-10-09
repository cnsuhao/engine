package org.ovirt.engine.core.vdsbroker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.businessentities.NonOperationalReason;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSDomainsData;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VdsDynamic;
import org.ovirt.engine.core.common.businessentities.VdsStatistics;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.vdscommands.SetVdsStatusVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.VdsIdAndVdsVDSCommandParametersBase;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.FileUtil;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.timer.OnTimerMethodAnnotation;
import org.ovirt.engine.core.utils.timer.SchedulerUtil;
import org.ovirt.engine.core.utils.timer.SchedulerUtilQuartzImpl;
import org.ovirt.engine.core.vdsbroker.irsbroker.IRSErrorException;
import org.ovirt.engine.core.vdsbroker.irsbroker.IrsBrokerCommand;
import org.ovirt.engine.core.vdsbroker.vdsbroker.CollectVdsNetworkDataVDSCommand;
import org.ovirt.engine.core.vdsbroker.vdsbroker.GetCapabilitiesVDSCommand;
import org.ovirt.engine.core.vdsbroker.vdsbroker.IVdsServer;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VDSNetworkException;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VDSRecoveringException;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VdsServerConnector;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VdsServerWrapper;
import org.ovirt.engine.core.vdsbroker.xmlrpc.XmlRpcUtils;

public class VdsManager {
    private VDS _vds;
    private long lastUpdate;
    private long updateStartTime;

    private static Log log = LogFactory.getLog(VdsManager.class);

    public boolean getRefreshStatistics() {
        return (_refreshIteration == _numberRefreshesBeforeSave);
    }

    private static final int VDS_REFRESH_RATE = Config.<Integer> GetValue(ConfigValues.VdsRefreshRate) * 1000;

    private String onTimerJobId;

    private final int _numberRefreshesBeforeSave = Config.<Integer> GetValue(ConfigValues.NumberVmRefreshesBeforeSave);
    private int _refreshIteration = 1;

    private final Object _lockObj = new Object();
    private static Map<Guid, String> recoveringJobIdMap = new ConcurrentHashMap<Guid, String>();
    private boolean isSetNonOperationalExecuted;
    private MonitoringStrategy monitoringStrategy;
    public Object getLockObj() {
        return _lockObj;
    }

    public static void cancelRecoveryJob(Guid vdsId) {
        String jobId = recoveringJobIdMap.remove(vdsId);
        if (jobId != null) {
            log.infoFormat("Cancelling the recovery from crash timer for VDS {0} because vds started initializing", vdsId);
            try {
                SchedulerUtilQuartzImpl.getInstance().deleteJob(jobId);
            } catch (Exception e) {
                log.warnFormat("Failed deleting job {0} at cancelRecoveryJob", jobId);
            }
        }
    }

    private final AtomicInteger mFailedToRunVmAttempts;
    private final AtomicInteger mUnrespondedAttempts;

    private static final int VDS_DURING_FAILURE_TIMEOUT_IN_MINUTES = Config
            .<Integer> GetValue(ConfigValues.TimeToReduceFailedRunOnVdsInMinutes);
    private String duringFailureJobId;
    private boolean privateInitialized;

    public boolean getInitialized() {
        return privateInitialized;
    }

    public void setInitialized(boolean value) {
        privateInitialized = value;
    }

    private IVdsServer _vdsProxy;

    public IVdsServer getVdsProxy() {
        return _vdsProxy;
    }

    public Guid getVdsId() {
        return _vdsId;
    }

    private boolean mBeforeFirstRefresh = true;

    public boolean getbeforeFirstRefresh() {
        return mBeforeFirstRefresh;
    }

    public void setbeforeFirstRefresh(boolean value) {
        mBeforeFirstRefresh = value;
    }

    private final Guid _vdsId;

    private VdsManager(VDS vds) {
        _vds = vds;
        _vdsId = vds.getId();
        monitoringStrategy = MonitoringStrategyFactory.getMonitoringStrategyForVds(vds);
        mUnrespondedAttempts = new AtomicInteger();
        mFailedToRunVmAttempts = new AtomicInteger();
        log.info("Eneterd VdsManager:constructor");
        if (_vds.getstatus() == VDSStatus.PreparingForMaintenance) {
            _vds.setprevious_status(_vds.getstatus());
        } else {
            _vds.setprevious_status(VDSStatus.Up);
        }
        // if ssl is on and no certificate file
        if (Config.<Boolean> GetValue(ConfigValues.UseSecureConnectionWithServers)
                    && !FileUtil.fileExists(Config.resolveCertificatePath())) {
            if (_vds.getstatus() != VDSStatus.Maintenance && _vds.getstatus() != VDSStatus.InstallFailed) {
                setStatus(VDSStatus.NonResponsive, _vds);
                UpdateDynamicData(_vds.getDynamicData());
            }
            log.error("Could not find VDC Certificate file.");
            AuditLogableBase logable = new AuditLogableBase(_vdsId);
            AuditLogDirector.log(logable, AuditLogType.CERTIFICATE_FILE_NOT_FOUND);
        }
        InitVdsBroker();
        _vds = null;

    }

    public static VdsManager buildVdsManager(VDS vds) {
        VdsManager vdsManager = new VdsManager(vds);
        return vdsManager;
    }

    public void schedulJobs() {
        SchedulerUtil sched = SchedulerUtilQuartzImpl.getInstance();
        duringFailureJobId = sched.scheduleAFixedDelayJob(this, "OnVdsDuringFailureTimer", new Class[0],
                    new Object[0], VDS_DURING_FAILURE_TIMEOUT_IN_MINUTES, VDS_DURING_FAILURE_TIMEOUT_IN_MINUTES,
                    TimeUnit.MINUTES);
        sched.pauseJob(duringFailureJobId);
        // start with refresh statistics
        _refreshIteration = _numberRefreshesBeforeSave - 1;

        onTimerJobId = sched.scheduleAFixedDelayJob(this, "OnTimer", new Class[0], new Object[0], VDS_REFRESH_RATE,
                VDS_REFRESH_RATE, TimeUnit.MILLISECONDS);
    }

    private void InitVdsBroker() {
        log.infoFormat("vdsBroker({0},{1})", _vds.gethost_name(), _vds.getport());
        int clientTimeOut = Config.<Integer> GetValue(ConfigValues.vdsTimeout) * 1000;
        Pair<VdsServerConnector, HttpClient> returnValue =
                XmlRpcUtils.getConnection(_vds.gethost_name(),
                        _vds.getport(),
                        clientTimeOut,
                        VdsServerConnector.class,
                        Config.<Boolean> GetValue(ConfigValues.UseSecureConnectionWithServers));
        _vdsProxy = new VdsServerWrapper(returnValue.getFirst(), returnValue.getSecond());
    }

    public void UpdateVmDynamic(VmDynamic vmDynamic) {
        DbFacade.getInstance().getVmDynamicDao().update(vmDynamic);
    }

    private VdsUpdateRunTimeInfo _vdsUpdater;
    private final VdsMonitor vdsMonitor = new VdsMonitor();

    @OnTimerMethodAnnotation("OnTimer")
    public void OnTimer() {
        try {
            setIsSetNonOperationalExecuted(false);
            Guid vdsId = null;
            Guid storagePoolId = null;
            String vdsName = null;
            ArrayList<VDSDomainsData> domainsList = null;

            synchronized (getLockObj()) {
                _vds = DbFacade.getInstance().getVdsDao().get(getVdsId());
                if (_vds == null) {
                    log.errorFormat("VdsManager::refreshVdsRunTimeInfo - OnTimer is NULL for {0}",
                            getVdsId());
                    return;
                }

                try {
                    if (_refreshIteration == _numberRefreshesBeforeSave) {
                        _refreshIteration = 1;
                    } else {
                        _refreshIteration++;
                    }
                    if (isMonitoringNeeded()) {
                        setStartTime();
                        _vdsUpdater = new VdsUpdateRunTimeInfo(VdsManager.this, _vds);
                        _vdsUpdater.Refresh();
                        mUnrespondedAttempts.set(0);
                        setLastUpdate();
                    }
                    if (!getInitialized() && _vds.getstatus() != VDSStatus.NonResponsive
                            && _vds.getstatus() != VDSStatus.PendingApproval) {
                        log.infoFormat("Initializing Host: {0}",  _vds.getvds_name());
                        ResourceManager.getInstance().HandleVdsFinishedInit(_vds.getId());
                        setInitialized(true);
                    }
                } catch (VDSNetworkException e) {
                    logNetworkException(e);
                } catch (VDSRecoveringException ex) {
                    HandleVdsRecoveringException(ex);
                } catch (IRSErrorException ex) {
                    logFailureMessage(ex);
                    if (log.isDebugEnabled()) {
                        logException(ex);
                    }
                } catch (RuntimeException ex) {
                    logFailureMessage(ex);
                    logException(ex);
                }
                try {
                    if (_vdsUpdater != null) {
                        _vdsUpdater.AfterRefreshTreatment();

                        // Get vds data for updating domains list, ignoring vds which is down, since it's not connected
                        // to
                        // the storage anymore (so there is no sense in updating the domains list in that case).
                        if (_vds != null && _vds.getstatus() != VDSStatus.Maintenance) {
                            vdsId = _vds.getId();
                            vdsName = _vds.getvds_name();
                            storagePoolId = _vds.getStoragePoolId();
                            domainsList = _vds.getDomains();
                        }
                    }

                    _vds = null;
                    _vdsUpdater = null;
                } catch (IRSErrorException ex) {
                    logAfterRefreshFailureMessage(ex);
                    if (log.isDebugEnabled()) {
                        logException(ex);
                    }
                } catch (RuntimeException ex) {
                    logAfterRefreshFailureMessage(ex);
                    logException(ex);
                }

            }

            // Now update the status of domains, this code should not be in
            // synchronized part of code
            if (domainsList != null) {
                IrsBrokerCommand.UpdateVdsDomainsData(vdsId, vdsName, storagePoolId, domainsList);
            }
        } catch (Exception e) {
            log.error("Timer update runtimeinfo failed. Exception:", e);
        }
    }

    private void logFailureMessage(RuntimeException ex) {
        log.warnFormat(
                "VdsManager::refreshVdsRunTimeInfo::Failed to refresh VDS , vds = {0} : {1}, error = '{2}', continuing.",
                _vds.getId(),
                _vds.getvds_name(),
                ExceptionUtils.getMessage(ex));
    }

    private static void logException(final RuntimeException ex) {
        log.error("ResourceManager::refreshVdsRunTimeInfo", ex);
    }

    private void logAfterRefreshFailureMessage(RuntimeException ex) {
        log.warnFormat(
                "ResourceManager::refreshVdsRunTimeInfo::Failed to AfterRefreshTreatment VDS  error = '{0}', continuing.",
                ExceptionUtils.getMessage(ex));
    }

    public boolean isMonitoringNeeded() {
        return (monitoringStrategy.isMonitoringNeeded(_vds) &&
                _vds.getstatus() != VDSStatus.Installing &&
                _vds.getstatus() != VDSStatus.InstallFailed &&
                _vds.getstatus() != VDSStatus.Reboot &&
                _vds.getstatus() != VDSStatus.Maintenance &&
                _vds.getstatus() != VDSStatus.PendingApproval && _vds.getstatus() != VDSStatus.Down);
    }

    private void HandleVdsRecoveringException(VDSRecoveringException ex) {
        if (_vds.getstatus() != VDSStatus.Initializing && _vds.getstatus() != VDSStatus.NonOperational) {
            setStatus(VDSStatus.Initializing, _vds);
            UpdateDynamicData(_vds.getDynamicData());
            AuditLogableBase logable = new AuditLogableBase(_vds.getId());
            logable.AddCustomValue("ErrorMessage", ex.getMessage());
            AuditLogDirector.log(logable, AuditLogType.VDS_INITIALIZING);
            log.warnFormat(
                    "ResourceManager::refreshVdsRunTimeInfo::Failed to refresh VDS , vds = {0} : {1}, error = {2}, continuing.",
                    _vds.getId(),
                    _vds.getvds_name(),
                    ex.getMessage());
            final int VDS_RECOVERY_TIMEOUT_IN_MINUTES = Config.<Integer> GetValue(ConfigValues.VdsRecoveryTimeoutInMintues);
            String jobId = SchedulerUtilQuartzImpl.getInstance().scheduleAOneTimeJob(this, "onTimerHandleVdsRecovering", new Class[0],
                    new Object[0], VDS_RECOVERY_TIMEOUT_IN_MINUTES, TimeUnit.MINUTES);
            recoveringJobIdMap.put(_vds.getId(), jobId);
        }
    }

    @OnTimerMethodAnnotation("onTimerHandleVdsRecovering")
    public void onTimerHandleVdsRecovering() {
        recoveringJobIdMap.remove(getVdsId());
        VDS vds = DbFacade.getInstance().getVdsDao().get(getVdsId());
        if (vds.getstatus() == VDSStatus.Initializing) {
            try {
                ResourceManager
                            .getInstance()
                            .getEventListener()
                            .vdsNonOperational(vds.getId(),
                                    NonOperationalReason.TIMEOUT_RECOVERING_FROM_CRASH,
                                    true,
                                    true,
                                Guid.Empty);
                setIsSetNonOperationalExecuted(true);
            } catch (RuntimeException exp) {
                log.errorFormat(
                            "HandleVdsRecoveringException::Error in recovery timer treatment, vds = {0} : {1}, error = {2}.",
                            vds.getId(),
                            vds.getvds_name(),
                            exp.getMessage());
            }
        }
    }

    /**
     * Save dynamic data to cache and DB.
     *
     * @param dynamicData
     */
    public void UpdateDynamicData(VdsDynamic dynamicData) {
        DbFacade.getInstance().getVdsDynamicDao().update(dynamicData);
    }

    /**
     * Save statistics data to cache and DB.
     *
     * @param statisticsData
     */
    public void UpdateStatisticsData(VdsStatistics statisticsData) {
        DbFacade.getInstance().getVdsStatisticsDao().update(statisticsData);
    }

    public void activate() {
        VDS vds = null;
        try {
            // refresh vds from db in case changed while was down
            if (log.isDebugEnabled()) {
                log.debugFormat(
                        "ResourceManager::activateVds - trying to activate host {0} , meanwhile setting status to Unassigned meanwhile",
                        getVdsId());
            }
            vds = DbFacade.getInstance().getVdsDao().get(getVdsId());
            /**
             * refresh capabilities
             */
            VDSStatus newStatus = refreshCapabilities(new AtomicBoolean(), vds);
            if (log.isDebugEnabled()) {
                log.debugFormat(
                        "ResourceManager::activateVds - success to refreshCapabilities for host {0} , new status will be {1} ",
                        getVdsId(),
                        newStatus);
            }
        } catch (java.lang.Exception e) {
            log.infoFormat("ResourceManager::activateVds - failed to get VDS = {0} capabilities with error: {1}.",
                    getVdsId(), e.getMessage());
            log.infoFormat("ResourceManager::activateVds - failed to activate VDS = {0}", getVdsId());

        } finally {
            if (vds != null) {
                UpdateDynamicData(vds.getDynamicData());

                // Update VDS after testing special hardware capabilities
                monitoringStrategy.processHardwareCapabilities(vds);

                // Always check VdsVersion
                ResourceManager.getInstance().getEventListener().handleVdsVersion(vds.getId());
            }
        }
    }

    public void setStatus(VDSStatus status, VDS vds) {
        synchronized (getLockObj()) {
            if (vds == null) {
                vds = DbFacade.getInstance().getVdsDao().get(getVdsId());
            }
            if (vds.getprevious_status() != vds.getstatus()) {
                vds.setprevious_status(vds.getstatus());
                if (_vds != null) {
                    _vds.setprevious_status(vds.getstatus());
                }
            }
            // update to new status
            vds.setstatus(status);
            if (_vds != null) {
                _vds.setstatus(status);
            }

            switch (status) {
            case NonOperational:
                if (_vds != null) {
                    _vds.setNonOperationalReason(vds.getNonOperationalReason());
                }
                if(vds.getvm_count() > 0) {
                    break;
                }
            case NonResponsive:
            case Down:
            case Maintenance:
                vds.setcpu_sys(Double.valueOf(0));
                vds.setcpu_user(Double.valueOf(0));
                vds.setcpu_idle(Double.valueOf(0));
                vds.setcpu_load(Double.valueOf(0));
                vds.setusage_cpu_percent(0);
                vds.setusage_mem_percent(0);
                vds.setusage_network_percent(0);
                if (_vds != null) {
                    _vds.setcpu_sys(Double.valueOf(0));
                    _vds.setcpu_user(Double.valueOf(0));
                    _vds.setcpu_idle(Double.valueOf(0));
                    _vds.setcpu_load(Double.valueOf(0));
                    _vds.setusage_cpu_percent(0);
                    _vds.setusage_mem_percent(0);
                    _vds.setusage_network_percent(0);
                }
            }
        }
    }

    /**
     * This function called when vds have failed vm attempts one in predefined time. Its increments failure attemts to
     * one
     *
     * @param obj
     * @param arg
     */
    @OnTimerMethodAnnotation("OnVdsDuringFailureTimer")
    public void OnVdsDuringFailureTimer() {
        synchronized (getLockObj()) {
            VDS vds = DbFacade.getInstance().getVdsDao().get(getVdsId());
            /**
             * Disable timer if vds returns from suspitious mode
             */
            if (mFailedToRunVmAttempts.decrementAndGet() == 0) {
                SchedulerUtilQuartzImpl.getInstance().pauseJob(duringFailureJobId);
            }
            /**
             * Move vds to Up status from error
             */
            if (mFailedToRunVmAttempts.get() < Config.<Integer> GetValue(ConfigValues.NumberOfFailedRunsOnVds)
                    && vds.getstatus() == VDSStatus.Error) {
                setStatus(VDSStatus.Up, vds);
                UpdateDynamicData(vds.getDynamicData());
            }
            log.infoFormat("OnVdsDuringFailureTimer of vds {0} entered. Time:{1}. Attemts after{2}", vds.getvds_name(),
                    new java.util.Date(), mFailedToRunVmAttempts);
        }
    }

    public void failedToRunVm(VDS vds) {
        if (mFailedToRunVmAttempts.get() < Config.<Integer> GetValue(ConfigValues.NumberOfFailedRunsOnVds)
                && mFailedToRunVmAttempts.incrementAndGet() >= Config
                        .<Integer> GetValue(ConfigValues.NumberOfFailedRunsOnVds)) {
            ResourceManager.getInstance().runVdsCommand(VDSCommandType.SetVdsStatus,
                    new SetVdsStatusVDSCommandParameters(vds.getId(), VDSStatus.Error));

            SchedulerUtilQuartzImpl.getInstance().resumeJob(duringFailureJobId);
            AuditLogableBase logable = new AuditLogableBase(vds.getId());
            logable.AddCustomValue("Time", Config.<Integer> GetValue(ConfigValues.TimeToReduceFailedRunOnVdsInMinutes)
                    .toString());
            AuditLogDirector.log(logable, AuditLogType.VDS_FAILED_TO_RUN_VMS);
            log.infoFormat("Vds {0} moved to Error mode after {1} attemts. Time: {2}", vds.getvds_name(),
                    mFailedToRunVmAttempts, new java.util.Date());
        }
    }

    /**
     */
    public void SuccededToRunVm(Guid vmId) {
        mUnrespondedAttempts.set(0);
        ResourceManager.getInstance().SuccededToRunVm(vmId, _vds.getId());
    }

    public VDSStatus refreshCapabilities(AtomicBoolean processHardwareCapsNeeded, VDS vds) {
        log.debug("refreshCapabilities:GetCapabilitiesVDSCommand started method");
        MonitoringStrategy vdsMonitoringStrategy = MonitoringStrategyFactory.getMonitoringStrategyForVds(vds);
        VDS oldVDS = vds.clone();
        GetCapabilitiesVDSCommand vdsBrokerCommand = new GetCapabilitiesVDSCommand(
                new VdsIdAndVdsVDSCommandParametersBase(vds));
        vdsBrokerCommand.Execute();
        if (vdsBrokerCommand.getVDSReturnValue().getSucceeded()) {
            // Verify version capabilities
            HashSet<Version> hostVersions = null;
            Version clusterCompatibility = vds.getvds_group_compatibility_version();
            if (FeatureSupported.hardwareInfo(clusterCompatibility) &&
                // If the feature is enabled in cluster level, we continue by verifying that this VDS also
                // supports the specific cluster level. Otherwise getHardwareInfo API won't exist for the
                // host and an exception will be raised by vdsm.
                (hostVersions = vds.getSupportedClusterVersionsSet()) != null &&
                hostVersions.contains(clusterCompatibility)) {
                VDSReturnValue ret = ResourceManager.getInstance().runVdsCommand(VDSCommandType.GetHardwareInfo,
                        new VdsIdAndVdsVDSCommandParametersBase(vds));
                if (!ret.getSucceeded()) {
                    AuditLogableBase logable = new AuditLogableBase(vds.getId());
                    AuditLogDirector.log(logable, AuditLogType.VDS_FAILED_TO_GET_HOST_HARDWARE_INFO);
                }
            }

            VDSStatus returnStatus = vds.getstatus();
            boolean isSetNonOperational = CollectVdsNetworkDataVDSCommand.persistAndEnforceNetworkCompliance(vds);
            if (isSetNonOperational) {
                setIsSetNonOperationalExecuted(true);
            }

            if (isSetNonOperational && returnStatus != VDSStatus.NonOperational) {
                if (log.isDebugEnabled()) {
                    log.debugFormat(
                            "refreshCapabilities:GetCapabilitiesVDSCommand vds {0} networks  not match it's cluster networks, vds will be moved to NonOperational",
                            vds.getStaticData().getId());
                }
                vds.setstatus(VDSStatus.NonOperational);
                vds.setNonOperationalReason(NonOperationalReason.NETWORK_UNREACHABLE);
                returnStatus = vds.getstatus();
            }

            // We process the software capabilities.
            VDSStatus oldStatus = vds.getstatus();
            vdsMonitoringStrategy.processSoftwareCapabilities(vds);
            returnStatus = vds.getstatus();

            if (returnStatus != oldStatus && returnStatus == VDSStatus.NonOperational) {
                setIsSetNonOperationalExecuted(true);
            }

            processHardwareCapsNeeded.set(monitoringStrategy.processHardwareCapabilitiesNeeded(oldVDS, vds));

            return returnStatus;
        } else if (vdsBrokerCommand.getVDSReturnValue().getExceptionObject() != null) {
            // if exception is VDSNetworkException then call to
            // handleNetworkException
            if (vdsBrokerCommand.getVDSReturnValue().getExceptionObject() instanceof VDSNetworkException
                    && handleNetworkException((VDSNetworkException) vdsBrokerCommand.getVDSReturnValue()
                            .getExceptionObject(), vds)) {
                UpdateDynamicData(vds.getDynamicData());
                UpdateStatisticsData(vds.getStatisticsData());
            }
            throw vdsBrokerCommand.getVDSReturnValue().getExceptionObject();
        } else {
            log.errorFormat("refreshCapabilities:GetCapabilitiesVDSCommand failed with no exception!");
            throw new RuntimeException(vdsBrokerCommand.getVDSReturnValue().getExceptionString());
        }
    }

    /**
     * Handle network exception, return true if save vdsDynamic to DB is needed.
     *
     * @param ex
     * @return
     */
    public boolean handleNetworkException(VDSNetworkException ex, VDS vds) {
        if (vds.getstatus() != VDSStatus.Down) {
            if (mUnrespondedAttempts.get() < Config.<Integer> GetValue(ConfigValues.VDSAttemptsToResetCount)
                    || lastUpdate
                            + (TimeUnit.SECONDS.toMillis(Config.<Integer> GetValue(ConfigValues.TimeoutToResetVdsInSeconds))) > System.currentTimeMillis()) {
                boolean result = false;
                if (vds.getstatus() != VDSStatus.Connecting && vds.getstatus() != VDSStatus.PreparingForMaintenance
                        && vds.getstatus() != VDSStatus.NonResponsive) {
                    setStatus(VDSStatus.Connecting, vds);
                    result = true;
                }
                mUnrespondedAttempts.incrementAndGet();
                return result;
            }

            if (vds.getstatus() == VDSStatus.NonResponsive || vds.getstatus() == VDSStatus.Maintenance) {
                // clearNotRespondingVds();
                setStatus(VDSStatus.NonResponsive, vds);
                return true;
            }
            setStatus(VDSStatus.NonResponsive, vds);
            log.errorFormat(
                    "VDS::handleNetworkException Server failed to respond,  vds_id = {0}, vds_name = {1}, error = {2}",
                    vds.getId(), vds.getvds_name(), ex.getMessage());

            AuditLogableBase logable = new AuditLogableBase(vds.getId());
            AuditLogDirector.log(logable, AuditLogType.VDS_FAILURE);
            ResourceManager.getInstance().getEventListener().vdsNotResponding(vds);
        }
        return true;
    }

    public void dispose() {
        log.info("vdsManager::disposing");
        SchedulerUtilQuartzImpl.getInstance().deleteJob(onTimerJobId);
        XmlRpcUtils.shutDownConnection(((VdsServerWrapper) _vdsProxy).getHttpClient());
    }

    /**
     * Log the network exception depending on the VDS status.
     *
     * @param e
     *            The exception to log.
     */
    private void logNetworkException(VDSNetworkException e) {
        switch (_vds.getstatus()) {
        case Down:
            break;
        case NonResponsive:
            log.debugFormat(
                    "ResourceManager::refreshVdsRunTimeInfo::Failed to refresh VDS , vds = {0} : {1}, VDS Network Error, continuing.\n{2}",
                    _vds.getId(),
                    _vds.getvds_name(),
                    e.getMessage());
            break;
        default:
            log.warnFormat(
                    "ResourceManager::refreshVdsRunTimeInfo::Failed to refresh VDS , vds = {0} : {1}, VDS Network Error, continuing.\n{2}",
                    _vds.getId(),
                    _vds.getvds_name(),
                    e.getMessage());
        }
    }

    public void setIsSetNonOperationalExecuted(boolean isExecuted) {
        this.isSetNonOperationalExecuted = isExecuted;
    }

    public boolean isSetNonOperationalExecuted() {
        return isSetNonOperationalExecuted;
    }

    private void setStartTime() {
        updateStartTime = System.currentTimeMillis();
    }
    private void setLastUpdate() {
        lastUpdate = System.currentTimeMillis();
    }

    /**
     * @return elapsed time in milliseconds it took to update the Host run-time info. 0 means the updater never ran.
     */
    public long getLastUpdateElapsed() {
        return lastUpdate - updateStartTime;
    }

    /**
     * @return VdsMonitor a class with means for lock and conditions for signaling
     */
    public VdsMonitor getVdsMonitor() {
        return vdsMonitor;
    }

}
