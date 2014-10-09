package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.VmOperationParameterBase;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.vdscommands.PauseVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

public class PauseVmCommand<T extends VmOperationParameterBase> extends VmOperationCommandBase<T> {
    public PauseVmCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected void Perform() {
        if (getVm().isRunning()) {
            setActionReturnValue(Backend.getInstance().getResourceManager()
                    .RunVdsCommand(VDSCommandType.Pause, new PauseVDSCommandParameters(getVdsId(), getVmId()))
                    .getReturnValue());
            // Vds.pause(VmId);
            setSucceeded(true);
        } else {
            setActionReturnValue(getVm().getStatus());
        }
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_PAUSE_VM : AuditLogType.USER_FAILED_PAUSE_VM;
    }

    public static boolean CanPauseVm(Guid vmId, java.util.ArrayList<String> message) {
        boolean retValue = true;
        VM vm = DbFacade.getInstance().getVmDao().get(vmId);
        if (vm == null) {
            retValue = false;
            message.add(VdcBllMessages.ACTION_TYPE_FAILED_VM_NOT_FOUND.toString());
        } else {
            if (vm.getStatus() == VMStatus.WaitForLaunch || vm.getStatus() == VMStatus.MigratingFrom
                    || vm.getStatus() == VMStatus.NotResponding) {
                retValue = false;
                message.add(VdcBllMessages.ACTION_TYPE_FAILED_VM_STATUS_ILLEGAL.toString());
            } else if (!vm.isRunning()) {
                retValue = false;
                message.add(VdcBllMessages.ACTION_TYPE_FAILED_VM_IS_NOT_RUNNING.toString());
            }
        }

        if (!retValue) {
            message.add(VdcBllMessages.VAR__ACTION__PAUSE.toString());
            message.add(VdcBllMessages.VAR__TYPE__VM.toString());
        }
        return retValue;
    }

    @Override
    protected boolean canDoAction() {
        return CanPauseVm(getParameters().getVmId(), getReturnValue().getCanDoActionMessages());
    }
}
