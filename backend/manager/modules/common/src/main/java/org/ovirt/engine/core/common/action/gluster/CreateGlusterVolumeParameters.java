package org.ovirt.engine.core.common.action.gluster;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;

/**
 * Command parameters for the "Create Volume" action
 */
public class CreateGlusterVolumeParameters extends VdcActionParametersBase {
    private static final long serialVersionUID = 2015321730118872954L;

    @NotNull(message = "VALIDATION.GLUSTER.VOLUME.NOT_NULL")
    @Valid
    private GlusterVolumeEntity volume;

    public CreateGlusterVolumeParameters(GlusterVolumeEntity volume) {
        setVolume(volume);
    }

    public GlusterVolumeEntity getVolume() {
        return volume;
    }

    public void setVolume(GlusterVolumeEntity volume) {
        this.volume = volume;
    }
}
