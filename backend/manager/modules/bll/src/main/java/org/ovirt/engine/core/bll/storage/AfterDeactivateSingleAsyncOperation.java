package org.ovirt.engine.core.bll.storage;

import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VdsSpmStatus;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.vdscommands.RefreshStoragePoolVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public class AfterDeactivateSingleAsyncOperation extends ActivateDeactivateSingleAsyncOperation {
    private final boolean _isLastMaster;
    private Guid _newMasterStorageDomainId = new Guid();

    public AfterDeactivateSingleAsyncOperation(java.util.ArrayList<VDS> vdss, storage_domains domain,
            storage_pool storagePool, boolean isLastMaster, Guid newMasterStorageDomain) {
        super(vdss, domain, storagePool);
        _isLastMaster = isLastMaster;
        _newMasterStorageDomainId = newMasterStorageDomain;
    }

    @Override
    public void execute(int iterationId) {
        try {
            log.infoFormat("After deactivate treatment vds: {0},pool {1}", getVdss().get(iterationId).getvds_name(),
                    getStoragePool().getname());
            if (!_isLastMaster) {
                Backend.getInstance()
                        .getResourceManager()
                        .RunVdsCommand(
                                VDSCommandType.RefreshStoragePool,
                                new RefreshStoragePoolVDSCommandParameters(getVdss().get(iterationId).getId(),
                                        getStoragePool().getId(),
                                        !_newMasterStorageDomainId.equals(Guid.Empty) ? _newMasterStorageDomainId
                                                : DbFacade.getInstance()
                                                        .getStorageDomainDao()
                                                        .getMasterStorageDomainIdForPool(
                                                                getStoragePool().getId()), getStoragePool()
                                                .getmaster_domain_version()));
            }

            if (getVdss().get(iterationId).getspm_status() == VdsSpmStatus.None) {
                StorageHelperDirector.getInstance().getItem(getStorageDomain().getstorage_type())
                        .disconnectStorageFromDomainByVdsId(getStorageDomain(), getVdss().get(iterationId).getId());
            }
        } catch (RuntimeException e) {
            log.errorFormat("Failed to refresh storagePool. Host {0} to storage pool {1}. Exception: {3}", getVdss()
                    .get(iterationId).getvds_name(), getStoragePool().getname(), e);
        }
    }

    private static Log log = LogFactory.getLog(AfterDeactivateSingleAsyncOperation.class);
}
