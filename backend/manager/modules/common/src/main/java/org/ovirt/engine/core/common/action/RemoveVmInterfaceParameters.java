package org.ovirt.engine.core.common.action;

import org.ovirt.engine.core.compat.*;

public class RemoveVmInterfaceParameters extends VmOperationParameterBase {
    private static final long serialVersionUID = -1016346012853472189L;
    private Guid _interfaceId = new Guid();

    public RemoveVmInterfaceParameters(Guid vmId, Guid ifaceId) {
        super(vmId);
        _interfaceId = ifaceId;
    }

    public Guid getInterfaceId() {
        return _interfaceId;
    }

    public RemoveVmInterfaceParameters() {
    }
}
