package org.ovirt.engine.core.common.businessentities;

import java.io.Serializable;

import javax.persistence.Embeddable;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.ovirt.engine.core.common.businessentities.mapping.GuidType;
import org.ovirt.engine.core.compat.Guid;

@Embeddable
@TypeDef(name = "guid", typeClass = GuidType.class)
public class VmDeviceId implements Serializable,Comparable<VmDeviceId> {

    /**
     *
     */
    private static final long serialVersionUID = 7807607542617897504L;

    @Type(type = "guid")
    private Guid deviceId;

    @Type(type = "guid")
    private Guid vmId;

    public VmDeviceId() {
    }

    public VmDeviceId(Guid deviceId, Guid vmId) {
        this.deviceId = deviceId;
        this.vmId = vmId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((deviceId == null) ? 0 : deviceId.hashCode());
        result = prime * result + ((vmId == null) ? 0 : vmId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VmDeviceId other = (VmDeviceId) obj;
        if (deviceId == null) {
            if (other.deviceId != null)
                return false;
        } else if (!deviceId.equals(other.deviceId))
            return false;
        if (vmId == null) {
            if (other.vmId != null)
                return false;
        } else if (!vmId.equals(other.vmId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("deviceId = ").append(getDeviceId());
        sb.append(", vmId = ").append(getVmId());
        return sb.toString();
    }

    public Guid getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Guid deviceId) {
        this.deviceId = deviceId;
    }

    public Guid getVmId() {
        return vmId;
    }

    public void setVmId(Guid vmId) {
        this.vmId = vmId;
    }

    @Override
    public int compareTo(VmDeviceId other) {
        int vmComparsion = getVmId().compareTo(other.getVmId());
        if (vmComparsion == 0) {
            return getDeviceId().compareTo(other.getDeviceId());
        } else {
            return vmComparsion;
        }
    }
}
