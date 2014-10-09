package org.ovirt.engine.core.vdsbroker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.BusinessEntity;
import org.ovirt.engine.core.common.businessentities.DiskImageDynamic;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VdsDynamic;
import org.ovirt.engine.core.common.businessentities.VdsStatistics;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.VmExitStatus;
import org.ovirt.engine.core.common.businessentities.VmGuestAgentInterface;
import org.ovirt.engine.core.common.businessentities.VmPauseStatus;
import org.ovirt.engine.core.common.businessentities.VmStatistics;
import org.ovirt.engine.core.common.businessentities.network.InterfaceStatus;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkStatus;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkStatistics;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkStatistics;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.utils.VmDeviceCommonUtils;
import org.ovirt.engine.core.common.utils.VmDeviceType;
import org.ovirt.engine.core.common.vdscommands.DestroyVmVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.FullListVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.GetVmStatsVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VdsIdAndVdsVDSCommandParametersBase;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.RefObject;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dao.MassOperationsDao;
import org.ovirt.engine.core.utils.NetworkUtils;
import org.ovirt.engine.core.utils.ObjectIdentityChecker;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;
import org.ovirt.engine.core.vdsbroker.irsbroker.IRSErrorException;
import org.ovirt.engine.core.vdsbroker.vdsbroker.DestroyVDSCommand;
import org.ovirt.engine.core.vdsbroker.vdsbroker.FullListVdsCommand;
import org.ovirt.engine.core.vdsbroker.vdsbroker.GetAllVmStatsVDSCommand;
import org.ovirt.engine.core.vdsbroker.vdsbroker.GetStatsVDSCommand;
import org.ovirt.engine.core.vdsbroker.vdsbroker.GetVmStatsVDSCommand;
import org.ovirt.engine.core.vdsbroker.vdsbroker.ListVDSCommand;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VDSErrorException;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VDSNetworkException;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VDSProtocolException;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VDSRecoveringException;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VdsBrokerCommand;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VdsProperties;
import org.ovirt.engine.core.vdsbroker.vdsbroker.entities.VmInternalData;
import org.ovirt.engine.core.vdsbroker.xmlrpc.XmlRpcStruct;

@SuppressWarnings({ "synthetic-access", "unchecked" })
public class VdsUpdateRunTimeInfo {
    private Map<Guid, VmInternalData> _runningVms;
    private final Map<Guid, VmDynamic> _vmDynamicToSave = new HashMap<Guid, VmDynamic>();
    private final Map<Guid, VmStatistics> _vmStatisticsToSave = new HashMap<Guid, VmStatistics>();
    private final Map<Guid, List<VmNetworkInterface>> _vmInterfaceStatisticsToSave =
            new HashMap<Guid, List<VmNetworkInterface>>();
    private final Map<Guid, DiskImageDynamic> _vmDiskImageDynamicToSave =
            new HashMap<Guid, DiskImageDynamic>();
    private final Map<VmDeviceId, VmDevice> vmDeviceToSave = new HashMap<VmDeviceId, VmDevice>();
    private final List<VmDevice> newVmDevices = new ArrayList<VmDevice>();
    private final List<VmDeviceId> removedDeviceIds = new ArrayList<VmDeviceId>();
    private final Map<VM, VmDynamic> _vmsClientIpChanged = new HashMap<VM, VmDynamic>();
    private final Map<Guid, List<VmGuestAgentInterface>> vmGuestAgentNics =
            new HashMap<Guid, List<VmGuestAgentInterface>>();
    private final List<VmDynamic> _poweringUpVms = new ArrayList<VmDynamic>();
    private final List<Guid> _vmsToRerun = new ArrayList<Guid>();
    private final List<Guid> _autoVmsToRun = new ArrayList<Guid>();
    private final List<Guid> _vmsMovedToDown = new ArrayList<Guid>();
    private final List<Guid> _vmsToRemoveFromAsync = new ArrayList<Guid>();
    private final List<Guid> _succededToRunVms = new ArrayList<Guid>();
    private boolean _saveVdsDynamic;
    private VDSStatus _firstStatus = VDSStatus.forValue(0);
    private boolean _saveVdsStatistics;
    private final VdsManager _vdsManager;
    private final MonitoringStrategy monitoringStrategy;
    private final VDS _vds;
    private final Map<Guid, VM> _vmDict;
    private boolean processHardwareCapsNeeded;
    private boolean refreshedCapabilities = false;
    private static Map<Guid, Long> hostDownTimes = new HashMap<Guid, Long>();
    private int runningVmsInTransition = 0;

    private static final Log log = LogFactory.getLog(VdsUpdateRunTimeInfo.class);

    private void saveDataToDb() {
        if (_saveVdsDynamic) {
            _vdsManager.UpdateDynamicData(_vds.getDynamicData());
        }

        if (_saveVdsStatistics) {
            VdsStatistics stat = _vds.getStatisticsData();
            _vdsManager.UpdateStatisticsData(stat);
            checkVdsMemoryThreshold(stat);

            final List<VdsNetworkStatistics> statistics = new LinkedList<VdsNetworkStatistics>();
            for (VdsNetworkInterface iface : _vds.getInterfaces()) {
                statistics.add(iface.getStatistics());
            }
            if (!statistics.isEmpty()) {
                TransactionSupport.executeInScope(TransactionScopeOption.Required,
                        new TransactionMethod<Void>() {

                            @Override
                            public Void runInTransaction() {
                                getDbFacade().getInterfaceDao().massUpdateStatisticsForVds(statistics);
                                return null;
                            }
                        });
            }
        }

        updateAllInTransaction(_vmDynamicToSave.values(), getDbFacade().getVmDynamicDao());
        updateAllInTransaction(_vmStatisticsToSave.values(), getDbFacade().getVmStatisticsDao());

        final List<VmNetworkStatistics> allVmInterfaceStatistics = new LinkedList<VmNetworkStatistics>();
        for (List<VmNetworkInterface> list : _vmInterfaceStatisticsToSave.values()) {
            for (VmNetworkInterface iface : list) {
                allVmInterfaceStatistics.add(iface.getStatistics());
            }
        }

        updateAllInTransaction(allVmInterfaceStatistics, getDbFacade().getVmNetworkStatisticsDao());
        updateAllInTransaction(_vmDiskImageDynamicToSave.values(), getDbFacade().getDiskImageDynamicDao());
        saveVmDevicesToDb();
        saveVmGuestAgentNetworkDevices();
    }

    private void saveVmGuestAgentNetworkDevices() {
        if (!vmGuestAgentNics.isEmpty()) {
            TransactionSupport.executeInScope(TransactionScopeOption.Required,
                    new TransactionMethod<Void>() {
                        @Override
                        public Void runInTransaction() {
                            for (Guid vmId : vmGuestAgentNics.keySet()) {
                                getDbFacade().getVmGuestAgentInterfaceDao().removeAllForVm(vmId);
                            }

                            for (List<VmGuestAgentInterface> nics : vmGuestAgentNics.values()) {
                                if (nics != null) {
                                    for (VmGuestAgentInterface nic : nics) {
                                        getDbFacade().getVmGuestAgentInterfaceDao().save(nic);
                                    }
                                }
                            }
                            return null;
                        }
                    });
        }
    }

    private void saveVmDevicesToDb() {
        updateAllInTransaction("UpdateVmDeviceRuntimeInfo", vmDeviceToSave.values(), getDbFacade().getVmDeviceDao());

        if (!removedDeviceIds.isEmpty()) {
            TransactionSupport.executeInScope(TransactionScopeOption.Required,
                    new TransactionMethod<Void>() {
                        @Override
                        public Void runInTransaction() {
                            getDbFacade().getVmDeviceDao().removeAll(removedDeviceIds);
                            return null;
                        }
                    });
        }

        if (!newVmDevices.isEmpty()) {
            TransactionSupport.executeInScope(TransactionScopeOption.Required,
                    new TransactionMethod<Void>() {

                        @Override
                        public Void runInTransaction() {
                            getDbFacade().getVmDeviceDao().saveAll(newVmDevices);
                            return null;
                        }
                    });
        }
    }

    /**
     * Update all the given entities in a transaction, so that a new connection/transaction won't be opened for each
     * entity update.
     *
     * @param <T>
     *            The type of entity.
     * @param entities
     *            The entities to update.
     * @param dao
     *            The DAO used for updating.
     */
    private static <T extends BusinessEntity<ID> & Comparable<T>, ID extends Serializable & Comparable<? super ID>> void updateAllInTransaction(final Collection<T> entities, final MassOperationsDao<T, ID> dao) {
        updateAllInTransaction(null, entities,dao);
    }

    /**
     * Update all the given entities in a transaction, so that a new connection/transaction won't be opened for each
     * entity update.
     *
     * @param <T>
     *            The type of entity.
     * @param procedureName
     *            The name of stored procedure to use for update
     * @param entities
     *            The entities to update.
     * @param dao
     *            The DAO used for updating.
     */

    private static <T extends BusinessEntity<ID> & Comparable<T>, ID extends Serializable & Comparable<? super ID>> void updateAllInTransaction
        (final String procedureName, final Collection<T> entities, final MassOperationsDao<T, ID> dao) {
    final List<T> sortedList = new ArrayList<T>(entities);
    Collections.sort(sortedList);
    if (!entities.isEmpty()) {
        TransactionSupport.executeInScope(TransactionScopeOption.Required,
            new TransactionMethod<Void>() {

                @Override
                public Void runInTransaction() {
                            dao.updateAll(procedureName, sortedList);
                    return null;
                }
            });
    }
}


    /**
     * check if value is less than configurable threshold , if yes , generated event log message
     *
     * @param stat
     */
    private void checkVdsMemoryThreshold(VdsStatistics stat) {

        Integer threshold = Config.GetValue(ConfigValues.LogPhysicalMemoryThresholdInMB);
        if (stat.getmem_available() < threshold) {
            AuditLogableBase logable = new AuditLogableBase();
            logable.setVdsId(stat.getId());
            logable.AddCustomValue("HostName", _vds.getvds_name());
            logable.AddCustomValue("AvailableMemory", stat.getmem_available().toString());
            logable.AddCustomValue("Threshold", threshold.toString());
            auditLog(logable, AuditLogType.VDS_LOW_MEM);
        }
    }

    public VdsUpdateRunTimeInfo(VdsManager vdsManager, VDS vds) {
        _vdsManager = vdsManager;
        _vds = vds;
        _firstStatus = _vds.getstatus();
        monitoringStrategy = getMonitoringStrategyForVds(vds);
        _vmDict = getDbFacade().getVmDao().getAllRunningByVds(_vds.getId());

        for (VM vm : _vmDict.values()) {
            if (vm.isRunning() && vm.getStatus() != VMStatus.Up) {
                runningVmsInTransition++;
            }
        }
    }

    protected MonitoringStrategy getMonitoringStrategyForVds(VDS vds) {
        return MonitoringStrategyFactory.getMonitoringStrategyForVds(vds);
    }

    public void Refresh() {
        try {
            refreshVdsRunTimeInfo();
        } finally {
            try {
                if (_firstStatus != _vds.getstatus() && _vds.getstatus() == VDSStatus.Up) {
                    // use this lock in order to allow only one host updating DB and
                    // calling UpEvent in a time
                    VdsManager.cancelRecoveryJob(_vds.getId());
                    if (log.isDebugEnabled()) {
                        log.debugFormat("vds {0}-{1} firing up event.", _vds.getId(), _vds.getvds_name());
                    }
                    _vdsManager.setIsSetNonOperationalExecuted(!ResourceManager.getInstance()
                            .getEventListener()
                            .vdsUpEvent(_vds.getId()));

                    // Check hardware capabilities in case VDS moved to up
                    processHardwareCapsNeeded = true;
                }
                // save all data to db
                saveDataToDb();
            } catch (IRSErrorException ex) {
                logFailureMessage("ResourceManager::refreshVdsRunTimeInfo:", ex);
                if (log.isDebugEnabled()) {
                    log.error(ExceptionUtils.getMessage(ex), ex);
                }
            } catch (RuntimeException ex) {
                logFailureMessage("ResourceManager::refreshVdsRunTimeInfo:", ex);
                log.error(ExceptionUtils.getMessage(ex), ex);
            }
        }
    }

    private void logFailureMessage(String messagePrefix, RuntimeException ex) {
        log.errorFormat("{0} Error: {1}, vds = {2} : {3}",
                messagePrefix,
                ExceptionUtils.getMessage(ex),
                _vds.getId(),
                _vds.getvds_name());
    }

    public void AfterRefreshTreatment() {
        try {
            if (processHardwareCapsNeeded) {
                monitoringStrategy.processHardwareCapabilities(_vds);
                markIsSetNonOperationalExecuted();
            }

            if (refreshedCapabilities) {
                ResourceManager.getInstance().getEventListener().handleVdsVersion(_vds.getId());
                markIsSetNonOperationalExecuted();
            }

            if (_vds.getstatus() == VDSStatus.Maintenance) {
                try {
                    ResourceManager.getInstance().getEventListener().vdsMovedToMaintanance(_vds);
                } catch (RuntimeException ex) {
                    log.errorFormat("Host encounter a problem moving to maintenance mode, probably error during disconnecting it from pool {0}. The Host will stay in Maintenance",
                            ex.getMessage());
                }
            } else if (_vds.getstatus() == VDSStatus.NonOperational && _firstStatus != VDSStatus.NonOperational) {

                if (!_vdsManager.isSetNonOperationalExecuted()) {
                    ResourceManager
                            .getInstance()
                            .getEventListener()
                            .vdsNonOperational(_vds.getId(), _vds.getNonOperationalReason(), false, false,
                                    Guid.Empty);
                } else {
                    log.infoFormat("Host {0} : {1} is already in NonOperational status. SetNonOperationalVds command is skipped.",
                            _vds.getId(),
                            _vds.getvds_name());
                }
            }
            // rerun all vms from rerun list
            for (Guid vm_guid : _vmsToRerun) {
                log.errorFormat("Rerun vm {0}. Called from vds {1}", vm_guid, _vds.getvds_name());
                ResourceManager.getInstance().RerunFailedCommand(vm_guid, _vds.getId());

            }
            for (Guid vm_guid : _succededToRunVms) {
                _vdsManager.SuccededToRunVm(vm_guid);
            }
            // run all vms that crushed that marked with auto startup
            for (Guid vm_guid : _autoVmsToRun) {
                // Refrain from auto-start HA VM during its re-run attempts.
                if (!_vmsToRerun.contains(vm_guid)) {
                    ResourceManager.getInstance().getEventListener().runFailedAutoStartVM(vm_guid);
                }
            }

            // process all vms that their ip changed.
            for (Entry<VM, VmDynamic> pair : _vmsClientIpChanged.entrySet()) {
                ResourceManager.getInstance().getEventListener()
                        .processOnClientIpChange(_vds, pair.getValue().getId());
            }

            // process all vms that powering up.
            for (VmDynamic runningVm : _poweringUpVms) {
                ResourceManager
                        .getInstance()
                        .getEventListener()
                        .processOnVmPoweringUp(_vds.getId(), runningVm.getId(), runningVm.getdisplay_ip(),
                                runningVm.getdisplay());
            }

            // process all vms that went down
            for (Guid vm_guid : _vmsMovedToDown) {
                ResourceManager.getInstance().getEventListener().processOnVmStop(vm_guid);
            }
            for (Guid vm_guid : _vmsToRemoveFromAsync) {
                ResourceManager.getInstance().RemoveAsyncRunningVm(vm_guid);
            }
        } catch (IRSErrorException ex) {
            logFailureMessage("ResourceManager::RerunFailedCommand:", ex);
            if (log.isDebugEnabled()) {
                log.error(ExceptionUtils.getMessage(ex), ex);
            }
        } catch (RuntimeException ex) {
            logFailureMessage("ResourceManager::RerunFailedCommand:", ex);
            log.error(ExceptionUtils.getMessage(ex), ex);
        }
    }

    private void markIsSetNonOperationalExecuted() {
        if (!_vdsManager.isSetNonOperationalExecuted()) {
            VdsDynamic vdsDynamic = getDbFacade().getVdsDynamicDao().get(_vds.getId());
            if (vdsDynamic.getstatus() == VDSStatus.NonOperational) {
                _vdsManager.setIsSetNonOperationalExecuted(true);
            }
        }
    }

    private void refreshVdsRunTimeInfo() {
        boolean isVdsUpOrGoingToMaintanance = _vds.getstatus() == VDSStatus.Up
                || _vds.getstatus() == VDSStatus.PreparingForMaintenance || _vds.getstatus() == VDSStatus.Error
                || _vds.getstatus() == VDSStatus.NonOperational;
        try {
            if (isVdsUpOrGoingToMaintanance) {
                // check if its time for statistics refresh
                if (_vdsManager.getRefreshStatistics() || _vds.getstatus() == VDSStatus.PreparingForMaintenance) {
                    refreshVdsStats();
                } else {
                    /**
                     * TODO: Omer if vds team will not implement events to 4.2 please call here to refreshVdsStats -
                     * refresh dynamic data
                     */
                }
            } else {
                // refresh dynamic data
                final AtomicBoolean processHardwareNeededAtomic = new AtomicBoolean();
                VDSStatus refreshReturnStatus = _vdsManager.refreshCapabilities(processHardwareNeededAtomic, _vds);
                processHardwareCapsNeeded = processHardwareNeededAtomic.get();
                refreshedCapabilities = true;
                if (refreshReturnStatus != VDSStatus.NonOperational) {
                    _vdsManager.setStatus(VDSStatus.Up, _vds);
                }
                _saveVdsDynamic = true;
            }
            beforeFirstRefreshTreatment(isVdsUpOrGoingToMaintanance);
            refreshVmStats();
        } catch (VDSRecoveringException e) {
            // if PreparingForMaintenance and vds is in install failed keep to
            // move vds to maintenance
            if (_vds.getstatus() != VDSStatus.PreparingForMaintenance) {
                throw e;
            }
        }
        moveVDSToMaintenanceIfNeeded();
    }

    private void refreshVdsStats() {
        if (Config.<Boolean> GetValue(ConfigValues.DebugTimerLogging)) {
            log.debugFormat("vdsManager::refreshVdsStats entered, vds = {0} : {1}", _vds.getId(),
                    _vds.getvds_name());
        }
        // get statistics data, images checks and vm_count data (dynamic)
        GetStatsVDSCommand<VdsIdAndVdsVDSCommandParametersBase> vdsBrokerCommand =
                new GetStatsVDSCommand<VdsIdAndVdsVDSCommandParametersBase>(new VdsIdAndVdsVDSCommandParametersBase(_vds));
        vdsBrokerCommand.Execute();
        if (!vdsBrokerCommand.getVDSReturnValue().getSucceeded()
                && vdsBrokerCommand.getVDSReturnValue().getExceptionObject() != null) {
            VDSNetworkException ex =
                    (VDSNetworkException) ((vdsBrokerCommand.getVDSReturnValue().getExceptionObject() instanceof VDSNetworkException) ? vdsBrokerCommand
                            .getVDSReturnValue().getExceptionObject()
                            : null);
            if (ex != null) {
                if (_vdsManager.handleNetworkException(ex, _vds)) {
                    _saveVdsDynamic = true;
                }
                log.errorFormat("vds::refreshVdsStats Failed getVdsStats,  vds = {0} : {1}, error = {2}",
                        _vds.getId(), _vds.getvds_name(), ExceptionUtils.getMessage(ex));
            } else {
                log.errorFormat("vds::refreshVdsStats Failed getVdsStats,  vds = {0} : {1}, error = {2}",
                        _vds.getId(), _vds.getvds_name(), vdsBrokerCommand.getVDSReturnValue().getExceptionString());
            }
            throw vdsBrokerCommand.getVDSReturnValue().getExceptionObject();
        }
        // save also dynamic because vm_count data and image_check getting with
        // statistics data
        // TODO: omer- one day remove dynamic save when possible please check if vdsDynamic changed before save
        _saveVdsDynamic = true;
        _saveVdsStatistics = true;

        alertIfLowDiskSpaceOnHost();
        checkVdsInterfaces();

        if (Config.<Boolean> GetValue(ConfigValues.DebugTimerLogging)) {
            log.debugFormat("vds::refreshVdsStats\n{0}", toString());
        }
    }

    /**
     * Log to the audit log in case one/some of the paths monitored by VDSM are low on disk space.
     */
    private void alertIfLowDiskSpaceOnHost() {
        Map<String, Long> disksUsage = _vds.getLocalDisksUsage();
        if (disksUsage == null || disksUsage.isEmpty()) {
            return;
        }

        List<String> disksWithLowSpace = new ArrayList<String>();
        List<String> disksWithCriticallyLowSpace = new ArrayList<String>();
        final int lowSpaceCriticalThreshold =
                Config.<Integer> GetValue(ConfigValues.VdsLocalDisksCriticallyLowFreeSpace);
        final int lowSpaceThreshold =
                Config.<Integer> GetValue(ConfigValues.VdsLocalDisksLowFreeSpace);

        for (Entry<String, Long> diskUsage : disksUsage.entrySet()) {
            if (diskUsage.getValue() != null) {
                if (diskUsage.getValue() <= lowSpaceCriticalThreshold) {
                    disksWithCriticallyLowSpace.add(diskUsage.getKey());
                } else if (diskUsage.getValue() <= lowSpaceThreshold) {
                    disksWithLowSpace.add(diskUsage.getKey());
                }
            }
        }

        logLowDiskSpaceOnHostDisks(disksWithLowSpace, lowSpaceThreshold, AuditLogType.VDS_LOW_DISK_SPACE);
        logLowDiskSpaceOnHostDisks(disksWithCriticallyLowSpace,
                lowSpaceCriticalThreshold,
                AuditLogType.VDS_LOW_DISK_SPACE_ERROR);
    }

    /**
     * Log that the disks have low space, if the disks list is not empty.
     *
     * @param disksWithLowSpace
     *            The disks with the low space.
     * @param lowSpaceThreshold
     *            The low space threshold that below it we log.
     * @param logType
     *            The type of log to use.
     */
    private void logLowDiskSpaceOnHostDisks(List<String> disksWithLowSpace,
            final Integer lowSpaceThreshold,
            AuditLogType logType) {
        if (!disksWithLowSpace.isEmpty()) {
            AuditLogableBase logable = new AuditLogableBase(_vds.getId());
            logable.AddCustomValue("DiskSpace", lowSpaceThreshold.toString());
            logable.AddCustomValue("Disks", StringUtils.join(disksWithLowSpace, ", "));
            auditLog(logable, logType);
        }
    }

    // Check if one of the Host interfaces is down, we set the host to non-operational
    // We cannot have Host that don't have all networks in cluster in status Up
    private void checkVdsInterfaces() {
        if (_vds.getstatus() != VDSStatus.Up) {
            return;
        }
        Map<String, Boolean> bondsWithStatus = new HashMap<String, Boolean>();
        List<Network> clusterNetworks = getDbFacade().getNetworkDao()
                .getAllForCluster(_vds.getvds_group_id());
        List<String> networks = new ArrayList<String>();
        List<String> brokenNics = new ArrayList<String>();
        Map<String, List<String>> bondsWithListOfNics = new HashMap<String, List<String>>();

        List<VdsNetworkInterface> interfaces = _vds.getInterfaces();
        Map<String, Network> networksByName = NetworkUtils.networksByName(clusterNetworks);

        try {
            for (VdsNetworkInterface iface : interfaces) {

                // Handle nics that are non bonded and not vlan over bond
                if (isRequiredInterfaceDown(networksByName, iface)) {
                    brokenNics.add(iface.getName());
                    networks.add(iface.getNetworkName());
                }

                // Handle bond nics
                if (iface.getBondName() != null) {
                    populate(bondsWithStatus, clusterNetworks, networks, bondsWithListOfNics, iface);
                }
            }

            // check the bond statuses, if one is down we set the host to down
            // only if we didn't already set the host to down
            if (brokenNics.isEmpty()) {
                for (String key : bondsWithStatus.keySet()) {
                    if (!bondsWithStatus.get(key)) {
                        // add the nics name for audit log
                        for (String name : bondsWithListOfNics.get(key)) {
                            brokenNics.add(name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(String.format("Failure on checkInterfaces on update runtimeinfo for vds: %s", _vds.getvds_name()),
                    e);
        } finally {
            if (!brokenNics.isEmpty()) {
                // we give 1 minutes to a nic to get up in case the nic get the ip from DHCP server
                if (!hostDownTimes.containsKey(_vds.getId())) {
                    hostDownTimes.put(_vds.getId(), System.currentTimeMillis());
                    return;
                }

                // if less then 1 minutes, still waiting for DHCP
                int delay = Config.<Integer> GetValue(ConfigValues.NicDHCPDelayGraceInMS) * 1000;
                if (System.currentTimeMillis() < hostDownTimes.get(_vds.getId()) + delay) {
                    return;
                }

                // if we could retreive it within the timeout, remove from map (for future checks) and set the host to
                // non-operational
                hostDownTimes.remove(_vds.getId());

                try {
                    StringBuilder sNics = new StringBuilder();
                    StringBuilder sNetworks = new StringBuilder();

                    for (String nic : brokenNics) {
                        sNics.append(nic)
                                .append(", ");
                    }
                    for (String net : networks) {
                        sNetworks.append(net)
                                .append(", ");
                    }

                    String message =
                            String.format(
                                    "Host '%s' moved to Non-Operational state because interface/s '%s' are down which needed by network/s '%s' in the current cluster",
                                    _vds.getvds_name(),
                                    sNics.toString(),
                                    sNetworks.toString());

                    _vdsManager.setStatus(VDSStatus.NonOperational, _vds);
                    log.info(message);

                    AuditLogableBase logable = new AuditLogableBase(_vds.getId());
                    logable.AddCustomValue("Networks", StringHelper.trimEnd(sNetworks.toString(), ',', ' '));
                    logable.AddCustomValue("Interfaces", StringHelper.trimEnd(sNics.toString(), ',', ' '));
                    auditLog(logable, AuditLogType.VDS_SET_NONOPERATIONAL_IFACE_DOWN);
                } catch (Exception e) {
                    log.error(String.format("checkInterface: Failure on moving host: %s to non-operational.",
                            _vds.getvds_name()),
                            e);
                }
            } else {
                // no nics are down, remove from list if exists
                hostDownTimes.remove(_vds.getId());
            }
        }
    }

    private void populate(Map<String, Boolean> bondsWithStatus,
            List<Network> clusterNetworks,
            List<String> networks,
            Map<String, List<String>> bondsWithListOfNics,
            VdsNetworkInterface iface) {
        Pair<Boolean, String> retVal =
                isRequiredNetworkInCluster(iface.getBondName(), clusterNetworks);
        String networkName = retVal.getSecond();
        if (retVal.getFirst()) {
            if (!bondsWithStatus.containsKey(iface.getBondName())) {
                bondsWithStatus.put(iface.getBondName(), false);
            }
            // It is enough for at least one of the interfaces of the bond to be up
            bondsWithStatus.put(iface.getBondName(),
                    bondsWithStatus.get(iface.getBondName())
                            || (iface.getStatistics().getStatus() == InterfaceStatus.UP));

            if (!networks.contains(networkName)
                    && !bondsWithStatus.containsKey(iface.getName())) {
                networks.add(networkName);
            }
            // we remove the network from the audit log if the bond
            // is active
            else if (networks.contains(networkName) && bondsWithStatus.get(iface.getBondName())) {
                networks.remove(networkName);
            }
            if (!bondsWithListOfNics.containsKey(iface.getBondName())) {
                bondsWithListOfNics.put(iface.getBondName(), new ArrayList<String>());
            }
            bondsWithListOfNics.get(iface.getBondName()).add(iface.getName());
        }
    }

    /**
     * check if an interface implementing a required cluster network is down
     *
     * @param networksByName
     * @param iface
     */
    private boolean isRequiredInterfaceDown(Map<String, Network> networksByName, VdsNetworkInterface iface) {
        if (iface.getStatistics().getStatus() != InterfaceStatus.UP
                && iface.getNetworkName() != null
                && iface.getBonded() == null
                && !isBondOrVlanOverBond(iface)
                && networksByName.containsKey(iface.getNetworkName())) {

            Network net = networksByName.get(iface.getNetworkName());
            if (net.getCluster().getStatus() == NetworkStatus.OPERATIONAL && net.getCluster().isRequired()
                    && (iface.getVlanId() == null || !isVlanInterfaceUp(iface))) {
                return true;
            }
        }
        return false;
    }

    // method get bond name, list of cluster network - checks if the specified
    // bonds network is in the clusterNetworks,
    // if so return true and networkName of the bonds
    private Pair<Boolean, String> isRequiredNetworkInCluster(String bondName, List<Network> clusterNetworks) {
        Pair<Boolean, String> retVal = new Pair<Boolean, String>();
        for (VdsNetworkInterface iface : _vds.getInterfaces()) {
            if (iface.getName().equals(bondName)) {
                for (Network net : clusterNetworks) {
                    // If this is the network on the bond, or on a vlan over the bond, and the network is required
                    // we want to check this network
                    if ((net.getName().equals(iface.getNetworkName())
                            || isVlanOverBondNetwork(bondName, net.getName())) && net.getCluster().isRequired()) {
                        retVal.setFirst(true);
                        retVal.setSecond(net.getName());
                        return retVal;
                    }
                }
                retVal.setFirst(false);
                return retVal;
            }
        }
        retVal.setFirst(false);
        return retVal;
    }

    // IsBond return true if the interface is bond,
    // it also check if it's vlan over bond and return true in that case
    // i.e. it return true in case of bond0 and bond0.5
    private boolean isBondOrVlanOverBond(VdsNetworkInterface iface) {
        if (iface.getBonded() != null && iface.getBonded() == true) {
            return true;
        }

        // check if vlan over bond i.e if we are in bond0.5 we look for bond0
        String name = NetworkUtils.getVlanInterfaceName(iface.getName());
        if (name == null) {
            return false;
        }

        for (VdsNetworkInterface i : _vds.getInterfaces()) {
            if (name.equals(i.getName())) {
                return (i.getBonded() != null && i.getBonded() == true);
            }
        }
        return false;
    }

    // function check if vlan over bond connected to network
    // i.e. if we have bond0 that have vlan #5 like:
    // bond0 and bond0.5
    // bond0 is not connectet to network just the bond0.5 is connected to network
    // and this method check for that case
    private boolean isVlanOverBondNetwork(String bondName, String networkName) {
        for (VdsNetworkInterface iface : _vds.getInterfaces()) {
            String name = NetworkUtils.getVlanInterfaceName(iface.getName());
            // this if check if the interface is vlan
            if (name == null) {
                continue;
            } else if (name.equals(bondName)
                    && networkName.equals(iface.getNetworkName())) {
                return true;
            }
        }
        return false;
    }

    // If vlan we search if the interface is up (i.e. not eth2.5 we look for eth2)
    private boolean isVlanInterfaceUp(VdsNetworkInterface vlan) {
        String[] tokens = vlan.getName().split("[.]");
        if (tokens.length == 1) {
            // not vlan
            return true;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length - 1; i++) {
            sb.append(tokens[i])
                    .append(".");
        }
        String ifaceName = StringHelper.trimEnd(sb.toString(), '.');
        for (VdsNetworkInterface iface : _vds.getInterfaces()) {
            if (iface.getName().equals(ifaceName)) {
                return iface.getStatistics().getStatus() == InterfaceStatus.UP;
            }
        }

        // not suppose to get here
        return false;
    }

    private void beforeFirstRefreshTreatment(boolean isVdsUpOrGoingToMaintanance) {
        if (_vdsManager.getbeforeFirstRefresh()) {
            boolean flagsChanged = false;
            final AtomicBoolean processHardwareCapsNeededTemp = new AtomicBoolean();
            _vdsManager.refreshCapabilities(processHardwareCapsNeededTemp, _vds);
            flagsChanged = processHardwareCapsNeededTemp.get();
            _vdsManager.setbeforeFirstRefresh(false);
            refreshedCapabilities = true;
            _saveVdsDynamic = true;
            // change the _cpuFlagsChanged flag only if it was false,
            // because get capabilities is called twice on a new server in same
            // loop!
            processHardwareCapsNeeded = (processHardwareCapsNeeded) ? processHardwareCapsNeeded : flagsChanged;
        } else if (isVdsUpOrGoingToMaintanance || _vds.getstatus() == VDSStatus.Error) {
            return;
        }
        AuditLogableBase logable = new AuditLogableBase(_vds.getId());
        logable.AddCustomValue("VdsStatus", _vds.getstatus().toString());
        auditLog(logable, AuditLogType.VDS_DETECTED);
    }

    private void refreshVmStats() {
        if (Config.<Boolean> GetValue(ConfigValues.DebugTimerLogging)) {
            log.debug("vds::refreshVmList entered");
        }

        VdsBrokerCommand<VdsIdAndVdsVDSCommandParametersBase> command;
        if (!_vdsManager.getRefreshStatistics()) {
            command = new ListVDSCommand<VdsIdAndVdsVDSCommandParametersBase>(
                    new VdsIdAndVdsVDSCommandParametersBase(_vds));
        } else {
            command = new GetAllVmStatsVDSCommand<VdsIdAndVdsVDSCommandParametersBase>(
                    new VdsIdAndVdsVDSCommandParametersBase(_vds));
        }
        _runningVms = (Map<Guid, VmInternalData>) command.ExecuteWithReturnValue();

        if (command.getVDSReturnValue().getSucceeded()) {
            List<VM> running = checkVmsStatusChanged();

            proceedDownVms();

            // update repository and check if there are any vm in cache that not
            // in vdsm
            updateRepository(running);
            // Going over all returned VMs and updting the data structures
            // accordingly

            // checking the db for incoherent vm status;
            // setVmStatusDownForVmNotFound();

            refreshCommitedMemory();
            // Handle VM devices were changed (for 3.1 cluster and above)
            if (!VmDeviceCommonUtils.isOldClusterVersion(_vds.getvds_group_compatibility_version())) {
                handleVmDeviceChange();
            }

            prepareGuestAgentNetworkDevicesForUpdate();

        } else if (command.getVDSReturnValue().getExceptionObject() != null) {
            if (command.getVDSReturnValue().getExceptionObject() instanceof VDSErrorException) {
                log.errorFormat("Failed vds listing,  vds = {0} : {1}, error = {2}", _vds.getId(),
                        _vds.getvds_name(), command.getVDSReturnValue().getExceptionString());
            } else if (command.getVDSReturnValue().getExceptionObject() instanceof VDSNetworkException) {
                _saveVdsDynamic = _vdsManager.handleNetworkException((VDSNetworkException) command.getVDSReturnValue()
                        .getExceptionObject(), _vds);
            } else if (command.getVDSReturnValue().getExceptionObject() instanceof VDSProtocolException) {
                log.errorFormat("Failed vds listing,  vds = {0} : {1}, error = {2}", _vds.getId(),
                        _vds.getvds_name(), command.getVDSReturnValue().getExceptionString());
            }
            throw command.getVDSReturnValue().getExceptionObject();
        } else {
            log.error("GetCapabilitiesVDSCommand failed with no exception!");
        }
    }

    /**
     * Prepare the VM Guest Agent network devices for update. <br>
     * The evaluation of the network devices for update is done by comparing the calculated hash of the network devices
     * from VDSM to the latest hash kept on engine side.
     */
    private void prepareGuestAgentNetworkDevicesForUpdate() {
        for (VmInternalData vmInternalData : _runningVms.values()) {
            VmDynamic vmDynamic = vmInternalData.getVmDynamic();
            if (vmDynamic != null) {
                VM vm = _vmDict.get(vmDynamic.getId());
                if (vm != null) {
                    List<VmGuestAgentInterface> vmGuestAgentInterfaces = vmInternalData.getVmGuestAgentInterfaces();
                    int guestAgentNicHash = vmGuestAgentInterfaces == null ? 0 : vmGuestAgentInterfaces.hashCode();
                    if (guestAgentNicHash != vmDynamic.getGuestAgentNicsHash()) {
                        vmGuestAgentNics.put(vmDynamic.getId(), vmGuestAgentInterfaces);

                        // update new hash value
                        if (_vmDynamicToSave.containsKey(vm.getId())) {
                            updateGuestAgentInterfacesChanges(_vmDynamicToSave.get(vm.getId()),
                                    vmGuestAgentInterfaces,
                                    guestAgentNicHash);
                        } else {
                            updateGuestAgentInterfacesChanges(vmDynamic, vmGuestAgentInterfaces, guestAgentNicHash);
                            addVmDynamicToList(vmDynamic);
                        }
                    }
                }
            }
        }
    }

    private void updateGuestAgentInterfacesChanges(VmDynamic vmDynamic,
            List<VmGuestAgentInterface> vmGuestAgentInterfaces,
            int guestAgentNicHash) {
        vmDynamic.setGuestAgentNicsHash(guestAgentNicHash);
        vmDynamic.setvm_ip(extractVmIpsFromGuestAgentInterfaces(vmGuestAgentInterfaces));
    }

    private String extractVmIpsFromGuestAgentInterfaces(List<VmGuestAgentInterface> nics) {
        if (nics == null || nics.isEmpty()) {
            return null;
        }

        List<String> ips = new ArrayList<String>();
        for (VmGuestAgentInterface nic : nics) {
            if (nic.getIpv4Addresses() != null) {
                ips.addAll(nic.getIpv4Addresses());
            }
        }
        return ips.isEmpty() ? null : StringUtils.join(ips, " ");
    }

    /**
     * Handle changes in all VM devices
     */
    private void handleVmDeviceChange() {
        // Go over all the vms and detemine which ones require updating
        List<String> vmsToUpdateFromVds = new ArrayList<String>();
        for (VmInternalData vmInternalData : _runningVms.values()) {
            VmDynamic vmDynamic = vmInternalData.getVmDynamic();
            if (vmDynamic != null) {
                VM vm = _vmDict.get(vmDynamic.getId());
                if (vm != null) {
                    String dbHash = vm.getHash();
                    if ((dbHash == null && vmDynamic.getHash() != null) || (dbHash != null)
                            && !dbHash.equals(vmDynamic.getHash())) {
                        vmsToUpdateFromVds.add(vmDynamic.getId().toString());
                        // update new hash value
                        if (_vmDynamicToSave.containsKey(vm.getId())) {
                            _vmDynamicToSave.get(vm.getId()).setHash(vmDynamic.getHash());
                        } else {
                            addVmDynamicToList(vmDynamic);
                        }
                    }
                }
            }
        }

        if (vmsToUpdateFromVds.size() > 0) {
            // If there are vms that require updating,
            // get the new info from VDSM in one call, and then update them all
            updateVmDevices(vmsToUpdateFromVds);
        }
    }

    /**
     * Update the given list of VMs properties in DB
     *
     * @param vmsToUpdate
     */
    protected void updateVmDevices(List<String> vmsToUpdate) {
        XmlRpcStruct[] vms = getVmInfo(vmsToUpdate);
        if (vms != null) {
            for (XmlRpcStruct vm : vms) {
                processVmDevices(vm);
            }
        }
    }

    /**
     * gets VM full information for the given list of VMs
     * @param vmsToUpdate
     * @return
     */
    protected XmlRpcStruct[] getVmInfo(List<String> vmsToUpdate) {
        return (XmlRpcStruct[]) (new FullListVdsCommand<FullListVDSCommandParameters>(
                new FullListVDSCommandParameters(_vds.getId(), vmsToUpdate)).ExecuteWithReturnValue());
    }

    private boolean shouldLogDeviceDetails(String deviceType) {
        return !StringUtils.equalsIgnoreCase(deviceType, VmDeviceType.FLOPPY.getName());
    }

    private void logDeviceInformation(Guid vmId, XmlRpcStruct device) {
        String message = "Received a {0} Device without an address when processing VM {1} devices, skipping device";
        String deviceType = getDeviceType(device);

        if (shouldLogDeviceDetails(deviceType)) {
            Map<String,Object> deviceInfo = device.getInnerMap();
            log.infoFormat(message + ": {2}",
                           StringUtils.defaultIfEmpty(deviceType, StringUtils.EMPTY),
                           vmId,
                           deviceInfo.toString());
        } else {
            log.infoFormat(message,
                           StringUtils.defaultIfEmpty(deviceType, StringUtils.EMPTY),
                           vmId);
        }
    }

    /**
     * Actually process the VM device update in DB.
     * @param vm
     */
    private void processVmDevices(XmlRpcStruct vm) {
        if (vm == null || vm.getItem(VdsProperties.vm_guid) == null) {
            log.error("Received NULL VM or VM id when processing VM devices, abort.");
            return;
        }
        Guid vmId = new Guid((String) vm.getItem(VdsProperties.vm_guid));
        HashSet<Guid> processedDevices = new HashSet<Guid>();
        Object[] objects = (Object[]) vm.getItem(VdsProperties.Devices);
        List<VmDevice> devices = getDbFacade().getVmDeviceDao().getVmDeviceByVmId(vmId);
        Map<VmDeviceId, VmDevice> deviceMap = new HashMap<VmDeviceId, VmDevice>();
        for (VmDevice device : devices) {
            deviceMap.put(device.getId(), device);
        }
        for (Object o : objects) {
            XmlRpcStruct device = new XmlRpcStruct((Map<String, Object>) o);
            Guid deviceId = getDeviceId(device);
            if ((device.getItem(VdsProperties.Address)) == null) {
                logDeviceInformation(vmId, device);
                continue;
            }
            VmDevice vmDevice = deviceMap.get(new VmDeviceId(deviceId, vmId));
            if (deviceId == null || vmDevice == null) {
                deviceId = addNewVmDevice(vmId, device);
            } else {
                String alias = StringUtils.defaultIfEmpty((String) device.getItem(VdsProperties.Alias), "");
                vmDevice.setAddress(((Map<String, String>) device.getItem(VdsProperties.Address)).toString());
                vmDevice.setAlias(alias);
                addVmDeviceToList(vmDevice);
            }
            processedDevices.add(deviceId);
        }
        handleRemovedDevices(vmId, processedDevices, devices);
    }

    /**
     * Removes unmanaged devices from DB if were removed by libvirt. Empties device address with isPlugged = false
     * @param vmId
     * @param processedDevices
     */
    private void handleRemovedDevices(Guid vmId, HashSet<Guid> processedDevices, List<VmDevice> devices) {
        for (VmDevice device : devices) {
            if (!processedDevices.contains(device.getDeviceId())) {
                if (device.getIsManaged()) {
                    if (!device.getIsPlugged()) {
                        log.errorFormat("VM {0} managed non pluggable device was removed unexpectedly from libvirt: {1}",
                                vmId, device.toString());
                    } else {
                        device.setAddress("");
                        addVmDeviceToList(device);
                        log.debugFormat("VM {0} managed pluggable device was unplugged : {1}",
                                vmId,
                                device.toString());
                    }
                } else {
                    removedDeviceIds.add(device.getId());
                    log.debugFormat("VM {0} unmanaged device was marked for remove : {1}", vmId, device.toString());
                }
            }
        }
    }

    /**
     * Adds new devices recognized by libvirt
     * @param vmId
     * @param device
     */
    private Guid addNewVmDevice(Guid vmId, XmlRpcStruct device) {
        Guid newDeviceId = Guid.Empty;
        String typeName = (String) device.getItem(VdsProperties.Type);
        String deviceName = (String) device.getItem(VdsProperties.Device);
        // do not allow null or empty device or type values
        if (!StringUtils.isEmpty(typeName) && !StringUtils.isEmpty(deviceName)) {
            String address = ((Map<String, String>) device.getItem(VdsProperties.Address)).toString();
            String alias = StringUtils.defaultIfEmpty((String) device.getItem(VdsProperties.Alias), "");
            Object o = device.getItem(VdsProperties.SpecParams);
            newDeviceId = Guid.NewGuid();
            VmDeviceId id = new VmDeviceId(newDeviceId, vmId);
            VmDevice newDevice = new VmDevice(id, typeName, deviceName, address,
                    0,
                    o != null ? (Map<String, Object>) o : new HashMap<String, Object>(),
                    false,
                    true,
                    false,
                    alias);
            newVmDevices.add(newDevice);
            log.debugFormat("New device was marked for adding to VM {0} Devices : {1}", vmId, newDevice.toString());
        } else {
            log.errorFormat("Empty or NULL values were passed for a VM {0} device, Device is skipped", vmId);
        }
        return newDeviceId;
    }

    /**
     * gets the device id from the structure returned by VDSM device ids are stored in specParams map
     * @param device
     * @return
     */
    private static Guid getDeviceId(XmlRpcStruct device) {
        String deviceId = (String) device.getItem(VdsProperties.DeviceId);
        return deviceId == null ? null : new Guid(deviceId);
    }

    /**
     * gets the device type from the structure returned by VDSM
     * @param device
     * @return
     */
    private static String getDeviceType(XmlRpcStruct device) {
        return (String) device.getItem(VdsProperties.Device);
    }

    // if not statistics check if status changed and add to running list
    private List<VM> checkVmsStatusChanged() {
        List<VM> running = new ArrayList<VM>();
        if (!_vdsManager.getRefreshStatistics()) {
            List<VmDynamic> tempRunningList = new ArrayList<VmDynamic>();
            for (VmInternalData runningVm : _runningVms.values()) {
                tempRunningList.add(runningVm.getVmDynamic());
            }
            for (VmDynamic runningVm : tempRunningList) {
                VM vmToUpdate = _vmDict.get(runningVm.getId());

                if (vmToUpdate == null
                        || (vmToUpdate.getStatus() != runningVm.getstatus() && !(vmToUpdate.getStatus() == VMStatus.SavingState && runningVm
                                .getstatus() == VMStatus.Up))) {
                    GetVmStatsVDSCommand<GetVmStatsVDSCommandParameters> command =
                            new GetVmStatsVDSCommand<GetVmStatsVDSCommandParameters>(new GetVmStatsVDSCommandParameters(
                                    _vds, runningVm.getId()));
                    command.Execute();
                    if (command.getVDSReturnValue().getSucceeded()) {
                        _runningVms.put(runningVm.getId(),
                                (VmInternalData) command.getReturnValue());
                    } else {
                        _runningVms.remove(runningVm.getId());
                    }
                } else {
                    // status not changed move to next vm
                    running.add(vmToUpdate);
                    _runningVms.remove(vmToUpdate.getId());
                }
            }
        }
        return running;
    }

    /**
     * Delete all vms with status Down
     */
    private void proceedDownVms() {
        for (VmInternalData vmInternalData : _runningVms.values()) {
            VmDynamic vm = vmInternalData.getVmDynamic();
            if (vm.getstatus() != VMStatus.Down) {
                continue;
            }

            VM vmTo = _vmDict.get(vm.getId());
            VMStatus status = VMStatus.Unassigned;
            if (vmTo != null) {
                status = vmTo.getStatus();
                proceedVmBeforeDeletion(vmTo, vm);

                // when going to suspend, delete vm from cache later
                if (status == VMStatus.SavingState) {
                    ResourceManager.getInstance().InternalSetVmStatus(vmTo, VMStatus.Suspended);
                }

                clearVm(vmTo, vmInternalData.getVmDynamic().getExitStatus(), vmInternalData.getVmDynamic().getExitMessage());
            }

            VmStatistics vmStatistics = getDbFacade().getVmStatisticsDao().get(vm.getId());
            if (vmStatistics != null) {
                DestroyVDSCommand<DestroyVmVDSCommandParameters> vdsBrokerCommand =
                        new DestroyVDSCommand<DestroyVmVDSCommandParameters>(new DestroyVmVDSCommandParameters(
                                _vds.getId(), vm.getId(), false, false, 0));
                vdsBrokerCommand.Execute();

                if (vmTo != null && status == VMStatus.SavingState) {
                    afterSuspendTreatment(vm);
                } else if (status != VMStatus.MigratingFrom) {
                    handleVmOnDown(vmTo, vm, vmStatistics);
                }
            }
        }
    }

    private void handleVmOnDown(VM cacheVm, VmDynamic vmDynamic, VmStatistics vmStatistics) {
        VmExitStatus exitStatus = vmDynamic.getExitStatus();

        if (exitStatus != VmExitStatus.Normal) {

            auditVmOnDownEvent(exitStatus, vmDynamic.getExitMessage(), vmStatistics.getId());

            // Vm failed to run - try to rerun it on other Vds
            if (cacheVm != null) {
                if (ResourceManager.getInstance().IsVmInAsyncRunningList(vmDynamic.getId())) {
                    log.infoFormat("Running on vds during rerun failed vm: {0}", vmDynamic.getrun_on_vds());
                    _vmsToRerun.add(vmDynamic.getId());
                } else if (cacheVm.isAutoStartup()) {
                    _autoVmsToRun.add(vmDynamic.getId());
                }
            }
            // if failed in destination right after migration
            else // => cacheVm == null
            {
                ResourceManager.getInstance().RemoveAsyncRunningVm(vmDynamic.getId());
                addVmDynamicToList(vmDynamic);
            }
        } else {
            // if went down normally during migration process (either on source or on destination)
            // don't generate an event
            if (cacheVm != null) {
                auditVmOnDownEvent(exitStatus, vmDynamic.getExitMessage(), vmStatistics.getId());
            }

            // Vm moved safely to down status. May be migration - just remove it from Async Running command.
            ResourceManager.getInstance().RemoveAsyncRunningVm(vmDynamic.getId());
        }
    }

    /**
     * Generate an error or information event according to the exit status of a VM in status 'down'
     */
    private void auditVmOnDownEvent(VmExitStatus exitStatus, String exitMessage, Guid vmStatisticsId) {
        AuditLogType type = exitStatus == VmExitStatus.Normal ? AuditLogType.VM_DOWN : AuditLogType.VM_DOWN_ERROR;
        AuditLogableBase logable = new AuditLogableBase(_vds.getId(), vmStatisticsId);
        if (exitMessage != null) {
            logable.AddCustomValue("ExitMessage", "Exit message: " + exitMessage);
        }
        auditLog(logable, type);
    }

    private void afterSuspendTreatment(VmDynamic vm) {
        AuditLogType type = vm.getExitStatus() == VmExitStatus.Normal ? AuditLogType.USER_SUSPEND_VM_OK
                : AuditLogType.USER_FAILED_SUSPEND_VM;

        AuditLogableBase logable = new AuditLogableBase(_vds.getId(), vm.getId());
        auditLog(logable, type);
        ResourceManager.getInstance().RemoveAsyncRunningVm(vm.getId());
    }

    private void proceedVmBeforeDeletion(VM curVm, VmDynamic vmDynamic) {
        AuditLogType type = AuditLogType.UNASSIGNED;
        AuditLogableBase logable = new AuditLogableBase(_vds.getId(), curVm.getId());
        switch (curVm.getStatus()) {
        case MigratingFrom: {
            // if a VM that was a source host in migration process is now down with normal
            // exit status that's OK, otherwise..
            if (vmDynamic != null && vmDynamic.getExitStatus() != VmExitStatus.Normal) {
                if (curVm.getmigrating_to_vds() != null) {
                    DestroyVmVDSCommand<DestroyVmVDSCommandParameters> destroyCmd =
                            new DestroyVmVDSCommand<DestroyVmVDSCommandParameters>
                            (new DestroyVmVDSCommandParameters(new Guid(curVm.getmigrating_to_vds().toString()),
                                    curVm.getId(),
                                    true,
                                    false,
                                    0));
                    destroyCmd.Execute();
                    if (destroyCmd.getVDSReturnValue().getSucceeded()) {
                        log.infoFormat("Stopped migrating vm: {0} on vds: {1}", curVm.getVmName(),
                                curVm.getmigrating_to_vds());
                    } else {
                        log.infoFormat("Could not stop migrating vm: {0} on vds: {1}, Error: {2}", curVm.getVmName(),
                                curVm.getmigrating_to_vds(), destroyCmd.getVDSReturnValue().getExceptionString());
                    }
                }
                // set vm status to down if source vm crushed
                ResourceManager.getInstance().InternalSetVmStatus(curVm, VMStatus.Down, vmDynamic.getExitStatus(), vmDynamic.getExitMessage());
                addVmDynamicToList(curVm.getDynamicData());
                addVmStatisticsToList(curVm.getStatisticsData());
                addVmInterfaceStatisticsToList(curVm.getInterfaces());
                type = AuditLogType.VM_MIGRATION_ABORT;
                logable.AddCustomValue("MigrationError", vmDynamic.getExitMessage());

                ResourceManager.getInstance().RemoveAsyncRunningVm(vmDynamic.getId());
            }
            break;
        }
        case PoweredDown: {
            logable.AddCustomValue("VmStatus", "PoweredDown");
            type = AuditLogType.VM_DOWN;
            break;
        }
        default:
            break;
        }
        if (type != AuditLogType.UNASSIGNED) {
            auditLog(logable, type);
        }
    }

    private void updateRepository(List<VM> running) {
        for (VmInternalData vmInternalData : _runningVms.values()) {
            VmDynamic runningVm = vmInternalData.getVmDynamic();
            VM vmToUpdate = _vmDict.get(runningVm.getId());

            // launch powerclient on clientIp change logic
            // if not migrating here and not down
            if (!inMigrationTo(runningVm, vmToUpdate) && runningVm.getstatus() != VMStatus.Down) {
                if (vmToUpdate != null) {
                    if (_vmDict.containsKey(vmToUpdate.getId())
                            && !StringHelper.EqOp(runningVm.getclient_ip(), vmToUpdate.getClientIp())) {
                        _vmsClientIpChanged.put(vmToUpdate, runningVm);
                    }
                }
                if (vmToUpdate != null) {
                    logVmStatusTransition(vmToUpdate, runningVm);
                    // open spice for dedicated VMs
                    if (vmToUpdate.getStatus() != VMStatus.Up && runningVm.getstatus() == VMStatus.Up
                            || vmToUpdate.getStatus() != VMStatus.PoweringUp
                            && runningVm.getstatus() == VMStatus.PoweringUp) {
                        // Vm moved to powering Up or up status - launch spice
                        // if no current client ip already connected.
                        if (runningVm.getdisplay() != null) {
                            _poweringUpVms.add(runningVm);
                        } else {
                            log.error("UpdateRepository - runningVm.display is null, cannot start spice for it");
                        }
                    }

                    // Generate an event for those machines that transition from "PoweringDown" to
                    // "Up" as this means that the power down operation failed:
                    if (vmToUpdate.getStatus() == VMStatus.PoweringDown && runningVm.getstatus() == VMStatus.Up) {
                        AuditLogableBase logable = new AuditLogableBase(_vds.getId(), vmToUpdate.getId());
                        auditLog(logable, AuditLogType.VM_POWER_DOWN_FAILED);
                    }

                    if (vmToUpdate.getStatus() != VMStatus.Up && vmToUpdate.getStatus() != VMStatus.MigratingFrom
                            && runningVm.getstatus() == VMStatus.Up) {
                        // Vm moved to Up status - remove its record from Async
                        // running handling
                        if (log.isDebugEnabled()) {
                            log.debugFormat("removing VM {0} from successful run VMs list", vmToUpdate.getId());
                        }
                        if (!_succededToRunVms.contains(vmToUpdate.getId())) {
                            _succededToRunVms.add(vmToUpdate.getId());
                        }
                    }
                    afterMigrationFrom(runningVm, vmToUpdate);

                    if (vmToUpdate.getStatus() != VMStatus.NotResponding
                            && runningVm.getstatus() == VMStatus.NotResponding) {
                        AuditLogableBase logable = new AuditLogableBase(_vds.getId(), vmToUpdate.getId());
                        auditLog(logable, AuditLogType.VM_NOT_RESPONDING);
                    }
                    // check if vm is suspended and remove it from async list
                    else if (runningVm.getstatus() == VMStatus.Paused) {
                        _vmsToRemoveFromAsync.add(vmToUpdate.getId());
                        if (vmToUpdate.getStatus() != VMStatus.Paused) {
                            // check exit message to determine wht the vm has
                            // paused
                            AuditLogType logType = AuditLogType.UNASSIGNED;
                            AuditLogableBase logable = new AuditLogableBase(_vds.getId(), vmToUpdate.getId());
                            VmPauseStatus pauseStatus = runningVm.getPauseStatus();
                            if (pauseStatus.equals(VmPauseStatus.NOERR) || pauseStatus.equals(VmPauseStatus.NONE)) {
                                // user requested pause, no log needed
                            } else if (pauseStatus == VmPauseStatus.ENOSPC) {
                                logType = AuditLogType.VM_PAUSED_ENOSPC;
                            } else if (pauseStatus == VmPauseStatus.EIO) {
                                logType = AuditLogType.VM_PAUSED_EIO;
                            } else if (pauseStatus == VmPauseStatus.EPERM) {
                                logType = AuditLogType.VM_PAUSED_EPERM;
                            } else {
                                logType = AuditLogType.VM_PAUSED_ERROR;
                            }
                            if (logType != AuditLogType.UNASSIGNED) {
                                auditLog(logable, logType);
                            }
                        }

                    }
                }
                if (vmToUpdate != null || runningVm.getstatus() != VMStatus.MigratingFrom) {
                    RefObject<VM> tempRefObj = new RefObject<VM>(vmToUpdate);
                    boolean updateSucceed = updateVmRunTimeInfo(tempRefObj, runningVm);
                    vmToUpdate = tempRefObj.argvalue;
                    if (updateSucceed) {
                        addVmDynamicToList(vmToUpdate.getDynamicData());
                    }
                }
                if (vmToUpdate != null) {
                    updateVmStatistics(vmToUpdate);
                    if (_vmDict.containsKey(runningVm.getId())) {
                        running.add(_vmDict.get(runningVm.getId()));
                        if (!_vdsManager.getInitialized()) {
                            ResourceManager.getInstance().RemoveVmFromDownVms(_vds.getId(), runningVm.getId());
                        }
                    }
                }
            } else {
                if (runningVm.getstatus() == VMStatus.MigratingTo && vmToUpdate != null) {
                    running.add(vmToUpdate);
                }

                VmDynamic vmDynamic = getDbFacade().getVmDynamicDao().get(runningVm.getId());
                if (vmDynamic == null || vmDynamic.getstatus() != VMStatus.Unknown) {
                    _vmDynamicToSave.remove(runningVm.getId());
                }
            }
        }
        // compare between vm in cache and vm from vdsm
        removeVmsFromCache(running);
    }

    private static void logVmStatusTransition(VM vmToUpdate, VmDynamic runningVm) {
        if (vmToUpdate.getStatus() != runningVm.getstatus()) {
            log.infoFormat("VM {0} {1} moved from {2} --> {3}",
                    vmToUpdate.getVmName(),
                    vmToUpdate.getId(),
                    vmToUpdate.getStatus().name(),
                    runningVm.getstatus().name());
        }
    }

    // del from cache all vms that not in vdsm
    private void removeVmsFromCache(List<VM> running) {
        Guid vmGuid;
        for (VM vmToRemove : _vmDict.values()) {
            if (running.contains(vmToRemove))
            {
                continue;
            }
            proceedVmBeforeDeletion(vmToRemove, null);
            boolean isInMigration = false;
            if (vmToRemove.getStatus() == VMStatus.MigratingFrom) {
                isInMigration = true;
                vmToRemove.setRunOnVds(vmToRemove.getmigrating_to_vds());
                log.infoFormat("Setting VM {0} {1} to status unknown", vmToRemove.getVmName(), vmToRemove.getId());
                ResourceManager.getInstance().InternalSetVmStatus(vmToRemove, VMStatus.Unknown);
                addVmDynamicToList(vmToRemove.getDynamicData());
                addVmStatisticsToList(vmToRemove.getStatisticsData());
                addVmInterfaceStatisticsToList(vmToRemove.getInterfaces());
            } else {
                clearVm(vmToRemove,
                        VmExitStatus.Error,
                        String.format("Could not find VM %s on host, assuming it went down unexpectedly",
                                vmToRemove.getVmName()));
            }
            log.infoFormat("vm {0} running in db and not running in vds - add to rerun treatment. vds {1}",
                    vmToRemove.getVmName(), _vds.getvds_name());

            vmGuid = vmToRemove.getId();
            if (!isInMigration && !_vmsToRerun.contains(vmGuid)
                    && ResourceManager.getInstance().IsVmInAsyncRunningList(vmGuid)) {
                _vmsToRerun.add(vmGuid);
            }
            // vm should be auto startup
            // not already in start up list
            // not in reported from vdsm at all
            // or reported from vdsm with error code
            else if (vmToRemove.isAutoStartup()
                    && !_autoVmsToRun.contains(vmGuid)
                    && (!_runningVms.containsKey(vmGuid) || (_runningVms.containsKey(vmGuid) && _runningVms.get(vmGuid)
                            .getVmDynamic()
                            .getExitStatus() != VmExitStatus.Normal))) {
                _autoVmsToRun.add(vmGuid);
            }
        }
    }

    private boolean inMigrationTo(VmDynamic runningVm, VM vmToUpdate) {
        boolean returnValue = false;
        if (runningVm.getstatus() == VMStatus.MigratingTo) {
            // in migration
            log.infoFormat(
                    "RefreshVmList vm id '{0}' is migrating to vds '{1}' ignoring it in the refresh until migration is done",
                    runningVm.getId(),
                    _vds.getvds_name());
            returnValue = true;
        } else if ((vmToUpdate == null && runningVm.getstatus() != VMStatus.MigratingFrom)) {
            // check if the vm exists on another vds
            VmDynamic vmDynamic = getDbFacade().getVmDynamicDao().get(runningVm.getId());
            if (vmDynamic != null && vmDynamic.getrun_on_vds() != null
                    && !vmDynamic.getrun_on_vds().equals(_vds.getId()) && runningVm.getstatus() != VMStatus.Up) {
                log.infoFormat(
                        "RefreshVmList vm id '{0}' status = {1} on vds {2} ignoring it in the refresh until migration is done",
                        runningVm.getId(),
                        runningVm.getstatus(),
                        _vds.getvds_name());
                returnValue = true;
            }
        }
        return returnValue;
    }

    private void afterMigrationFrom(VmDynamic runningVm, VM vmToUpdate) {
        VMStatus oldVmStatus = vmToUpdate.getStatus();
        VMStatus currentVmStatus = runningVm.getstatus();

        // if the VM's status on source host was MigratingFrom and now the VM is running and its status
        // is not MigratingFrom, it means the migration failed
        if (oldVmStatus == VMStatus.MigratingFrom && currentVmStatus != VMStatus.MigratingFrom && currentVmStatus.isRunning()) {
            _vmsToRerun.add(runningVm.getId());
            log.infoFormat("Adding VM {0} to re-run list", runningVm.getId());
            vmToUpdate.setMigratingToVds(null);
        }
    }

    private void refreshCommitedMemory() {
        Integer memCommited = _vds.getguest_overhead() != null ? 0 : null;
        int vmsCoresCount = 0;
        for (VM vm : _vmDict.values()) {
            if (_vds.getguest_overhead() != null) {
                memCommited += vm.getVmMemSizeMb();
                memCommited += _vds.getguest_overhead();
            }
            vmsCoresCount += vm.getNumOfCpus();
        }
        if (memCommited == null || !memCommited.equals(_vds.getmem_commited())) {
            _vds.setmem_commited(memCommited);
            _saveVdsDynamic = true;
        }
        if (_vds.getvms_cores_count() == null || !_vds.getvms_cores_count().equals(vmsCoresCount)) {
            _vds.setvms_cores_count(vmsCoresCount);
            _saveVdsDynamic = true;
        }

        if (_vds.getpending_vcpus_count() != 0 && runningVmsInTransition == 0) {
            _vds.setpending_vcpus_count(0);
            _saveVdsDynamic = true;
        }

        if (_vds.getpending_vmem_size() != 0 && runningVmsInTransition == 0) {
            // set also vmem size to 0
            _vds.setpending_vmem_size(0);
            _saveVdsDynamic = true;
        }
    }

    private void moveVDSToMaintenanceIfNeeded() {
        if ((_vds.getstatus() == VDSStatus.PreparingForMaintenance)
                && monitoringStrategy.canMoveToMaintenance(_vds)) {
            _vdsManager.setStatus(VDSStatus.Maintenance, _vds);
            _saveVdsDynamic = true;
            _saveVdsStatistics = true;
            log.infoFormat(
                    "Updated vds status from 'Preparing for Maintenance' to 'Maintenance' in database,  vds = {0} : {1}",
                    _vds.getId(),
                    _vds.getvds_name());
        }
    }

    private boolean updateVmRunTimeInfo(RefObject<VM> vmToUpdate, VmDynamic vmNewDynamicData) {
        boolean returnValue = false;
        if (vmToUpdate.argvalue == null) {
            vmToUpdate.argvalue = getDbFacade().getVmDao().get(vmNewDynamicData.getId());
            // if vm exists in db update info
            if (vmToUpdate.argvalue != null) {
                // TODO: This is done to keep consistency with VmDAO.getById(Guid).
                // It should probably be removed, but some research is required.
                vmToUpdate.argvalue.setInterfaces(getDbFacade()
                        .getVmNetworkInterfaceDao()
                        .getAllForVm(vmToUpdate.argvalue.getId()));

                _vmDict.put(vmToUpdate.argvalue.getId(), vmToUpdate.argvalue);
                if (vmNewDynamicData.getstatus() == VMStatus.Up) {
                    if (!_succededToRunVms.contains(vmToUpdate.argvalue.getId())) {
                        _succededToRunVms.add(vmToUpdate.argvalue.getId());
                    }
                }
            }
        }
        if (vmToUpdate.argvalue != null) {
            // check if dynamic data changed - update cache and DB
            List<String> props = ObjectIdentityChecker.GetChangedFields(
                    vmToUpdate.argvalue.getDynamicData(), vmNewDynamicData);
            // dont check fields:
            props.remove("vm_host");
            props.remove("run_on_vds");
            props.remove("disks");
            props.remove("boot_sequence");
            props.remove("last_vds_run_on");
            props.remove("hibernation_vol_handle");
            props.remove("exitMessage");
            props.remove("lastStartTime");
            if (vmNewDynamicData.getstatus() != VMStatus.Up) {
                props.remove("app_list");
                vmNewDynamicData.setapp_list(vmToUpdate.argvalue.getAppList());
            } else if (props.contains("status")
                    && vmToUpdate.argvalue.getDynamicData().getstatus() == VMStatus.SavingState) {
                vmNewDynamicData.setstatus(VMStatus.SavingState);
                props.remove("status");
            }
            // if anything else changed
            if (props.size() > 0) {
                vmToUpdate.argvalue.updateRunTimeDynamicData(vmNewDynamicData, _vds.getId(), _vds.getvds_name());
                returnValue = true;
            }
        } else {
            // This should only happened when someone run a VM from command
            // line.
            if (Config.<Boolean> GetValue(ConfigValues.DebugTimerLogging)) {
                log.info("VDS::UpdateVmRunTimeInfo Error: found VM on a VDS that is not in the database!");
            }
        }

        return returnValue;
    }

    private void updateVmStatistics(VM vmToUpdate) {
        // check if time for vm statistics refresh - update cache and DB
        if (_vdsManager.getRefreshStatistics()) {
            VmStatistics vmStatistics = _runningVms.get(vmToUpdate.getId()).getVmStatistics();
            vmToUpdate.updateRunTimeStatisticsData(vmStatistics, vmToUpdate);
            addVmStatisticsToList(vmToUpdate.getStatisticsData());
            updateInterfaceStatistics(vmToUpdate, vmStatistics);

            for (DiskImageDynamic imageDynamic : _runningVms.get(vmToUpdate.getId()).getVmDynamic().getDisks()) {
                _vmDiskImageDynamicToSave.put(imageDynamic.getId(), imageDynamic);
            }
        }
    }

    private void updateInterfaceStatistics(VM vm, VmStatistics statistics) {
        if (statistics.getInterfaceStatistics() == null) {
            return;
        }

        if (vm.getInterfaces() == null || vm.getInterfaces().isEmpty()) {
            vm.setInterfaces(getDbFacade().getVmNetworkInterfaceDao().getAllForVm(vm.getId()));
        }
        List<String> macs = new ArrayList<String>();

        vm.setUsageNetworkPercent(0);

        for (VmNetworkInterface ifStats : statistics.getInterfaceStatistics()) {
            boolean firstTime = !macs.contains(ifStats.getMacAddress());

            VmNetworkInterface vmIface = null;
            for (VmNetworkInterface tempIf : vm.getInterfaces()) {
                if (tempIf.getMacAddress().equals(ifStats.getMacAddress())) {
                    vmIface = tempIf;
                    break;
                }
            }
            if (vmIface == null) {
                continue;
            }

            // RX rate and TX rate are reported by VDSM in % (minimum value
            // 0, maximum value 100)
            // Rx drop and TX drop are reported in packet numbers

            // if rtl+pv it will get here 2 times (we take the max one)
            if (firstTime) {

                vmIface.getStatistics().setReceiveRate(ifStats.getStatistics().getReceiveRate());
                vmIface.getStatistics().setReceiveDropRate(ifStats.getStatistics().getReceiveDropRate());
                vmIface.getStatistics().setTransmitRate(ifStats.getStatistics().getTransmitRate());
                vmIface.getStatistics().setTransmitDropRate(ifStats.getStatistics().getTransmitDropRate());
            } else {
                vmIface.getStatistics().setReceiveRate(Math.max(vmIface.getStatistics().getReceiveRate(),
                        ifStats.getStatistics().getReceiveRate()));
                vmIface.getStatistics().setReceiveDropRate(Math.max(vmIface.getStatistics().getReceiveDropRate(),
                        ifStats.getStatistics().getReceiveDropRate()));
                vmIface.getStatistics().setTransmitRate(Math.max(vmIface.getStatistics().getTransmitRate(),
                        ifStats.getStatistics().getTransmitRate()));
                vmIface.getStatistics().setTransmitDropRate(Math.max(vmIface.getStatistics().getTransmitDropRate(),
                        ifStats.getStatistics().getTransmitDropRate()));
            }
            vmIface.setVmId(vm.getId());

            if (ifStats.getSpeed() != null && vmIface.getStatistics().getReceiveRate() != null
                    && vmIface.getStatistics().getReceiveRate() > 0) {

                double rx_percent = vmIface.getStatistics().getReceiveRate();
                double tx_percent = vmIface.getStatistics().getTransmitRate();

                vm.setUsageNetworkPercent(Math.max(vm.getUsageNetworkPercent(),
                        (int) Math.max(rx_percent, tx_percent)));
            }

            if (firstTime) {
                macs.add(ifStats.getMacAddress());
            }
        }

        vm.setUsageNetworkPercent((vm.getUsageNetworkPercent() > 100) ? 100 : vm.getUsageNetworkPercent());
        addVmInterfaceStatisticsToList(vm.getInterfaces());
    }

    /**
     * Add or update vmDynamic to save list
     *
     * @param vmDynamic
     */
    private void addVmDynamicToList(VmDynamic vmDynamic) {
        _vmDynamicToSave.put(vmDynamic.getId(), vmDynamic);
    }

    /**
     * Add or update vmStatistics to save list
     *
     * @param vmStatistics
     */
    private void addVmStatisticsToList(VmStatistics vmStatistics) {
        _vmStatisticsToSave.put(vmStatistics.getId(), vmStatistics);
    }

    private void addVmInterfaceStatisticsToList(List<VmNetworkInterface> list) {
        if (list.size() <= 0) {
            return;
        }
        _vmInterfaceStatisticsToSave.put(list.get(0).getVmId().getValue(), list);
    }

    /**
     * Add or update vmDynamic to save list
     *
     * @param vmDevice
     */
    private void addVmDeviceToList(VmDevice vmDevice) {
        vmDeviceToSave.put(vmDevice.getId(), vmDevice);
    }

    private void clearVm(VM vm, VmExitStatus exitStatus, String exitMessage) {
        if (vm.getStatus() != VMStatus.MigratingFrom ) {
            // we must check that vm.getStatus() != VMStatus.Down because if it was set to down
            // the exit status and message were set, and we don't want to override them here.
            // we will add it to _vmDynamicToSave though because it might been removed from it in #updateRepository
            if (vm.getStatus() != VMStatus.Suspended && vm.getStatus() != VMStatus.Down) {
                ResourceManager.getInstance().InternalSetVmStatus(vm, VMStatus.Down, exitStatus, exitMessage);
            }
            addVmDynamicToList(vm.getDynamicData());
            addVmStatisticsToList(vm.getStatisticsData());
            addVmInterfaceStatisticsToList(vm.getInterfaces());
            if (!ResourceManager.getInstance().IsVmInAsyncRunningList(vm.getId())) {
                _vmsMovedToDown.add(vm.getId());
            }
        }
    }

    /**
     * An access method for test usages
     * @return The devices to be added to the database
     */
    protected List<VmDevice> getNewVmDevices() {
        return Collections.unmodifiableList(newVmDevices);
    }

    /**
     * An access method for test usages
     * @return The devices to be removed from the database
     */
    protected List<VmDeviceId> getRemovedVmDevices() {
        return Collections.unmodifiableList(removedDeviceIds);
    }

    protected void auditLog(AuditLogableBase auditLogable, AuditLogType logType) {
        AuditLogDirector.log(auditLogable, logType);
    }

    public DbFacade getDbFacade() {
        return DbFacade.getInstance();
    }
}
