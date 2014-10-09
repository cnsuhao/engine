package org.ovirt.engine.core.vdsbroker.vdsbroker;

import java.util.List;

import org.ovirt.engine.core.vdsbroker.irsbroker.*;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.vdscommands.*;

public class CreateVGVDSCommand<P extends CreateVGVDSCommandParameters> extends VdsBrokerCommand<P> {
    private OneUuidReturnForXmlRpc _result;

    public CreateVGVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void ExecuteVdsBrokerCommand() {

        String storageDomainId = getParameters().getStorageDomainId().toString();
        List<String> deviceList = getParameters().getDeviceList();
        String[] deviceArray = deviceList.toArray(new String[deviceList.size()]);
        boolean isForce = getParameters().isForce();
        boolean supportForceCreateVG = Config.<Boolean> GetValue(
                ConfigValues.SupportForceCreateVG, getVds().getvds_group_compatibility_version().toString());

        _result = supportForceCreateVG ?
                getBroker().createVG(storageDomainId, deviceArray, isForce) :
                getBroker().createVG(storageDomainId, deviceArray);

        ProceedProxyReturnValue();
        setReturnValue(_result.mUuid);
    }

    @Override
    protected StatusForXmlRpc getReturnStatus() {
        return _result.mStatus;
    }

    @Override
    protected Object getReturnValueFromBroker() {
        return _result;
    }
}
