package org.ovirt.engine.core.common.businessentities;

import org.ovirt.engine.core.compat.Guid;

public class DiskLunMap implements BusinessEntity<DiskLunMapId> {
    private static final long serialVersionUID = -6528043171116600954L;

    private DiskLunMapId id = new DiskLunMapId();

    public DiskLunMap() {
    }

    public DiskLunMap(Guid diskId, String lunId) {
        getId().setDiskId(diskId);
        getId().setLunId(lunId);
    }

    public DiskLunMapId getId() {
        return id;
    }

    public void setId(DiskLunMapId id) {
        this.id = id;
    }

    public Guid getDiskId() {
        return id.getDiskId();
    }

    public void setDiskId(Guid diskId) {
        id.setDiskId(diskId);
    }

    public String getLunId() {
        return id.getLunId();
    }

    public void setLunId(String lunId) {
        id.setLunId(lunId);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DiskLunMap other = (DiskLunMap) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }
}
