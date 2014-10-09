package org.ovirt.engine.core.common.businessentities.network;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.ovirt.engine.core.common.businessentities.BusinessEntitiesDefinitions;
import org.ovirt.engine.core.common.businessentities.BusinessEntity;
import org.ovirt.engine.core.common.businessentities.IVdcQueryable;
import org.ovirt.engine.core.common.businessentities.Nameable;
import org.ovirt.engine.core.common.businessentities.mapping.GuidType;
import org.ovirt.engine.core.common.utils.ValidationUtils;
import org.ovirt.engine.core.common.validation.annotation.MTU;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.common.validation.group.UpdateEntity;
import org.ovirt.engine.core.compat.Guid;

@Entity
@Table(name = "network")
@TypeDef(name = "guid", typeClass = GuidType.class)
public class Network extends IVdcQueryable implements Serializable, BusinessEntity<Guid>, Nameable {
    private static final long serialVersionUID = 7357288865938773402L;

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "org.ovirt.engine.core.dao.GuidGenerator")
    @Column(name = "Id")
    @Type(type = "guid")
    private Guid id;

    @Pattern(regexp = "^[_a-zA-Z0-9]{1,15}$", message = "NETWORK_ILEGAL_NETWORK_NAME", groups = { CreateEntity.class,
            UpdateEntity.class })
    @Size(min = 1, max = BusinessEntitiesDefinitions.NETWORK_NAME_SIZE)
    @Column(name = "name")
    private String name;

    @Size(max = BusinessEntitiesDefinitions.GENERAL_MAX_SIZE)
    @Column(name = "description")
    private String description;

    @Column(name = "type")
    private Integer type;

    @Pattern(regexp = ValidationUtils.IP_PATTERN, message = "NETWORK_ADDR_IN_STATIC_IP_BAD_FORMAT")
    @Size(max = BusinessEntitiesDefinitions.GENERAL_NETWORK_ADDR_SIZE)
    @Column(name = "addr")
    private String addr;

    @Pattern(regexp = ValidationUtils.IP_PATTERN, message = "NETWORK_ADDR_IN_SUBNET_BAD_FORMAT")
    @Size(max = BusinessEntitiesDefinitions.GENERAL_SUBNET_SIZE)
    @Column(name = "subnet")
    private String subnet;

    @Pattern(regexp = ValidationUtils.IP_PATTERN, message = "NETWORK_ADDR_IN_GATEWAY_BAD_FORMAT")
    @Size(max = BusinessEntitiesDefinitions.GENERAL_GATEWAY_SIZE)
    @Column(name = "gateway")
    private String gateway;

    @Column(name = "vlan_id")
    @Min(value = 0, message = "NETWORK_VLAN_OUT_OF_RANGE", groups = { CreateEntity.class, UpdateEntity.class })
    @Max(value = 4094, message = "NETWORK_VLAN_OUT_OF_RANGE", groups = { CreateEntity.class, UpdateEntity.class })
    private Integer vlanId;

    @Column(name = "stp")
    private boolean stp = false;

    @Column(name = "storage_pool_id")
    @Type(type = "guid")
    @NotNull(message = "VALIDATION.STORAGE_POOL.ID.NOT_NULL", groups = { CreateEntity.class, UpdateEntity.class })
    private Guid dataCenterId;

    @ManyToOne
    @JoinTable(name = "network_cluster", joinColumns = @JoinColumn(name = "network_id"),
    inverseJoinColumns = @JoinColumn(name = "cluster_id"))
    private NetworkCluster cluster;

    private boolean vmNetwork = true;

    @MTU
    private int mtu;

    public Network() {
    }

    public Network(String addr, String description, Guid id, String name, String subnet, String gateway, Integer type,
            Integer vlan_id, boolean stp, int mtu, boolean vmNetwork) {
        this.addr = addr;
        this.description = description;
        this.id = id;
        this.name = name;
        this.subnet = subnet;
        this.gateway = gateway;
        this.type = type;
        this.vlanId = vlan_id;
        this.stp = stp;
        this.mtu = mtu;
        this.vmNetwork = vmNetwork;
    }

    public NetworkCluster getCluster() {
        return cluster;
    }

    public String getAddr() {
        return this.addr;
    }

    public void setAddr(String value) {
        this.addr = value;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    @Override
    public Guid getId() {
        return this.id;
    }

    @Override
    public void setId(Guid value) {
        this.id = value;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getSubnet() {
        return this.subnet;
    }

    public void setSubnet(String value) {
        this.subnet = value;
    }

    public String getGateway() {
        return this.gateway;
    }

    public void setGateway(String value) {
        this.gateway = value;
    }

    public Integer getType() {
        return this.type;
    }

    public void setType(Integer value) {
        this.type = value;
    }

    public Integer getVlanId() {
        return this.vlanId;
    }

    public void setVlanId(Integer value) {
        this.vlanId = value;
    }

    public boolean getStp() {
        return this.stp;
    }

    public void setStp(boolean value) {
        this.stp = value;
    }

    public Guid getDataCenterId() {
        return this.dataCenterId;
    }

    public void setDataCenterId(Guid value) {
        this.dataCenterId = value;
    }

    public void setCluster(NetworkCluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public Object getQueryableId() {
        return getId();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getName())
                .append(" {id=")
                .append(getId())
                .append(", description=")
                .append(getDescription())
                .append(", subnet=")
                .append(getSubnet())
                .append(", gateway=")
                .append(getGateway())
                .append(", type=")
                .append(getType())
                .append(", vlanId=")
                .append(getVlanId())
                .append(", stp=")
                .append(getStp())
                .append(", dataCenterId=")
                .append(getDataCenterId())
                .append(", mtu=")
                .append(getMtu())
                .append(", vmNetwork=")
                .append(isVmNetwork())
                .append(", cluster=")
                .append(getCluster())
                .append("}");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((addr == null) ? 0 : addr.hashCode());
        //FIXME: remove cluster from hashCode calculation - breaks the tests when working in JDBC template mode
        /*
        result = prime * result + ((cluster == null) ? 0 : cluster.hashCode());
        */
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((gateway == null) ? 0 : gateway.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((dataCenterId == null) ? 0 : dataCenterId.hashCode());
        result = prime * result + (stp ? 1231 : 1237);
        result = prime * result + ((subnet == null) ? 0 : subnet.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((vlanId == null) ? 0 : vlanId.hashCode());
        result = prime * result + (mtu);
        result = prime * result + ((vmNetwork) ? 11 : 13);
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
        Network other = (Network) obj;
        if (addr == null) {
            if (other.addr != null)
                return false;
        } else if (!addr.equals(other.addr))
            return false;
        //FIXME: currently removing cluster from equals, tests are failing
        /*
        if (cluster == null) {
            if (other.cluster != null)
                return false;
        } else if (!cluster.equals(other.cluster))
            return false;
            */
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (gateway == null) {
            if (other.gateway != null)
                return false;
        } else if (!gateway.equals(other.gateway))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (dataCenterId == null) {
            if (other.dataCenterId != null)
                return false;
        } else if (!dataCenterId.equals(other.dataCenterId))
            return false;
        if (stp != other.stp)
            return false;
        if (subnet == null) {
            if (other.subnet != null)
                return false;
        } else if (!subnet.equals(other.subnet))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (vlanId == null) {
            if (other.vlanId != null)
                return false;
        } else if (!vlanId.equals(other.vlanId))
            return false;
        if (mtu != other.mtu)
            return false;
        if (vmNetwork != other.vmNetwork) {
            return false;
        }
        return true;
    }

    public int getMtu() {
        return mtu;
    }

    public void setMtu(int mtu) {
        this.mtu = mtu;
    }

    public boolean isVmNetwork() {
        return vmNetwork;
    }

    public void setVmNetwork(boolean vmNetwork) {
        this.vmNetwork = vmNetwork;
    }
}
