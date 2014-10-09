package org.ovirt.engine.core.vdsbroker.gluster;

import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeAdvancedDetails;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeStatusOption;
import org.ovirt.engine.core.common.vdscommands.gluster.GlusterVolumeAdvancedDetailsVDSParameters;
import org.ovirt.engine.core.vdsbroker.irsbroker.GlusterVolumeStatusReturnForXmlRpc;
import org.ovirt.engine.core.vdsbroker.vdsbroker.StatusForXmlRpc;

public class GetGlusterVolumeAdvancedDetailsVDSCommand<P extends GlusterVolumeAdvancedDetailsVDSParameters> extends AbstractGlusterBrokerCommand<P> {
    private GlusterVolumeStatusReturnForXmlRpc result;

    public GetGlusterVolumeAdvancedDetailsVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected StatusForXmlRpc getReturnStatus() {
        return result.getStatus();
    }

    private boolean getSucceeded() {
        return (result.getStatus().mCode == 0);
    }

    @Override
    protected void ExecuteVdsBrokerCommand() {
        executeVolumeStatusInfo("");
        GlusterVolumeAdvancedDetails volumeAdvancedDetails = result.getVolumeAdvancedDetails();

        if (getParameters().isDetailRequired()) {
            if (getSucceeded()) {
                executeVolumeStatusInfo(GlusterVolumeStatusOption.DETAIL.name().toLowerCase());
                if (getSucceeded()) {
                    volumeAdvancedDetails.copyDetailsFrom(result.getVolumeAdvancedDetails());
                    executeVolumeStatusInfo(GlusterVolumeStatusOption.CLIENTS.name().toLowerCase());
                    if (getSucceeded()) {
                        volumeAdvancedDetails.copyClientsFrom(result.getVolumeAdvancedDetails());
                        executeVolumeStatusInfo(GlusterVolumeStatusOption.MEM.name().toLowerCase());

                        if (getSucceeded()) {
                            volumeAdvancedDetails.copyMemoryFrom(result.getVolumeAdvancedDetails());
                        }
                    }
                }
            }
        }
        setReturnValue(volumeAdvancedDetails);
    }

    private void executeVolumeStatusInfo(String volumeStatusOption) {
        result =
                getBroker().glusterVolumeStatus(getParameters().getClusterId(),
                        getParameters().getVolumeName(),
                        getParameters().getBrickName() == null ? "" : getParameters().getBrickName(),
                        volumeStatusOption);
        ProceedProxyReturnValue();
    }
}
