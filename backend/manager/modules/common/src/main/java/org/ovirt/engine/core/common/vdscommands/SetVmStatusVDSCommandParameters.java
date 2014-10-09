package org.ovirt.engine.core.common.vdscommands;

import org.ovirt.engine.core.compat.*;
import org.ovirt.engine.core.common.businessentities.*;

public class SetVmStatusVDSCommandParameters extends VDSParametersBase {
    private Guid _vmId = new Guid();
    private VMStatus _status = VMStatus.forValue(0);

    public SetVmStatusVDSCommandParameters(Guid vmId, VMStatus status) {
        _vmId = vmId;
        _status = status;
    }

    public Guid getVmId() {
        return _vmId;
    }

    public VMStatus getStatus() {
        return _status;
    }

    public SetVmStatusVDSCommandParameters() {
    }

    @Override
    public String toString() {
        return String.format("vmId = %s, status = %s", getVmId(), getStatus());
    }
}
