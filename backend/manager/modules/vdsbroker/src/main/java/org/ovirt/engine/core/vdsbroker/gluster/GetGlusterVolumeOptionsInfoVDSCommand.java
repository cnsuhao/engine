package org.ovirt.engine.core.vdsbroker.gluster;

import org.ovirt.engine.core.common.vdscommands.VdsIdVDSCommandParametersBase;
import org.ovirt.engine.core.vdsbroker.irsbroker.GlusterVolumeOptionsInfoReturnForXmlRpc;
import org.ovirt.engine.core.vdsbroker.vdsbroker.StatusForXmlRpc;

public class GetGlusterVolumeOptionsInfoVDSCommand<P extends VdsIdVDSCommandParametersBase> extends AbstractGlusterBrokerCommand<P> {
    private GlusterVolumeOptionsInfoReturnForXmlRpc result;

    public GetGlusterVolumeOptionsInfoVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected StatusForXmlRpc getReturnStatus() {
        return result.mStatus;
    }

    @Override
    protected void ExecuteVdsBrokerCommand() {
        result = getBroker().glusterVolumeSetOptionsList();
        ProceedProxyReturnValue();
        setReturnValue(result.optionsHelpSet);
    }
}
