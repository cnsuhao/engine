package org.ovirt.engine.core.vdsbroker.vdsbroker;

import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.network.VmInterfaceType;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.utils.VmDeviceType;
import org.ovirt.engine.core.common.vdscommands.VmNicDeviceVDSParameters;
import org.ovirt.engine.core.vdsbroker.xmlrpc.XmlRpcStringUtils;
import org.ovirt.engine.core.vdsbroker.xmlrpc.XmlRpcStruct;

public class HotPlugNicVDSCommand<P extends VmNicDeviceVDSParameters> extends VdsBrokerCommand<P> {

    protected XmlRpcStruct struct = new XmlRpcStruct();

    public HotPlugNicVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void ExecuteVdsBrokerCommand() {
        init();
        status = getBroker().hotPlugNic(struct);
        ProceedProxyReturnValue();
    }

    protected void init() {
        struct.add(VdsProperties.vm_guid, getParameters().getVm().getId().toString());
        struct.add(VdsProperties.VM_NETWORK_INTERFACE, initNicStructure());
    }

    private XmlRpcStruct initNicStructure() {
        XmlRpcStruct map = new XmlRpcStruct();
        VmNetworkInterface nic = getParameters().getNic();
        VmDevice vmDevice = getParameters().getVmDevice();

        map.add(VdsProperties.Type, VmDeviceType.INTERFACE.getName());
        map.add(VdsProperties.Device, VmDeviceType.BRIDGE.getName());
        map.add(VdsProperties.MAC_ADDR, nic.getMacAddress());
        map.add(VdsProperties.NETWORK, StringUtils.defaultString(nic.getNetworkName()));

        if (FeatureSupported.networkLinking(getParameters().getVm().getVdsGroupCompatibilityVersion())) {
            map.add(VdsProperties.LINK_ACTIVE, String.valueOf(nic.isLinked()));
        }
        addAddress(map, vmDevice.getAddress());
        map.add(VdsProperties.SpecParams, vmDevice.getSpecParams());
        map.add(VdsProperties.NIC_TYPE, VmInterfaceType.forValue(nic.getType()).name());
        map.add(VdsProperties.DeviceId, vmDevice.getId().getDeviceId().toString());

        if (vmDevice.getBootOrder() > 0) {
            map.add(VdsProperties.BootOrder, String.valueOf(vmDevice.getBootOrder()));
        }

        if (nic.isPortMirroring()) {
            map.add(VdsProperties.PORT_MIRRORING, nic.getNetworkName() == null
                    ? Collections.<String> emptyList() : Collections.singletonList(nic.getNetworkName()));
        }

        VmInfoBuilder.addNetworkFiltersToNic(map, getParameters().getVm().getVdsGroupCompatibilityVersion());
        return map;
    }

    private void addAddress(XmlRpcStruct map, String address) {
        if (org.apache.commons.lang.StringUtils.isNotBlank(address)) {
            map.add(VdsProperties.Address, XmlRpcStringUtils.string2Map(getParameters().getVmDevice().getAddress()));
        }
    }

}
