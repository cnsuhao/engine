package org.ovirt.engine.core.bll.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ovirt.engine.core.bll.InternalCommandAttribute;
import org.ovirt.engine.core.common.action.StoragePoolParametersBase;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

@InternalCommandAttribute
public abstract class ConnectHostToStoragePooServerCommandBase<T extends StoragePoolParametersBase> extends
        StorageHandlingCommandBase<T> {
    private List<StorageServerConnections> _connections;
    private List<StorageServerConnections> _isoConnections;
    private List<StorageServerConnections> _exportConnections;
    private StorageType _isoType = StorageType.NFS;
    private StorageType _exportType = StorageType.NFS;
    private boolean _needToConnectIso = false;
    private boolean _needToConnectExport = false;

    public ConnectHostToStoragePooServerCommandBase(T parameters) {
        super(parameters);
    }

    protected boolean getNeedToConnectIso() {
        return _needToConnectIso;
    }

    protected void setNeedToConnectIso(boolean value) {
        _needToConnectIso = value;
    }

    protected boolean getNeedToConnectExport() {
        return _needToConnectExport;
    }

    protected void setNeedToConnectExport(boolean value) {
        _needToConnectExport = value;
    }

    protected List<StorageServerConnections> getConnections() {
        return _connections;
    }

    protected List<StorageServerConnections> getIsoConnections() {
        return _isoConnections;
    }

    protected List<StorageServerConnections> getExportConnections() {
        return _exportConnections;
    }

    protected StorageType getIsoType() {
        return _isoType;
    }

    protected StorageType getExportType() {
        return _exportType;
    }

    protected void InitConnectionList() {
        List<storage_domains> allDomains = DbFacade.getInstance().getStorageDomainDao().getAllForStoragePool(
                getStoragePool().getId());
        List<storage_domains> isoDomains = getStorageDomainsByStoragePoolId(allDomains, StorageDomainType.ISO);
        List<storage_domains> exportDomains =
                getStorageDomainsByStoragePoolId(allDomains, StorageDomainType.ImportExport);

        Set<StorageServerConnections> connections = new HashSet<StorageServerConnections>(
                DbFacade.getInstance().getStorageServerConnectionDao().getAllForStoragePool(getStoragePool().getId()));
        if (isoDomains.size() != 0) {
            _isoType = isoDomains.get(0).getstorage_type();
            Set<StorageServerConnections> isoConnections =
                    new HashSet<StorageServerConnections>(
                            StorageHelperDirector.getInstance().getItem(getIsoType())
                                    .getStorageServerConnectionsByDomain(isoDomains.get(0).getStorageStaticData()));
            if (_isoType != getStoragePool().getstorage_pool_type()) {
                connections.removeAll(isoConnections);
            } else {
                isoConnections.removeAll(connections);
            }
            _isoConnections = new ArrayList<StorageServerConnections>(isoConnections);
            setNeedToConnectIso(_isoConnections.size() > 0);
        }
        if (exportDomains.size() != 0) {
            _exportType = exportDomains.get(0).getstorage_type();
            Set<StorageServerConnections> exportConnections =
                    new HashSet<StorageServerConnections>(
                            StorageHelperDirector.getInstance().getItem(getExportType())
                                    .getStorageServerConnectionsByDomain(exportDomains.get(0).getStorageStaticData()));
            if (_exportType != getStoragePool().getstorage_pool_type()) {
                connections.removeAll(exportConnections);
            } else {
                exportConnections.removeAll(connections);
            }
            _exportConnections = new ArrayList<StorageServerConnections>(exportConnections);
            setNeedToConnectExport(exportConnections.size() > 0);
        }
        _connections = new ArrayList<StorageServerConnections>(connections);
    }

    protected List<storage_domains> getStorageDomainsByStoragePoolId(List<storage_domains> allDomains, StorageDomainType type) {
        List<storage_domains> domains = new ArrayList<storage_domains>();
        for (storage_domains s : allDomains) {
            StorageDomainStatus status = s.getstatus();
            if (s.getstorage_domain_type() == type
                    && (StorageDomainStatus.Active == status || StorageDomainStatus.Unknown == status)) {
                domains.add(s);
            }
        }
        return domains;
    }
}
