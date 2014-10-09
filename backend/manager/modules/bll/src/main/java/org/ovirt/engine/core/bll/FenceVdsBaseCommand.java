package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.FenceVdsActionParameters;
import org.ovirt.engine.core.common.action.RunVmParams;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.FenceActionType;
import org.ovirt.engine.core.common.businessentities.FenceAgentOrder;
import org.ovirt.engine.core.common.businessentities.FenceStatusReturnValue;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.vdscommands.DestroyVmVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.SetVdsStatusVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.SetVmStatusVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.ThreadUtils;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.threadpool.ThreadPoolUtil;

public abstract class FenceVdsBaseCommand<T extends FenceVdsActionParameters> extends VdsCommand<T> {
    private final int SLEEP_BEFORE_FIRST_ATTEMPT = 5000;
    private static final String INTERNAL_FENCE_USER = "Engine";
    private static Log log = LogFactory.getLog(FenceVdsBaseCommand.class);
    protected FenceExecutor executor;
    protected List<VM> mVmList = null;
    private boolean privateFenceSucceeded;
    private FenceExecutor primaryExecutor;
    private FenceExecutor secondaryExecutor;
    private FenceInvocationResult primaryResult;
    private FenceInvocationResult secondaryResult;


    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected FenceVdsBaseCommand(Guid commandId) {
        super(commandId);
    }

    public FenceVdsBaseCommand(T parameters) {
        super(parameters);
        mVmList = getVmDAO().getAllRunningForVds(getVdsId());
    }

    /**
     * Gets the number of times to retry a get status PM operation after stop/start PM operation.
     *
     * @return
     */
    protected abstract int getRerties();

    /**
     * Gets the number of seconds to delay between each retry.
     *
     * @return
     */
    protected abstract int getDelayInSeconds();

    protected boolean getFenceSucceeded() {
        return privateFenceSucceeded;
    }

    protected void setFenceSucceeded(boolean value) {
        privateFenceSucceeded = value;
    }

    @Override
    protected boolean canDoAction() {
        boolean retValue = false;
        String event;
        if (getVds() == null) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_HOST_NOT_EXIST);
            return false;
        }
        // get the event to look for , if we requested to start Host then we should look when we stopped it and vice
        // versa.
        if (getParameters().getAction() == FenceActionType.Start) {
            event = AuditLogType.USER_VDS_STOP.name();
        }
        else {
            event = AuditLogType.USER_VDS_START.name();
        }
        if (getVds().getpm_enabled()
                && IsPowerManagementLegal(getVds().getStaticData(), getVdsGroup().getcompatibility_version().toString())) {
            // check if we are in the interval of X seconds from startup
            // if yes , system is still initializing , ignore fence operations
            java.util.Date waitTo =
                    Backend.getInstance()
                            .getStartedAt()
                            .AddSeconds((Integer) Config.GetValue(ConfigValues.DisableFenceAtStartupInSec));
            java.util.Date now = new java.util.Date();
            if (waitTo.before(now) || waitTo.equals(now)) {
                // Check Quiet time between PM operations, this is done only if command is not internal and parent command is not <Restart>
                int secondsLeftToNextPmOp = (isInternalExecution() || (getParameters().getParentCommand() == VdcActionType.RestartVds))
                        ?
                        0
                        :
                        DbFacade.getInstance().getAuditLogDao().getTimeToWaitForNextPmOp(getVds().getvds_name(), event);
                if (secondsLeftToNextPmOp <= 0) {
                    // try to get vds status
                    executor = createExecutorForProxyCheck();
                    if (executor.FindVdsToFence()) {
                        if (!(retValue = executor.checkProxyHostConnectionToHost())) {
                            addCanDoActionMessage(VdcBllMessages.VDS_FAILED_FENCE_VIA_PROXY_CONNECTION);
                        }
                    } else {
                        addCanDoActionMessage(VdcBllMessages.VDS_NO_VDS_PROXY_FOUND);
                    }
                } else {
                    addCanDoActionMessage(VdcBllMessages.VDS_FENCE_DISABLED_AT_QUIET_TIME);
                    addCanDoActionMessage(String.format("$seconds %1$s", secondsLeftToNextPmOp));
                }
            } else {
                addCanDoActionMessage(VdcBllMessages.VDS_FENCE_DISABLED_AT_SYSTEM_STARTUP_INTERVAL);
            }
            // retry operation only when fence is enabled on Host.
            if (!retValue) {
                HandleError();
            }
        }
        else {
            addCanDoActionMessage(VdcBllMessages.VDS_FENCE_DISABLED);
        }
        getReturnValue().setSucceeded(retValue);
        return retValue;
    }

    @Override
    protected void executeCommand() {
        VDSStatus lastStatus = getVds().getstatus();
        VDSReturnValue vdsReturnValue = null;
        try {
            // Set status immediately to prevent a race (BZ 636950/656224)
            setStatus();
            // Check which fence invocation pattern to invoke
            // Regular (no secondary agent) , multiple sequential agents or multiple concurent agents
            if (StringUtils.isEmpty(getVds().getPmSecondaryIp())){
                handleSingleAgent(lastStatus, vdsReturnValue);
            }
            else {
                if (getVds().isPmSecondaryConcurrent()){
                    handleMultipleConcurrentAgents(lastStatus, vdsReturnValue);
                }
                else {
                    handleMultipleSequentialAgents(lastStatus, vdsReturnValue);
                }
            }
            setSucceeded(getFenceSucceeded());
        } finally {
            if (!getSucceeded()) {
                setStatus(lastStatus);
                AlertIfPowerManagementOperationFailed();
            }
        }
    }

    /**
     * Handling the case of a single fence agent
     * @param lastStatus
     */
    private void handleSingleAgent(VDSStatus lastStatus, VDSReturnValue vdsReturnValue) {
        executor = new FenceExecutor(getVds(), getParameters().getAction());
        if (executor.FindVdsToFence()) {
            vdsReturnValue = executor.Fence();
            setFenceSucceeded(vdsReturnValue.getSucceeded());
            if (getFenceSucceeded()) {
                executor = new FenceExecutor(getVds(), FenceActionType.Status);
                if (waitForStatus(getVds().getvds_name(), getParameters().getAction(), FenceAgentOrder.Primary)) {
                    handleSpecificCommandActions();
                } else {
                    handleWaitFailure(lastStatus);
                }
            } else {
                handleError(lastStatus, vdsReturnValue);
            }
        }
    }

    /**
     * Handling the case of a multiple sequential fence agents
     * If operation fails on Primary agent, the Secondary agent is used.
     * @param lastStatus
     */
    private void handleMultipleSequentialAgents(VDSStatus lastStatus, VDSReturnValue vdsReturnValue) {
        executor = new FenceExecutor(getVds(), getParameters().getAction());
        if (executor.FindVdsToFence()) {
            vdsReturnValue = executor.Fence(FenceAgentOrder.Primary);
            setFenceSucceeded(vdsReturnValue.getSucceeded());
            if (getFenceSucceeded()) {
                executor = new FenceExecutor(getVds(), FenceActionType.Status);
                if (waitForStatus(getVds().getvds_name(), getParameters().getAction(), FenceAgentOrder.Primary)) {
                    handleSpecificCommandActions();
                } else {
                    vdsReturnValue = executor.Fence(FenceAgentOrder.Secondary);
                    setFenceSucceeded(vdsReturnValue.getSucceeded());
                    if (getFenceSucceeded()) {
                        executor = new FenceExecutor(getVds(), FenceActionType.Status);
                        if (waitForStatus(getVds().getvds_name(), getParameters().getAction(),FenceAgentOrder.Secondary)) {
                            handleSpecificCommandActions();
                        }
                        else {
                            handleWaitFailure(lastStatus);
                        }
                    }
                    else {
                        handleError(lastStatus, vdsReturnValue);
                    }
                }
            } else {
                handleError(lastStatus, vdsReturnValue);
            }
        }
    }

    /**
     * Handling the case of a multiple concurrent fence agents
     * for Stop we should have two concurrent threads and wait for both to succeed
     * for Start  we should have two concurrent threads and wait for one to succeed
     * @param lastStatus
     */
    private void handleMultipleConcurrentAgents(VDSStatus lastStatus, VDSReturnValue vdsReturnValue) {
        primaryExecutor = new FenceExecutor(getVds(), getParameters().getAction());
        secondaryExecutor = new FenceExecutor(getVds(), getParameters().getAction());
        primaryResult = new FenceInvocationResult();
        secondaryResult = new FenceInvocationResult();
        List<Callable<FenceInvocationResult>> tasks = new ArrayList<Callable<FenceInvocationResult>>();
        Future<FenceInvocationResult> f1 = null;
        Future<FenceInvocationResult> f2 = null;
        tasks.add(new Callable<FenceInvocationResult>() {
            @Override
            public FenceInvocationResult call() {
                return run(primaryExecutor, FenceAgentOrder.Primary);
            }
        });
        tasks.add(new Callable<FenceInvocationResult>() {
            @Override
            public FenceInvocationResult call() {
                return run(secondaryExecutor, FenceAgentOrder.Secondary);
            }
        });
        try {
            ExecutorCompletionService<FenceInvocationResult> ecs = ThreadPoolUtil.createCompletionService(tasks);
            switch (getParameters().getAction()) {
            case Start:
                try {
                f1 = ecs.take();
                setResult(f1);
                if (primaryResult.isSucceeded() || secondaryResult.isSucceeded()) {
                    handleSpecificCommandActions();
                    setFenceSucceeded(true);
                } else {
                    tryOtherAgent(lastStatus, ecs);
                }
                } catch (InterruptedException e) {
                    tryOtherAgent(lastStatus, ecs);
                } catch (ExecutionException e) {
                    tryOtherAgent(lastStatus, ecs);
                }

                break;
            case Stop:
                f1 = ecs.take();
                f2 = ecs.take();

                if (f1.get().getOrder() == FenceAgentOrder.Primary) {
                    primaryResult = f1.get();
                    secondaryResult = f2.get();
                } else {
                    primaryResult = f2.get();
                    secondaryResult = f1.get();
                }
                if (primaryResult.isSucceeded() && secondaryResult.isSucceeded()) {
                    handleSpecificCommandActions();
                    setFenceSucceeded(true);
                } else {
                    handleError(lastStatus,
                            (!primaryResult.isSucceeded()) ? primaryResult.getValue() : secondaryResult.getValue());
                }
                break;
            default:
                setFenceSucceeded(true);
                break;
            }
        } catch (InterruptedException e) {
            log.error(e);
        } catch (ExecutionException e) {
            log.error(e);
        }
    }

    private void tryOtherAgent(VDSStatus lastStatus, ExecutorCompletionService<FenceInvocationResult> ecs)
            throws InterruptedException, ExecutionException {
        Future<FenceInvocationResult> f2;
        f2 = ecs.take();
        setResult(f2);
        if (primaryResult.isSucceeded() || secondaryResult.isSucceeded()) {
            handleSpecificCommandActions();
            setFenceSucceeded(true);
        } else {
            handleError(lastStatus, primaryResult.getValue());
            handleError(lastStatus, secondaryResult.getValue());
        }
    }

    private void setResult(Future<FenceInvocationResult> f) throws InterruptedException, ExecutionException {
        if (f.get().getOrder() == FenceAgentOrder.Primary) {
            primaryResult = f.get();
        }
        else {
            secondaryResult = f.get();
        }
    }

    private FenceInvocationResult run(FenceExecutor fenceExecutor, FenceAgentOrder order) {
        FenceInvocationResult fenceInvocationResult = new FenceInvocationResult();
        fenceInvocationResult.setOrder(order);
        fenceInvocationResult.setValue(fenceExecutor.Fence(order));
        if (fenceInvocationResult.getValue().getSucceeded()) {
            this.executor = new FenceExecutor(getVds(), FenceActionType.Status);
            fenceInvocationResult.setSucceeded(waitForStatus(getVds().getvds_name(), getParameters().getAction(), order));
        }
        return fenceInvocationResult;
    }
    private void handleWaitFailure(VDSStatus lastStatus) {
        VDSReturnValue vdsReturnValue;
        // since there is a chance that Agent & Host use the same power supply and
        // a Start command had failed (because we just get success on the script
        // invocation and not on its result), we have to try the Start command again
        // before giving up
        if (getParameters().getAction() == FenceActionType.Start) {
            executor = new FenceExecutor(getVds(), FenceActionType.Start);
            vdsReturnValue = executor.Fence();
            setFenceSucceeded(vdsReturnValue.getSucceeded());
            if (getFenceSucceeded()) {
                executor = new FenceExecutor(getVds(), FenceActionType.Status);
                if (waitForStatus(getVds().getvds_name(), FenceActionType.Start, FenceAgentOrder.Primary)) {
                    handleSpecificCommandActions();
                } else {
                    setFenceSucceeded(false);
                }
            } else {
                handleError(lastStatus, vdsReturnValue);
            }

        } else {
            // We reach this if we wait for on/off status after start/stop as defined in configurable delay/retries and
            // did not reach the desired on/off status.We assume that fence operation didn't complete successfully
            // Setting this flag will cause the appropriate Alert to pop and to restore host status to it's previous
            // value as appears in the finally block.
            setFenceSucceeded(false);
        }
    }

    private void handleError(VDSStatus lastStatus, final VDSReturnValue vdsReturnValue) {
        if (!((FenceStatusReturnValue) (vdsReturnValue.getReturnValue())).getIsSkipped()) {
            // Since this is a non-transactive command , restore last status
            setSucceeded(false);
            log.errorFormat("Failed to {0} VDS", getParameters().getAction()
                    .name()
                    .toLowerCase());
            throw new VdcBLLException(VdcBllErrors.VDS_FENCE_OPERATION_FAILED);
        } else { // Fence operation was skipped because Host is already in the requested state.
            setStatus(lastStatus);
        }
    }

    /**
     * Create the executor used in the can do action check. The executor created does not do retries to find a proxy
     * host, so that clients calling the can do action will get a quick response, and don't risk timing out.
     *
     * @return An executor used to check the availability of a proxy host.
     */
    protected FenceExecutor createExecutorForProxyCheck() {
        return new FenceExecutor(getVds(), FenceActionType.Status);
    }

    protected void DestroyVmOnDestination(VM vm) {
        if (vm.getStatus() == VMStatus.MigratingFrom) {
            try {
                if (vm.getmigrating_to_vds() != null) {
                    Backend.getInstance()
                            .getResourceManager()
                            .RunVdsCommand(
                                    VDSCommandType.DestroyVm,
                                    new DestroyVmVDSCommandParameters(new Guid(vm.getmigrating_to_vds().toString()), vm
                                            .getId(), true, false, 0));
                    log.infoFormat("Stopped migrating vm: {0} on vds: {1}", vm.getVmName(), vm.getmigrating_to_vds());
                }
            } catch (RuntimeException ex) {
                log.infoFormat("Could not stop migrating vm: {0} on vds: {1}, Error: {2}", vm.getVmName(),
                        vm.getmigrating_to_vds(), ex.getMessage());
                // intentionally ignored
            }
        }
    }

    protected void RestartVdsVms() {
        java.util.ArrayList<VdcActionParametersBase> runVmParamsList =
                new java.util.ArrayList<VdcActionParametersBase>();
        // restart all running vms of a failed vds.
        for (VM vm : mVmList) {
            DestroyVmOnDestination(vm);
            VDSReturnValue returnValue = Backend
                    .getInstance()
                    .getResourceManager()
                    .RunVdsCommand(VDSCommandType.SetVmStatus,
                            new SetVmStatusVDSCommandParameters(vm.getId(), VMStatus.Down));
            // Write that this VM was shut down by host reboot or manual fence
            if (returnValue != null && returnValue.getSucceeded()) {
                LogSettingVmToDown(getVds().getId(), vm.getId());
            }
            setVmId(vm.getId());
            setVmName(vm.getVmName());
            setVm(vm);
            VmPoolHandler.ProcessVmPoolOnStopVm(vm.getId(),
                    ExecutionHandler.createDefaultContexForTasks(getExecutionContext()));

            // Handle highly available VMs
            if (vm.isAutoStartup()) {
                runVmParamsList.add(new RunVmParams(vm.getId(), true));
            }
        }
        if (runVmParamsList.size() > 0) {
            Backend.getInstance().runInternalMultipleActions(VdcActionType.RunVm, runVmParamsList);
        }
        setVm(null);
        setVmId(Guid.Empty);
        setVmName(null);
    }

    protected void setStatus() {
        Backend.getInstance()
                .getResourceManager()
                .RunVdsCommand(VDSCommandType.SetVdsStatus,
                        new SetVdsStatusVDSCommandParameters(getVdsId(), VDSStatus.Reboot));
        RunSleepOnReboot();
    }

    protected void HandleError() {
    }

    @Override
    public String getUserName() {
        String userName = super.getUserName();
        return StringUtils.isEmpty(userName)? INTERNAL_FENCE_USER: userName;
    }



    protected boolean waitForStatus(String vdsName, FenceActionType actionType, FenceAgentOrder order) {
        final String FENCE_CMD = (actionType == FenceActionType.Start) ? "on" : "off";
        final String ACTION_NAME = actionType.name().toLowerCase();
        int i = 1;
        boolean statusReached = false;
        log.infoFormat("Waiting for vds {0} to {1}", vdsName, ACTION_NAME);
        // Waiting before first attempt to check the host status.
        // This is done because if we will attempt to get host status immediately
        // in most cases it will not turn from on/off to off/on and we will need
        // to wait a full cycle for it.
        ThreadUtils.sleep(SLEEP_BEFORE_FIRST_ATTEMPT);
        while (!statusReached && i <= getRerties()) {
            log.infoFormat("Attempt {0} to get vds {1} status", i, vdsName);
            if (executor.FindVdsToFence()) {
                VDSReturnValue returnValue = executor.Fence(order);
                if (returnValue != null && returnValue.getReturnValue() != null) {
                    FenceStatusReturnValue value = (FenceStatusReturnValue) returnValue.getReturnValue();
                    if (FENCE_CMD.equalsIgnoreCase(value.getStatus())) {
                        statusReached = true;
                        log.infoFormat("vds {0} status is {1}", vdsName, FENCE_CMD);
                    } else {
                        i++;
                        if (i <= getRerties())
                            ThreadUtils.sleep(getDelayInSeconds() * 1000);
                    }
                } else {
                    log.errorFormat("Failed to get host {0} status.", vdsName);
                    break;
                }
            } else {
                break;
            }
        }
        if (!statusReached) {
            // Send an Alert
            String actionName = (getParameters().getParentCommand() == VdcActionType.RestartVds) ?
                    FenceActionType.Restart.name() : ACTION_NAME;
            AuditLogableBase auditLogable = new AuditLogableBase();
            auditLogable.AddCustomValue("Host", vdsName);
            auditLogable.AddCustomValue("Status", actionName);
            AuditLogDirector.log(auditLogable, AuditLogType.VDS_ALERT_FENCE_STATUS_VERIFICATION_FAILED);
            log.errorFormat("Failed to verify host {0} {1} status. Have retried {2} times with delay of {3} seconds between each retry.",
                    vdsName,
                    ACTION_NAME,
                    getRerties(),
                    getDelayInSeconds());

        }
        return statusReached;
    }

    protected void setStatus(VDSStatus status) {
        if (getVds().getstatus() != status) {
            Backend.getInstance()
                    .getResourceManager()
                    .RunVdsCommand(VDSCommandType.SetVdsStatus,
                            new SetVdsStatusVDSCommandParameters(getVds().getId(), status));
        }
    }

    protected abstract void handleSpecificCommandActions();

    public static class FenceInvocationResult {

        private VDSReturnValue value;
        private boolean succeeded=false;
        private FenceAgentOrder order;

        public FenceAgentOrder getOrder() {
            return order;
        }

        public void setOrder(FenceAgentOrder order) {
            this.order = order;
        }

        public FenceInvocationResult() {
        }

        public VDSReturnValue getValue() {
            return value;
        }

        public void setValue(VDSReturnValue value) {
            this.value = value;
        }

        public boolean isSucceeded() {
            return succeeded;
        }

        public void setSucceeded(boolean succeeded) {
            this.succeeded = succeeded;
        }
    }
}
