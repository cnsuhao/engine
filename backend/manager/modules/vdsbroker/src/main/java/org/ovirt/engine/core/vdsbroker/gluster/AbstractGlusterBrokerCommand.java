package org.ovirt.engine.core.vdsbroker.gluster;

import org.ovirt.engine.core.common.errors.VDSError;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.vdscommands.VdsIdVDSCommandParametersBase;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VdsBrokerCommand;

/**
 * Gluster Specific VDS broker. The main purpose is to handle gluster command errors.
 */
public abstract class AbstractGlusterBrokerCommand<P extends VdsIdVDSCommandParametersBase> extends VdsBrokerCommand<P> {

    public AbstractGlusterBrokerCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void ProceedProxyReturnValue() {
        VdcBllErrors returnStatus = GetReturnValueFromStatus(getReturnStatus());
        switch (returnStatus) {
        case GlusterVolumeCreateFailed:
        case GlusterVolumeStartFailed:
        case GlusterVolumeStopFailed:
        case AddBricksToGlusterVolumeFailed:
        case GlusterVolumeSetOptionFailed:
        case GlusterVolumeRebalanceStartFailed:
        case GlusterVolumeDeleteFailed:
        case GlusterVolumeReplaceBrickStartFailed:
        case GlusterVolumeListFailed:
        case GlusterVolumeOptionInfoFailed:
        case GlusterVolumeResetOptionsFailed:
        case GlusterVolumeRemoveBricksFailed:
        case GlusterVolumeProfileStartFailed:
        case GlusterVolumeProfileStopFailed:
        case GlusterAddHostFailed:
        case RemoveGlusterServerFailed:
        case GlusterPeerListFailed:
        case GlusterVolumeStatusFailed:
        case GlusterVolumeProfileInfoFailed:
            // Capture error from gluster command and record failure
            getVDSReturnValue().setVdsError(new VDSError(returnStatus, getReturnStatus().mMessage));
            getVDSReturnValue().setSucceeded(false);
            break;
        default:
            super.ProceedProxyReturnValue();
            break;
        }
    }
}
