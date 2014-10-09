package org.ovirt.engine.core.bll.storage;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.StorageDomainManagementParameter;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.validation.group.UpdateEntity;
import org.ovirt.engine.core.common.vdscommands.SetStorageDomainDescriptionVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.ObjectIdentityChecker;

public class UpdateStorageDomainCommand<T extends StorageDomainManagementParameter> extends
        StorageDomainManagementCommandBase<T> {
    public UpdateStorageDomainCommand(T parameters) {
        super(parameters);
    }

    private boolean _storageDomainNameChanged;

    @Override
    protected boolean canDoAction() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__UPDATE);
        boolean returnValue = super.canDoAction() && checkStorageDomain()
                && checkStorageDomainStatus(StorageDomainStatus.Active) && checkStorageDomainNameLengthValid();
        StorageDomainStatic oldDomain =
                DbFacade.getInstance().getStorageDomainStaticDao().get(getStorageDomain().getId());

        // Only after validating the existing of the storage domain in DB, we set the field lastTimeUsedAsMaster in the
        // storage domain which is about to be updated.
        if (returnValue) {
            getStorageDomain().setLastTimeUsedAsMaster(oldDomain.getLastTimeUsedAsMaster());
        }

        // Collect changed fields to update in a list.
        List<String> props = ObjectIdentityChecker.GetChangedFields(oldDomain, getStorageDomain()
                .getStorageStaticData());

        // Allow change only to name field
        props.remove("storage_name");
        if (returnValue && props.size() > 0) {
            log.warnFormat("There was an attempt to update the following fields although they are not allowed to be updated: {0}",
                    StringUtils.join(props, ","));
            addCanDoActionMessage(VdcBllMessages.ERROR_CANNOT_CHANGE_STORAGE_DOMAIN_FIELDS);
            returnValue = false;
        }
        _storageDomainNameChanged =
                !StringUtils.equals(oldDomain.getstorage_name(), getStorageDomain().getstorage_name());
        // if domain is part of pool, and name changed, check that pool is up in
        // order to change description in spm
        if (returnValue
                && _storageDomainNameChanged
                && getStoragePool() != null
                && getStoragePool().getstatus() != StoragePoolStatus.Up) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_IMAGE_REPOSITORY_NOT_FOUND);
            returnValue = false;
        }
        if (returnValue && _storageDomainNameChanged && IsStorageWithSameNameExists()) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_DOMAIN_NAME_ALREADY_EXIST);
            returnValue = false;
        }
        return returnValue;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_UPDATE_STORAGE_DOMAIN
                : AuditLogType.USER_UPDATE_STORAGE_DOMAIN_FAILED;
    }

    @Override
    protected void executeCommand() {
        DbFacade.getInstance().getStorageDomainStaticDao().update(getStorageDomain().getStorageStaticData());
        if (_storageDomainNameChanged && getStoragePool() != null) {
            Backend.getInstance()
                    .getResourceManager()
                    .RunVdsCommand(
                            VDSCommandType.SetStorageDomainDescription,
                            new SetStorageDomainDescriptionVDSCommandParameters(getStoragePool().getId(),
                                    getStorageDomain().getId(), getStorageDomain().getstorage_name()));
        }
        setSucceeded(true);
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(UpdateEntity.class);
        return super.getValidationGroups();
    }

}
