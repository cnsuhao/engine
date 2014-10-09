package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.HotPlugDiskToVmParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.dal.VdcBllMessages;

@NonTransactiveCommandAttribute
@LockIdNameAttribute
public class HotUnPlugDiskFromVmCommand<T extends HotPlugDiskToVmParameters> extends HotPlugDiskToVmCommand<T> {

    private static final long serialVersionUID = -7170156978848697566L;

    public HotUnPlugDiskFromVmCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__HOT_UNPLUG);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__VM_DISK);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_HOTUNPLUG_DISK : AuditLogType.USER_FAILED_HOTUNPLUG_DISK;
    }

    @Override
    protected VDSCommandType getPlugAction() {
        return VDSCommandType.HotUnPlugDisk;
    }
}
