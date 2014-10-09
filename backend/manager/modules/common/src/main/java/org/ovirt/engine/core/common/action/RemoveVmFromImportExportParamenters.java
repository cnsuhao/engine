package org.ovirt.engine.core.common.action;

import org.ovirt.engine.core.compat.*;
import org.ovirt.engine.core.common.businessentities.*;

public class RemoveVmFromImportExportParamenters extends RemoveVmParameters implements java.io.Serializable {
    private static final long serialVersionUID = 1841755064122049392L;
    private VM _vm;

    public RemoveVmFromImportExportParamenters(VM vm, Guid storageDomainId, Guid storagePoolId) {
        super(vm.getId(), false);
        _vm = vm;
        this.setStorageDomainId(storageDomainId);
        this.setStoragePoolId(storagePoolId);
    }

    public VM getVm() {
        return _vm;
    }

    private Guid privateStorageDomainId = new Guid();

    public Guid getStorageDomainId() {
        return privateStorageDomainId;
    }

    public void setStorageDomainId(Guid value) {
        privateStorageDomainId = value;
    }

    private Guid privateStoragePoolId = new Guid();

    public Guid getStoragePoolId() {
        return privateStoragePoolId;
    }

    public void setStoragePoolId(Guid value) {
        privateStoragePoolId = value;
    }

    public RemoveVmFromImportExportParamenters() {
    }
}
