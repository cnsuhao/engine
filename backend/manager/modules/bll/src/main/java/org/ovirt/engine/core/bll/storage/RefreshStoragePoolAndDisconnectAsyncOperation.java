package org.ovirt.engine.core.bll.storage;

import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.vdscommands.RefreshStoragePoolVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public class RefreshStoragePoolAndDisconnectAsyncOperation extends ActivateDeactivateSingleAsyncOperation {
    public RefreshStoragePoolAndDisconnectAsyncOperation(java.util.ArrayList<VDS> vdss, storage_domains domain,
            storage_pool storagePool) {
        super(vdss, domain, storagePool);
    }

    @Override
    public void execute(int iterationId) {
        try {
            Guid masterDomainIdFromDb =
                    DbFacade.getInstance()
                            .getStorageDomainDao()
                            .getMasterStorageDomainIdForPool(getStoragePool().getId());
            Backend.getInstance()
                    .getResourceManager()
                    .RunVdsCommand(
                            VDSCommandType.RefreshStoragePool,
                            new RefreshStoragePoolVDSCommandParameters(getVdss().get(iterationId).getId(),
                                    getStoragePool().getId(), masterDomainIdFromDb, getStoragePool()
                                            .getmaster_domain_version()));
            StorageHelperDirector.getInstance().getItem(getStorageDomain().getstorage_type())
                    .disconnectStorageFromDomainByVdsId(getStorageDomain(), getVdss().get(iterationId).getId());
        } catch (RuntimeException e) {
            log.errorFormat("Failed to connect/refresh storagePool. Host {0} to storage pool {1}. Exception: {3}",
                    getVdss().get(iterationId).getvds_name(), getStoragePool().getname(), e);
        }

    }

    private static Log log = LogFactory.getLog(RefreshStoragePoolAndDisconnectAsyncOperation.class);
}
