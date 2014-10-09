package org.ovirt.engine.core.common.action;

import java.util.HashMap;

import javax.validation.Valid;

import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.vm_pools;
import org.ovirt.engine.core.compat.Guid;

public class AddVmPoolWithVmsParameters extends VmPoolOperationParameters {
    private static final long serialVersionUID = 4826143612049185740L;

    @Valid
    private VM _vm;
    private int _vmsCount;
    private int _diskSize;
    private HashMap<Guid, DiskImage> diskInfoDestinationMap;

    public AddVmPoolWithVmsParameters() {
    }

    public AddVmPoolWithVmsParameters(vm_pools vmPool, VM vm, int count, int diskSize) {
        super(vmPool);
        _vm = vm;
        _vmsCount = count;
        _diskSize = diskSize;
    }

    @Valid
    public VmStatic getVmStaticData() {
        return _vm.getStaticData();
    }

    public int getVmsCount() {
        return _vmsCount;
    }

    public int getDiskSize() {
        return _diskSize;
    }

    public HashMap<Guid, DiskImage> getDiskInfoDestinationMap() {
        return diskInfoDestinationMap;
    }

    public void setDiskInfoDestinationMap(HashMap<Guid, DiskImage> diskInfoDestinationMap) {
        this.diskInfoDestinationMap = diskInfoDestinationMap;
    }
}
