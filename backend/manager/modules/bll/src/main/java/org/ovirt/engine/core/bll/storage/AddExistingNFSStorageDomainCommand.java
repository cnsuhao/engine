package org.ovirt.engine.core.bll.storage;

import org.apache.commons.lang.StringUtils;

import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.common.action.StorageDomainManagementParameter;
import org.ovirt.engine.core.common.action.StorageServerConnectionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.SANState;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;

public class AddExistingNFSStorageDomainCommand<T extends StorageDomainManagementParameter> extends
        AddNFSStorageDomainCommand<T> {

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected AddExistingNFSStorageDomainCommand(Guid commandId) {
        super(commandId);
    }

    public AddExistingNFSStorageDomainCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected boolean CanAddDomain() {
        return CheckExistingStorageDomain();
    }

    @Override
    protected void executeCommand() {
        if (StringUtils.isEmpty(getStorageDomain().getstorage())) {
            getStorageDomain().setstorage(
                    (String) Backend
                            .getInstance()
                            .runInternalAction(
                                    VdcActionType.AddStorageServerConnection,
                                    new StorageServerConnectionParametersBase(getStorageDomain().getStorageStaticData()
                                            .getConnection(), getVds().getId())).getActionReturnValue());
        }
        AddStorageDomainInDb();
        UpdateStorageDomainDynamicFromIrs();
        setSucceeded(true);
    }

    @Override
    protected boolean ConcreteCheckExistingStorageDomain(Pair<StorageDomainStatic, SANState> domain) {
        boolean returnValue = false;
        StorageDomainStatic domainFromIrs = domain.getFirst();
        if (StringUtils.isEmpty(getStorageDomain().getStorageStaticData().getstorage())
                && StringUtils.isEmpty(domainFromIrs.getstorage()) && domainFromIrs.getConnection() != null
                && getStorageDomain().getStorageStaticData().getConnection() != null) {
            returnValue = (StringUtils.equals(domainFromIrs.getConnection().getconnection(), getStorageDomain()
                    .getStorageStaticData().getConnection().getconnection()));
        } else if (!StringUtils.isEmpty(getStorageDomain().getStorageStaticData().getstorage())
                && !StringUtils.isEmpty(domainFromIrs.getstorage())) {
            returnValue = (StringUtils.equals(domainFromIrs.getstorage(), getStorageDomain().getStorageStaticData()
                    .getstorage()));
        }
        if (!returnValue) {
            addCanDoActionMessage(VdcBllMessages.ERROR_CANNOT_ADD_EXISTING_STORAGE_DOMAIN_CONNECTION_DATA_ILLEGAL);
        }
        return returnValue;
    }
}
