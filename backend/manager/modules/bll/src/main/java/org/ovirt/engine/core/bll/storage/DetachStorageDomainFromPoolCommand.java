package org.ovirt.engine.core.bll.storage;

import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.DetachStorageDomainFromPoolParameters;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StoragePoolIsoMapId;
import org.ovirt.engine.core.common.businessentities.StoragePoolIsoMap;
import org.ovirt.engine.core.common.vdscommands.DetachStorageDomainVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.IrsBaseVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@NonTransactiveCommandAttribute(forceCompensation=true)
public class DetachStorageDomainFromPoolCommand<T extends DetachStorageDomainFromPoolParameters> extends
        StorageDomainCommandBase<T> {
    public DetachStorageDomainFromPoolCommand(T parameters) {
        super(parameters);
    }

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */

    protected DetachStorageDomainFromPoolCommand(Guid commandId) {
        super(commandId);
    }

    @Override
    protected void executeCommand() {
        log.info("Start detach storage domain");
        changeStorageDomainStatusInTransaction(getStorageDomain().getStoragePoolIsoMapData(),
                StorageDomainStatus.Locked);
        log.info(" Detach storage domain: before connect");
        connectAllHostsToPool();

        log.info(" Detach storage domain: after connect");

        VDSReturnValue returnValue = runVdsCommand(
                VDSCommandType.DetachStorageDomain,
                new DetachStorageDomainVDSCommandParameters(getParameters().getStoragePoolId(),
                        getParameters().getStorageDomainId(), Guid.Empty, getStoragePool()
                                .getmaster_domain_version()));
        log.info(" Detach storage domain: after detach in vds");
        diconnectAllHostsInPool();

        log.info(" Detach storage domain: after disconnect storage");
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Object>() {
            @Override
            public Object runInTransaction() {
                StoragePoolIsoMap mapToRemove = getStorageDomain().getStoragePoolIsoMapData();
                getCompensationContext().snapshotEntity(mapToRemove);
                DbFacade.getInstance()
                        .getStoragePoolIsoMapDao()
                        .remove(new StoragePoolIsoMapId(mapToRemove.getstorage_id(),
                                mapToRemove.getstorage_pool_id()));
                getCompensationContext().stateChanged();
                return null;
            }
        });
        if (returnValue.getSucceeded() && getStorageDomain().getstorage_domain_type() == StorageDomainType.ISO) {
            // reset iso for this pool in vdsBroker cache
            runVdsCommand(VDSCommandType.ResetISOPath,
                    new IrsBaseVDSCommandParameters(getParameters().getStoragePoolId()));
        }
        log.info("End detach storage domain");
        setSucceeded(returnValue.getSucceeded());
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_DETACH_STORAGE_DOMAIN_FROM_POOL
                : AuditLogType.USER_DETACH_STORAGE_DOMAIN_FROM_POOL_FAILED;
    }

    @Override
    protected boolean canDoAction() {
        return canDetachDomain(getParameters().getDestroyingPool(),
                getParameters().getRemoveLast(),
                isInternalExecution());
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__STORAGE__DOMAIN);
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__DETACH);
    }
}
