package org.ovirt.engine.core.common.vdscommands;

import org.ovirt.engine.core.compat.*;

public class GetImageDomainsListVDSCommandParameters extends IrsBaseVDSCommandParameters {
    public GetImageDomainsListVDSCommandParameters(Guid storagePoolId, Guid imageGroupId) {
        super(storagePoolId);
        setImageGroupId(imageGroupId);
    }

    private Guid privateImageGroupId = new Guid();

    public Guid getImageGroupId() {
        return privateImageGroupId;
    }

    private void setImageGroupId(Guid value) {
        privateImageGroupId = value;
    }

    public GetImageDomainsListVDSCommandParameters() {
    }

    @Override
    public String toString() {
        return String.format("%s, imageGroupId = %s", super.toString(), getImageGroupId());
    }
}
