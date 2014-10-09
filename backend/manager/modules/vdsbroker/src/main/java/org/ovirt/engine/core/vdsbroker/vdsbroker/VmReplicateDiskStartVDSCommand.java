package org.ovirt.engine.core.vdsbroker.vdsbroker;

import org.ovirt.engine.core.common.vdscommands.VmReplicateDiskParameters;

public class VmReplicateDiskStartVDSCommand<P extends VmReplicateDiskParameters> extends VmReplicateDiskVDSCommand<P> {

    public VmReplicateDiskStartVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void ExecuteVdsBrokerCommand() {
        getBroker().diskReplicateStart(getParameters().getVmId().toString(), getSrcDiskXmlRpc(), getDstDiskXmlRpc());
    }
}
