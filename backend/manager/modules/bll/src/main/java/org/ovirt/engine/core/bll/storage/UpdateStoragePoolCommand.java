package org.ovirt.engine.core.bll.storage;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.utils.VersionSupport;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.StoragePoolManagementParameter;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StorageFormatType;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.interfaces.VDSBrokerFrontend;
import org.ovirt.engine.core.common.vdscommands.SetStoragePoolDescriptionVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.UpgradeStoragePoolVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.StorageDomainStaticDAO;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@NonTransactiveCommandAttribute
public class UpdateStoragePoolCommand<T extends StoragePoolManagementParameter> extends
        StoragePoolManagementCommandBase<T> {
    public UpdateStoragePoolCommand(T parameters) {
        super(parameters);
    }

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */

    protected UpdateStoragePoolCommand(Guid commandId) {
        super(commandId);
    }

    private storage_pool _oldStoragePool;

    @Override
    protected void executeCommand() {
        updateQuotaCache();
        if (_oldStoragePool.getstatus() == StoragePoolStatus.Up) {
            if (!StringUtils.equals(_oldStoragePool.getname(), getStoragePool().getname())) {
                runVdsCommand(VDSCommandType.SetStoragePoolDescription,
                    new SetStoragePoolDescriptionVDSCommandParameters(
                        getStoragePool().getId(), getStoragePool().getname())
                );
            }
        }

        copyUnchangedStoragePoolProperties(getStoragePool(), _oldStoragePool);

        getStoragePoolDAO().updatePartial(getStoragePool());

        updateStoragePoolFormatType();
        setSucceeded(true);
    }

    private void updateQuotaCache() {
        if(wasQuotaEnforcementDisabled()){
            getQuotaManager().removeStoragePoolFromCache(getStoragePool().getId());
        }
    }

    /**
     * Checks whether part of the update was disabling quota enforcement on the Data Center
     */
    private boolean wasQuotaEnforcementDisabled() {
        return _oldStoragePool.getQuotaEnforcementType() != QuotaEnforcementTypeEnum.DISABLED
                && getStoragePool().getQuotaEnforcementType() == QuotaEnforcementTypeEnum.DISABLED;
    }

    private void updateStoragePoolFormatType() {
        final storage_pool storagePool = getStoragePool();
        final Guid spId = storagePool.getId();
        final Version spVersion = storagePool.getcompatibility_version();
        final Version oldSpVersion = _oldStoragePool.getcompatibility_version();
        final StorageFormatType targetFormat;

        if (Version.OpEquality(spVersion, oldSpVersion)) {
            return;
        }

        // TODO: The entire version -> format type scheme should be moved to a place
        // when everyone can utilize it.
        if (spVersion.compareTo(Version.v3_0) == 0) {
            targetFormat = StorageFormatType.V2;
        } else if (spVersion.compareTo(Version.v3_1) >= 0) {
            targetFormat = StorageFormatType.V3;
        } else {
            targetFormat = StorageFormatType.V1;
        }

        StorageType spType = storagePool.getstorage_pool_type();
        if (targetFormat == StorageFormatType.V2
                && spType != StorageType.ISCSI && spType != StorageType.FCP) {
            // There is no format V2 for domains that aren't ISCSI/FCP
            return;
        }

        storagePool.setStoragePoolFormatType(targetFormat);

        TransactionSupport.executeInScope(TransactionScopeOption.RequiresNew,
                new TransactionMethod<Object>() {
                    @Override
                    public Object runInTransaction() {
                             getStoragePoolDAO().updatePartial(storagePool);
                        updateMemberDomainsFormat(targetFormat);
                        return null;
                    }
        });

        if (_oldStoragePool.getstatus() == StoragePoolStatus.Up) {
            // No need to worry about "reupgrading" as VDSM will silently ignore
            // the request.
            runVdsCommand(VDSCommandType.UpgradeStoragePool,
                new UpgradeStoragePoolVDSCommandParameters(spId, targetFormat));
        }
    }

    private void updateMemberDomainsFormat(StorageFormatType targetFormat) {
        Guid spId = getStoragePool().getId();
        StorageDomainStaticDAO sdStatDao = DbFacade.getInstance().getStorageDomainStaticDao();
        List<StorageDomainStatic> domains = sdStatDao.getAllForStoragePool(spId);
        for (StorageDomainStatic domain : domains) {
            StorageDomainType sdType = domain.getstorage_domain_type();

            if (sdType == StorageDomainType.Data || sdType == StorageDomainType.Master) {
                log.infoFormat("Updating storage domain {0} (type {1}) to format {2}",
                               domain.getId(), sdType, targetFormat);

                domain.setStorageFormat(targetFormat);
                sdStatDao.update(domain);
            }
        }
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_UPDATE_STORAGE_POOL : AuditLogType.USER_UPDATE_STORAGE_POOL_FAILED;
    }

    @Override
    protected boolean canDoAction() {
        boolean returnValue = super.canDoAction() && checkStoragePool();
        _oldStoragePool = getStoragePoolDAO().get(getStoragePool().getId());
        if (returnValue && !StringUtils.equals(_oldStoragePool.getname(), getStoragePool().getname())
                && getStoragePoolDAO().getByName(getStoragePool().getname()) != null) {
            returnValue = false;
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_POOL_NAME_ALREADY_EXIST);
        }
        if (returnValue
                && _oldStoragePool.getstorage_pool_type() != getStoragePool()
                        .getstorage_pool_type()
                && getStorageDomainStaticDAO().getAllForStoragePool(getStoragePool().getId()).size() > 0) {
            returnValue = false;
            getReturnValue()
                    .getCanDoActionMessages()
                    .add(VdcBllMessages.ERROR_CANNOT_CHANGE_STORAGE_POOL_TYPE_WITH_DOMAINS
                            .toString());
        }
        returnValue = returnValue && CheckStoragePoolNameLengthValid();
        if (returnValue
                && Version.OpInequality(_oldStoragePool.getcompatibility_version(), getStoragePool()
                        .getcompatibility_version())) {
            if (!isStoragePoolVersionSupported()) {
                addCanDoActionMessage(VersionSupport.getUnsupportedVersionMessage());
                returnValue = false;
            }
            // decreasing of compatibility version is not allowed
            else if (getStoragePool().getcompatibility_version().compareTo(_oldStoragePool.getcompatibility_version()) < 0) {
                returnValue = false;
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_CANNOT_DECREASE_COMPATIBILITY_VERSION);
            } else {
                // Check all clusters has at least the same compatibility version.
                returnValue = checkAllClustersLevel();
            }
        }

        StoragePoolValidator validator = createStoragePoolValidator();
        if (returnValue) {
            returnValue = validator.isNotLocalfsWithDefaultCluster();
        }
        if (returnValue) {
            returnValue = validator.isPosixDcAndMatchingCompatiblityVersion();
        }
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__UPDATE);
        return returnValue;
    }

    protected boolean checkAllClustersLevel() {
        boolean returnValue = true;
        List<VDSGroup> clusters = getVdsGroupDAO().getAllForStoragePool(getStoragePool().getId());
        List<String> lowLevelClusters = new ArrayList<String>();
        for (VDSGroup cluster : clusters) {
            if (getStoragePool().getcompatibility_version().compareTo(cluster.getcompatibility_version()) > 0) {
                lowLevelClusters.add(cluster.getname());
            }
        }
        if (!lowLevelClusters.isEmpty()) {
            returnValue = false;
            getReturnValue().getCanDoActionMessages().add(String.format("$ClustersList %1$s",
                    StringUtils.join(lowLevelClusters, ",")));
            getReturnValue()
                    .getCanDoActionMessages()
                    .add(VdcBllMessages.ERROR_CANNOT_UPDATE_STORAGE_POOL_COMPATIBILITY_VERSION_BIGGER_THAN_CLUSTERS
                            .toString());
        }
        return returnValue;
    }

    protected StoragePoolValidator createStoragePoolValidator() {
        return new StoragePoolValidator(getStoragePool(), getReturnValue().getCanDoActionMessages());
    }

    protected boolean isStoragePoolVersionSupported() {
        return VersionSupport.checkVersionSupported(getStoragePool().getcompatibility_version());
    }

    /* Getters for external resources and handlers */

    protected VDSBrokerFrontend getResourceManager() {
        return Backend.getInstance().getResourceManager();
    }

    protected StorageDomainStaticDAO getStorageDomainStaticDAO() {
        return DbFacade.getInstance().getStorageDomainStaticDao();
    }

    /**
     * Copy properties from old entity which assumed not to be available in the param object.
     *
     * @param oldStoragePool
     * @param newStoragePool
     */
    private static void copyUnchangedStoragePoolProperties(storage_pool newStoragePool, storage_pool oldStoragePool) {
        newStoragePool.setStoragePoolFormatType(oldStoragePool.getStoragePoolFormatType());
    }
}
