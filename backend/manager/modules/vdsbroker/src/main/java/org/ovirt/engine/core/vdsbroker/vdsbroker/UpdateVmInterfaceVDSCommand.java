package org.ovirt.engine.core.vdsbroker.vdsbroker;

import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.vdscommands.VmNicDeviceVDSParameters;
import org.ovirt.engine.core.vdsbroker.xmlrpc.XmlRpcStruct;

public class UpdateVmInterfaceVDSCommand extends VdsBrokerCommand<VmNicDeviceVDSParameters> {

    public UpdateVmInterfaceVDSCommand(VmNicDeviceVDSParameters parameters) {
        super(parameters);
    }

    @Override
    protected void ExecuteVdsBrokerCommand() {
        status = getBroker().vmUpdateDevice(getParameters().getVm().getId().toString(), initDeviceStructure());
        ProceedProxyReturnValue();
    }

    protected XmlRpcStruct initDeviceStructure() {
        XmlRpcStruct deviceStruct = new XmlRpcStruct();
        deviceStruct.add(VdsProperties.DeviceType, getParameters().getVmDevice().getType());
        deviceStruct.add(VdsProperties.Alias, getParameters().getVmDevice().getAlias());

        VmNetworkInterface nic = getParameters().getNic();
        deviceStruct.add(VdsProperties.NETWORK, StringUtils.defaultString(nic.getNetworkName()));
        deviceStruct.add(VdsProperties.LINK_ACTIVE, String.valueOf(nic.isLinked()));
        deviceStruct.add(VdsProperties.PORT_MIRRORING,
                nic.isPortMirroring() && nic.getNetworkName() != null
                        ? Collections.singletonList(nic.getNetworkName()) : Collections.<String> emptyList());

        return deviceStruct;
    }

}
