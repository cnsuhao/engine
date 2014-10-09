package org.ovirt.engine.core.common.businessentities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import org.ovirt.engine.core.common.businessentities.mapping.GuidType;
import org.ovirt.engine.core.compat.Guid;

@Entity
@Table(name = "storage_domain_dynamic")
@TypeDef(name = "guid", typeClass = GuidType.class)
public class StorageDomainDynamic implements BusinessEntity<Guid> {
    private static final long serialVersionUID = -5305319985243261293L;

    public StorageDomainDynamic() {
    }

    public StorageDomainDynamic(Integer available_disk_size, Guid id, Integer used_disk_size) {
        this.availableDiskSize = available_disk_size;
        this.id = id;
        this.usedDiskSize = used_disk_size;
    }

    @Column(name = "available_disk_size")
    private Integer availableDiskSize = 0;

    public Integer getavailable_disk_size() {
        return this.availableDiskSize;
    }

    public void setavailable_disk_size(Integer value) {
        this.availableDiskSize = value;
    }

    @Id
    @Type(type = "guid")
    @Column(name = "id")
    private Guid id = new Guid();

    @Override
    public Guid getId() {
        return this.id;
    }

    @Override
    public void setId(Guid id) {
        this.id = id;
    }


    @Column(name = "used_disk_size")
    private Integer usedDiskSize = 0;

    public Integer getused_disk_size() {
        return this.usedDiskSize;
    }

    public void setused_disk_size(Integer value) {
        this.usedDiskSize = value;
    }

    public int getfreeDiskPercent() {
        Integer usedDiskSizeObj = getused_disk_size();
        Integer availableDiskSizeObj = getavailable_disk_size();
        int usedDiskSize = usedDiskSizeObj == null ? 0 : usedDiskSizeObj;
        int availableDiskSize = availableDiskSizeObj == null ? 0 : availableDiskSizeObj;

        int sum = usedDiskSize + availableDiskSize;
        int val = (sum == 0) ? 0 : (100 - (usedDiskSize * 100) / sum);
        return val;
    }

    public int getfreeDiskInGB() {
            int availableDiskSize = getavailable_disk_size() == null ? 0 : getavailable_disk_size();
            return availableDiskSize;
    }

    public static StorageDomainDynamic copyOf(StorageDomainDynamic domain) {
        StorageDomainDynamic sdd = new StorageDomainDynamic();
        sdd.availableDiskSize = domain.availableDiskSize;
        sdd.id = new Guid(domain.id.getUuid());
        sdd.usedDiskSize = domain.usedDiskSize;
        return sdd;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((availableDiskSize == null) ? 0 : availableDiskSize.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((usedDiskSize == null) ? 0 : usedDiskSize.hashCode());
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
        StorageDomainDynamic other = (StorageDomainDynamic) obj;
        if (availableDiskSize == null) {
            if (other.availableDiskSize != null)
                return false;
        } else if (!availableDiskSize.equals(other.availableDiskSize))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (usedDiskSize == null) {
            if (other.usedDiskSize != null)
                return false;
        } else if (!usedDiskSize.equals(other.usedDiskSize))
            return false;
        return true;
    }

}
