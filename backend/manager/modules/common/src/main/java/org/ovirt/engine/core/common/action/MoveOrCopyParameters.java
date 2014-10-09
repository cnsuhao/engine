package org.ovirt.engine.core.common.action;

import java.io.Serializable;
import java.util.Map;

import org.ovirt.engine.core.compat.Guid;

public class MoveOrCopyParameters extends StorageDomainParametersBase implements Serializable {
    private static final long serialVersionUID = 1051590893103934441L;

    private Map<Guid, Guid> imageToDestinationDomainMap;
    private boolean importAsNewEntity;

    public MoveOrCopyParameters(Guid containerId, Guid storageDomainId) {
        super(storageDomainId);
        setContainerId(containerId);
        setTemplateMustExists(false);
        setForceOverride(false);
    }

    private Guid privateContainerId = Guid.Empty;

    public Guid getContainerId() {
        return privateContainerId;
    }

    public void setContainerId(Guid value) {
        privateContainerId = value;
    }

    private boolean privateCopyCollapse;

    public boolean getCopyCollapse() {
        return privateCopyCollapse;
    }

    public void setCopyCollapse(boolean value) {
        privateCopyCollapse = value;
    }

    private boolean privateTemplateMustExists;

    public boolean getTemplateMustExists() {
        return privateTemplateMustExists;
    }

    public void setTemplateMustExists(boolean value) {
        privateTemplateMustExists = value;
    }

    private boolean privateForceOverride;

    public boolean getForceOverride() {
        return privateForceOverride;
    }

    public void setForceOverride(boolean value) {
        privateForceOverride = value;
    }

    public MoveOrCopyParameters() {
    }

    public void setImageToDestinationDomainMap(Map<Guid, Guid> map) {
        imageToDestinationDomainMap = map;
    }

    public Map<Guid, Guid> getImageToDestinationDomainMap() {
        return imageToDestinationDomainMap;
    }

    public boolean isImportAsNewEntity() {
        return importAsNewEntity;
    }

    public void setImportAsNewEntity(boolean importAsNewEntity) {
        this.importAsNewEntity = importAsNewEntity;
    }
}
