package org.ovirt.engine.core.bll.network.vm;

import org.ovirt.engine.core.bll.VmCommand;
import org.ovirt.engine.core.bll.network.MacPoolManager;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.RemoveVmInterfaceParameters;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.network.VmInterfaceType;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.CustomLogField;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.CustomLogFields;

@CustomLogFields({ @CustomLogField("InterfaceName") })
public class RemoveVmInterfaceCommand<T extends RemoveVmInterfaceParameters> extends VmCommand<T> {

    private static final long serialVersionUID = 997624605993881039L;
    private String interfaceName = "";

    public RemoveVmInterfaceCommand(T parameters) {
        super(parameters);
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    @Override
    protected void executeVmCommand() {
        this.setVmName(getVmStaticDAO().get(getParameters().getVmId()).getVmName());

        // return mac to pool
        VmNetworkInterface iface = getVmNetworkInterfaceDao().get(getParameters().getInterfaceId());

        if (iface != null) {
            MacPoolManager.getInstance().freeMac(iface.getMacAddress());
            interfaceName = iface.getName();

            // Get Interface type.
            String interType = VmInterfaceType.forValue(iface.getType()).getDescription();
            if (interType != null) {
                AddCustomValue("InterfaceType", interType);
            }
        }

        // remove from db
        getVmNetworkInterfaceDao().remove(getParameters().getInterfaceId());
        getDbFacade().getVmNetworkStatisticsDao().remove(getParameters().getInterfaceId());
        getDbFacade().getVmDeviceDao().remove(new VmDeviceId(getParameters().getInterfaceId(),
                getParameters().getVmId()));
        setSucceeded(true);
    }

    @Override
    protected boolean canDoAction() {
        VmDynamic vm = getVmDynamicDao().get(getParameters().getVmId());
        if (vm.getstatus() != VMStatus.Down
                && getDbFacade().getVmDeviceDao()
                        .get(new VmDeviceId(getParameters().getInterfaceId(), getParameters().getVmId()))
                        .getIsPlugged()) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_CANNOT_REMOVE_ACTIVE_DEVICE);
            return false;
        }
        return true;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.NETWORK_REMOVE_VM_INTERFACE
                : AuditLogType.NETWORK_REMOVE_VM_INTERFACE_FAILED;
    }
}
