package org.ovirt.engine.core.common.vdscommands;

import org.ovirt.engine.core.compat.*;

public class VmLockVDSCommandParameters extends VdsAndVmIDVDSParametersBase {
    public VmLockVDSCommandParameters(Guid vdsId, Guid vmId) {
        super(vdsId, vmId);
    }

    public VmLockVDSCommandParameters() {
    }
}
