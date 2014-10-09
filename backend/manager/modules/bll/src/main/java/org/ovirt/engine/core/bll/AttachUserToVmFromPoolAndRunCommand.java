package org.ovirt.engine.core.bll;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.job.ExecutionContext;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.PermissionsOperationsParametes;
import org.ovirt.engine.core.common.action.RunVmParams;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.VmPoolUserParameters;
import org.ovirt.engine.core.common.businessentities.Snapshot.SnapshotType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.businessentities.permissions;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.job.Step;
import org.ovirt.engine.core.common.job.StepEnum;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.NGuid;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.job.ExecutionMessageDirector;

@LockIdNameAttribute
public class AttachUserToVmFromPoolAndRunCommand<T extends VmPoolUserParameters> extends
VmPoolUserCommandBase<T> {
    protected AttachUserToVmFromPoolAndRunCommand(Guid commandId) {
        super(commandId);
    }

    public AttachUserToVmFromPoolAndRunCommand(T parameters) {
        super(parameters);
    }

    /**
     * This lock is used to synchronize multiple users trying to attach a VM from pool, so that they won't be able to
     * attach the same VM to more than one user.
     */
    private static final Object _lockObject = new Object();

    @Override
    protected boolean canDoAction() {
        boolean returnValue = true;

        synchronized (_lockObject) {
            // no available VMs:
            if (Guid.Empty.equals(getVmToAttach(getParameters().getVmPoolId())))
            {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_NO_AVAILABLE_POOL_VMS);
                returnValue = false;
            }
        }

        // check user isn't already attached to vm from this pool
        if (returnValue) {
            List<VM> vmsForUser = DbFacade.getInstance().getVmDao().getAllForUser(getAdUserId());

            for (VM vm : vmsForUser) {
                if (vm.getVmPoolId() != null && getVmPoolId().equals(vm.getVmPoolId())) {
                    addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_USER_ATTACHED_TO_POOL);
                    returnValue = false;
                }
            }
        }
        if (!returnValue) {
            addCanDoActionMessage(VdcBllMessages.VAR__ACTION__ALLOCATE_AND_RUN);
            addCanDoActionMessage(VdcBllMessages.VAR__TYPE__VM_FROM_VM_POOL);
        }
        return returnValue;
    }

    @Override
    public Guid getVmId() {
        return getParameters().getVmId();
    }

    @Override
    public void setVmId(Guid value) {
        getParameters().setVmId(value);
    }

    @Override
    protected NGuid getVmPoolId() {
        return getParameters().getVmPoolId();
    }

    @Override
    protected void setVmPoolId(NGuid value) {
        getParameters().setVmPoolId(value);
    }

    @Override
    public Guid getAdUserId() {
        return getParameters().getUserId();
    }

    @Override
    protected TransactionScopeOption getTransactionScopeOption() {
        return getActionState() != CommandActionState.EXECUTE ? TransactionScopeOption.Suppress : super
                .getTransactionScopeOption();
    }

    @Override
    protected void executeCommand() {
        getParameters().setParentCommand(VdcActionType.AttachUserToVmFromPoolAndRun);

        initUser();
        boolean isPrestartedVm = false;
        Guid vmToAttach = Guid.Empty;
        synchronized (_lockObject) {
            vmToAttach = getPrestartedVmToAttach(getParameters().getVmPoolId());
            if (!Guid.Empty.equals(vmToAttach)) {
                isPrestartedVm = true;
            } else {
                vmToAttach = getNonPrestartedVmToAttach(getParameters().getVmPoolId());
            }

            if (!Guid.Empty.equals(vmToAttach)) {
                getParameters().setEntityId(vmToAttach);
                setVmId(vmToAttach);
                permissions perm = new permissions(getAdUserId(), PredefinedRoles.ENGINE_USER.getId(), vmToAttach,
                        VdcObjectType.VM);
                PermissionsOperationsParametes permParams = new PermissionsOperationsParametes(perm);
                permParams.setShouldBeLogged(false);
                permParams.setParentCommand(VdcActionType.AttachUserToVmFromPoolAndRun);
                VdcReturnValueBase vdcReturnValueFromAddPerm = Backend.getInstance().runInternalAction(VdcActionType.AddPermission,
                        permParams,
                        new CommandContext(getCompensationContext()));
                if (!vdcReturnValueFromAddPerm.getSucceeded()) {
                    log.infoFormat("Failed to give user {0} permission to Vm {1} ", getAdUserId(), vmToAttach);
                    setActionReturnValue(vdcReturnValueFromAddPerm);
                    return;
                } else {
                    log.infoFormat("Succceeded giving user {0} permission to Vm {1} ", getAdUserId(), vmToAttach);
                }
            } else {
                log.infoFormat("No free Vms in pool {0}. Cannot allocate for user {1} ", getVmPoolId(), getAdUserId());
                throw new VdcBLLException(VdcBllErrors.NO_FREE_VM_IN_POOL);
            }
        }

        // Only when using a Vm that is not prestarted do we need to run the vm
        if (!isPrestartedVm) {
            setVm(getVmDAO().get(vmToAttach));
            RunVmParams runVmParams = new RunVmParams(vmToAttach);
            runVmParams.setSessionId(getParameters().getSessionId());
            runVmParams.setUseVnc(getVm().getVmType() == VmType.Server);
            runVmParams.setParentParameters(getParameters());
            runVmParams.setEntityId(vmToAttach);
            runVmParams.setParentCommand(VdcActionType.AttachUserToVmFromPoolAndRun);
            runVmParams.setRunAsStateless(true);
            ExecutionContext runVmContext = createRunVmContext();
            VdcReturnValueBase vdcReturnValue = Backend.getInstance().runInternalAction(VdcActionType.RunVm,
                    runVmParams, new CommandContext(runVmContext));

            getTaskIdList().addAll(vdcReturnValue.getInternalTaskIdList());
            setSucceeded(vdcReturnValue.getSucceeded());
            setActionReturnValue(vmToAttach);
            getReturnValue().getTaskIdList().addAll(getReturnValue().getInternalTaskIdList());
        } else {
            // no need to start, just return it
            setActionReturnValue(vmToAttach);
            setSucceeded(true);
        }
    }

    private ExecutionContext createRunVmContext() {
        ExecutionContext ctx = new ExecutionContext();
        try {
            Step step = ExecutionHandler.addSubStep(getExecutionContext(),
                    getExecutionContext().getJob().getStep(StepEnum.EXECUTING),
                    StepEnum.TAKING_VM_FROM_POOL,
                    ExecutionMessageDirector.resolveStepMessage(StepEnum.TAKING_VM_FROM_POOL, Collections.singletonMap(VdcObjectType.VM.name().toLowerCase(), getVmName())));
            ctx.setStep(step);
            ctx.setMonitored(true);
            ctx.setShouldEndJob(true);
        } catch (RuntimeException e) {
            log.error("Error when creating executing context for running stateless VM", e);
        }
        return ctx;
    }

    @Override
    protected void endSuccessfully() {
        if (getVm() != null) {
            if (DbFacade.getInstance().getSnapshotDao().exists(getVm().getId(), SnapshotType.STATELESS)) {
                setSucceeded(Backend.getInstance().endAction(VdcActionType.RunVm,
                        getParameters().getImagesParameters().get(0), new CommandContext(getCompensationContext())).getSucceeded());

                if (!getSucceeded()) {
                    log.warn("EndSuccessfully: EndAction of RunVm failed, detaching user from Vm");
                    detachUserFromVmFromPool(); // just in case.
                    getReturnValue().setEndActionTryAgain(false);
                }
            }
            else
                // Pool-snapshot is gone (probably due to ProcessVmPoolOnStopVm
                // treatment) ->
                // no point in running the VM or trying to run again the EndAction
                // method:
            {
                log.warn("EndSuccessfully: No images were created for Vm, detaching user from Vm");
                detachUserFromVmFromPool(); // just in case.
                getReturnValue().setEndActionTryAgain(false);
            }
        } else {
            setCommandShouldBeLogged(false);
            log.warn("AttachUserToVmFromPoolAndRunCommand::EndSuccessfully: Vm is null - not performing full EndAction");
            setSucceeded(true);
        }
    }

    @Override
    protected void endWithFailure() {
        setSucceeded(Backend.getInstance().endAction(VdcActionType.RunVm,
                getParameters().getImagesParameters().get(0),
                new CommandContext(getCompensationContext())).getSucceeded());
        if (!getSucceeded()) {
            log.warn("AttachUserToVmFromPoolAndRunCommand::EndWitFailure: EndAction of RunVm Failed");
        }
        detachUserFromVmFromPool();
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        switch (getActionState()) {
        case EXECUTE:
            return getSucceeded() ? AuditLogType.USER_ATTACH_USER_TO_VM_FROM_POOL
                    : AuditLogType.USER_ATTACH_USER_TO_VM_FROM_POOL_FAILED;

        case END_SUCCESS:
            return getSucceeded() ? AuditLogType.USER_ATTACH_USER_TO_VM_FROM_POOL_FINISHED_SUCCESS
                    : AuditLogType.USER_ATTACH_USER_TO_VM_FROM_POOL_FINISHED_FAILURE;

        default:
            return AuditLogType.USER_ATTACH_USER_TO_VM_FROM_POOL_FINISHED_FAILURE;
        }
    }

    protected void detachUserFromVmFromPool() {
        // Detach user from vm from pool:
        if (!Guid.Empty.equals(getAdUserId())) {
            permissions perm = DbFacade
                    .getInstance()
                    .getPermissionDao()
                    .getForRoleAndAdElementAndObject(
                            PredefinedRoles.ENGINE_USER.getId(), getAdUserId(),
                            getVmId());
            if (perm != null) {
                DbFacade.getInstance().getPermissionDao().remove(perm.getId());
            }
        }
    }

    @Override
    protected Map<String, String> getExclusiveLocks() {
        return Collections.singletonMap(getAdUserId().toString(), getClass().getName());
    }
}
