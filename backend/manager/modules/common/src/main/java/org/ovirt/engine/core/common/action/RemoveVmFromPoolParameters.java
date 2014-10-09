package org.ovirt.engine.core.common.action;

import org.ovirt.engine.core.compat.*;

public class RemoveVmFromPoolParameters extends VmPoolParametersBase {
    private static final long serialVersionUID = -9051384517712295806L;
    private Guid _vmId = new Guid();
    private boolean removePoolUponDetachAllVMs = false;

    public RemoveVmFromPoolParameters(Guid vmId) {
        this(vmId, false);
    }

    public RemoveVmFromPoolParameters(Guid vmId, boolean removePoolUponDetachAllVMs) {
        super(Guid.Empty);
        _vmId = vmId;
        this.removePoolUponDetachAllVMs = removePoolUponDetachAllVMs;
    }

    public RemoveVmFromPoolParameters() {

    }

    public Guid getVmId() {
        return _vmId;
    }

    public boolean isRemovePoolUponDetachAllVMs() {
        return removePoolUponDetachAllVMs;
    }
}
