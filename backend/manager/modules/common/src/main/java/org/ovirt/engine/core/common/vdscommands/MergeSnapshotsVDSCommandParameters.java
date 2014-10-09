package org.ovirt.engine.core.common.vdscommands;

import org.ovirt.engine.core.compat.*;

public class MergeSnapshotsVDSCommandParameters extends AllStorageAndImageIdVDSCommandParametersBase {
    private Guid _imageId2 = new Guid();

    public MergeSnapshotsVDSCommandParameters(Guid storagePoolId, Guid storageDomainId, Guid vmId, Guid imageGroupId,
            Guid imageId, Guid imageId2, boolean postZero, String compatibilityVersion) {
        super(storagePoolId, storageDomainId, imageGroupId, imageId);
        _imageId2 = imageId2;
        setVmId(vmId);
        setPostZero(postZero);
        setCompatibilityVersion(compatibilityVersion);
    }

    public Guid getImageId2() {
        return _imageId2;
    }

    private Guid privateVmId = new Guid();

    public Guid getVmId() {
        return privateVmId;
    }

    public void setVmId(Guid value) {
        privateVmId = value;
    }

    private boolean privatePostZero;

    public boolean getPostZero() {
        return privatePostZero;
    }

    public void setPostZero(boolean value) {
        privatePostZero = value;
    }

    public MergeSnapshotsVDSCommandParameters() {
    }

    @Override
    public String toString() {
        return String.format("%s, imageId2 = %s, vmId = %s, postZero = %s",
                super.toString(),
                getImageId2(),
                getVmId(),
                getPostZero());
    }
}
