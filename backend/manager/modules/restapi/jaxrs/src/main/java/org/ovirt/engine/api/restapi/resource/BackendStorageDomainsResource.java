package org.ovirt.engine.api.restapi.resource;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.ovirt.engine.api.common.util.StatusUtils;
import org.ovirt.engine.api.model.Fault;
import org.ovirt.engine.api.model.LogicalUnit;
import org.ovirt.engine.api.model.Storage;
import org.ovirt.engine.api.model.StorageDomain;
import org.ovirt.engine.api.model.StorageDomainStatus;
import org.ovirt.engine.api.model.StorageDomains;
import org.ovirt.engine.api.model.VolumeGroup;
import org.ovirt.engine.api.resource.StorageDomainResource;
import org.ovirt.engine.api.resource.StorageDomainsResource;
import org.ovirt.engine.core.common.action.AddSANStorageDomainParameters;
import org.ovirt.engine.core.common.action.RemoveStorageDomainParameters;
import org.ovirt.engine.core.common.action.StorageDomainManagementParameter;
import org.ovirt.engine.core.common.action.StorageDomainParametersBase;
import org.ovirt.engine.core.common.action.StorageServerConnectionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.LUNs;
import org.ovirt.engine.core.common.businessentities.StorageDomainSharedStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.GetDeviceListQueryParameters;
import org.ovirt.engine.core.common.queries.GetExistingStorageDomainListParameters;
import org.ovirt.engine.core.common.queries.GetLunsByVgIdParameters;
import org.ovirt.engine.core.common.queries.StorageDomainQueryParametersBase;
import org.ovirt.engine.core.common.queries.StorageServerConnectionQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.api.restapi.types.StorageDomainMapper;
import org.ovirt.engine.api.restapi.util.StorageDomainHelper;

import static org.ovirt.engine.api.restapi.resource.BackendStorageDomainResource.getLinksToExclude;

public class BackendStorageDomainsResource
    extends AbstractBackendCollectionResource<StorageDomain, storage_domains>
    implements StorageDomainsResource {

    static final String[] SUB_COLLECTIONS = { "permissions", "files", "templates", "vms"};

    private StorageDomain storageDomain = null; //utility variable; used in the context of a single activation of remove()

    private final EntityIdResolver<Guid> ID_RESOLVER =
        new QueryIdResolver<Guid>(VdcQueryType.GetStorageDomainById, StorageDomainQueryParametersBase.class);

    public BackendStorageDomainsResource() {
        super(StorageDomain.class, storage_domains.class, SUB_COLLECTIONS);
    }

    @Override
    public StorageDomains list() {
        if (isFiltered())
            return mapCollection(getBackendCollection(VdcQueryType.GetAllStorageDomains,
                    new VdcQueryParametersBase()));
        else
            return mapCollection(getBackendCollection(SearchType.StorageDomain));
    }

    @Override
    @SingleEntityResource
    public StorageDomainResource getStorageDomainSubResource(String id) {
        return inject(new BackendStorageDomainResource(id, this));
    }

    private Response addNfsOrLocalOrPosix(VdcActionType action, StorageDomain model, StorageDomainStatic entity, Guid hostId) {
        StorageServerConnections cnx = mapToCnx(model);

        entity.setstorage(addStorageServerConnection(cnx, hostId));

        if (action == VdcActionType.AddNFSStorageDomain) {
            storage_domains existing =
                getExistingStorageDomain(hostId,
                                         entity.getstorage_type(),
                                         entity.getstorage_domain_type(),
                                         cnx);
            if (existing != null) {
                entity = existing.getStorageStaticData();
                action = VdcActionType.AddExistingNFSStorageDomain;
            }
        }

        if (action != VdcActionType.AddExistingNFSStorageDomain) {
            validateParameters(model, 2, "name");
        }

        return performCreate(action, getAddParams(entity, hostId), ID_RESOLVER);
    }

    private Response addSAN(StorageDomain model, StorageType storageType, StorageDomainStatic entity, Guid hostId) {
        boolean overrideLuns = model.getStorage().isSetOverrideLuns() ? model.getStorage().isOverrideLuns() : false;

        return performCreate(VdcActionType.AddSANStorageDomain,
                               getSanAddParams(entity,
                                               hostId,
                                               getLunIds(model.getStorage(), storageType, hostId),
                                               overrideLuns),
                               ID_RESOLVER);
    }

    private ArrayList<String> getLunIds(Storage storage, StorageType storageType, Guid hostId) {
        List<LogicalUnit> logicalUnits = new ArrayList<LogicalUnit>();

        if (storage.isSetLogicalUnits()) {
            logicalUnits = storage.getLogicalUnits();
        } else if (storage.isSetVolumeGroup() &&
                   storage.getVolumeGroup().isSetLogicalUnits()) {
            logicalUnits = storage.getVolumeGroup().getLogicalUnits();
        }

        ArrayList<String> lunIds = new ArrayList<String>();
        for (LogicalUnit unit : logicalUnits) {
            validateParameters(unit, 4, "id");
            //if the address and target were not supplied, we understand from this that
            //the user assumes that the host is already logged-in to the target of this lun.
            //so in this case we do not need (and do not have the required information) to login
            //to the target.
            if ( (storageType == StorageType.ISCSI) && (!isConnectionAssumed(unit)) ){
                connectStorageToHost(hostId, storageType, unit);
            }
            lunIds.add(unit.getId());
        }
        refreshHostStorage(hostId);
        return !lunIds.isEmpty() ? lunIds : null;
    }

    private boolean isConnectionAssumed(LogicalUnit unit) {
        //either 'target' and 'address' should both be provided, or none. Validate this
        if ( (unit.getAddress()!=null || unit.getTarget()!=null) ) {
            validateParameters(unit, "address", "target");
        }
        boolean connectionAssumed = (unit.getAddress()==null || unit.getTarget()==null);
        return connectionAssumed;
    }

    /**
     * This is a work-around for a VDSM bug. The call to GetDeviceList causes a necessary
     * refresh in the VDSM, without which the creation will fail.
     * @param hostId
     */
    private void refreshHostStorage(Guid hostId) {
        getBackendCollection(VdcQueryType.GetDeviceList, new GetDeviceListQueryParameters(hostId, StorageType.ISCSI));
    }

    private void connectStorageToHost(Guid hostId, StorageType storageType, LogicalUnit unit) {
        StorageServerConnections cnx = StorageDomainHelper.getConnection(storageType, unit.getAddress(), unit.getTarget(), unit.getUsername(), unit.getPassword(), unit.getPort());
        performAction(VdcActionType.ConnectStorageToVds,
                      new StorageServerConnectionParametersBase(cnx, hostId));
    }

    @Override
    public Response add(StorageDomain storageDomain) {
        validateParameters(storageDomain, "host.id|name", "type", "storage.type");
        validateEnums(StorageDomain.class, storageDomain);

        StorageDomainStatic entity = mapToStatic(storageDomain);
        Guid hostId = getHostId(storageDomain);

        Response resp = null;
        switch (entity.getstorage_type()) {
        case ISCSI:
        case FCP:
            resp = addSAN(storageDomain, entity.getstorage_type(), entity, hostId);
            break;
        case NFS:
            validateParameters(storageDomain.getStorage(), "address", "path");
            resp = addNfsOrLocalOrPosix(VdcActionType.AddNFSStorageDomain, storageDomain, entity, hostId);
            break;
        case LOCALFS:
            validateParameters(storageDomain.getStorage(), "path");
            resp = addNfsOrLocalOrPosix(VdcActionType.AddLocalStorageDomain, storageDomain, entity, hostId);
            break;
        case POSIXFS:
            validateParameters(storageDomain.getStorage(), "path", "vfsType");
            resp = addNfsOrLocalOrPosix(VdcActionType.AddPosixFsStorageDomain, storageDomain, entity, hostId);
            break;

        default:
            break;
        }

        addLinks(((StorageDomain) resp.getEntity()), getLinksToExclude(storageDomain));
        return resp;
    }

    @Override
    public Response remove(String id, StorageDomain storageDomain) {
        if (storageDomain==null) {
            Fault fault = new Fault();
            fault.setReason("storage-domain parameter is missing");
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity(fault).build());
        }
        validateParameters(storageDomain, "host.id|name");
        this.storageDomain = storageDomain;
        return super.remove(id);
    }

    protected StorageDomainStatic mapToStatic(StorageDomain model) {
        return getMapper(modelType, StorageDomainStatic.class).map(model, null);
    }

    @Override
    protected StorageDomain map(storage_domains entity, StorageDomain template) {
        StorageDomain model = super.map(entity, template);

        // Mapping the connection properties only in case it is a non-filtered session
        if (!isFiltered()) {
            switch (entity.getstorage_type()) {
                case ISCSI:
                    mapVolumeGroupIscsi(model, entity);
                    break;
                case FCP:
                    mapVolumeGroupFcp(model, entity);
                    break;
                case NFS:
                case LOCALFS:
                case POSIXFS:
                    mapNfsOrLocalOrPosix(model, entity);
                    break;
                }
        }

        return model;
    }

    protected void mapNfsOrLocalOrPosix(StorageDomain model, storage_domains entity) {
        final Storage storage = model.getStorage();
        StorageServerConnections cnx = getStorageServerConnection(entity.getstorage());
        if (cnx.getconnection().contains(":")) {
            String[] parts = cnx.getconnection().split(":");
            model.getStorage().setAddress(parts[0]);
            model.getStorage().setPath(parts[1]);
        } else {
            model.getStorage().setPath(cnx.getconnection());
        }
        storage.setMountOptions(cnx.getMountOptions());
        storage.setVfsType(cnx.getVfsType());
        if (cnx.getNfsRetrans()!=null) {
            storage.setNfsRetrans(cnx.getNfsRetrans().intValue());
        }
        if (cnx.getNfsTimeo()!=null) {
            storage.setNfsTimeo(cnx.getNfsTimeo().intValue());
        }
        if (cnx.getNfsVersion()!=null) {
            storage.setNfsVersion(StorageDomainMapper.map(cnx.getNfsVersion(), null));
        }
    }

    protected void mapVolumeGroupIscsi(StorageDomain model, storage_domains entity) {
        VolumeGroup vg = model.getStorage().getVolumeGroup();
        for (LUNs lun : getLunsByVgId(vg.getId())) {
            List<StorageServerConnections> lunConnections = lun.getLunConnections();
            if (lunConnections!=null) {
                for (StorageServerConnections cnx : lunConnections) {
                    LogicalUnit unit = map(lun);
                    unit = map(cnx, unit);
                    vg.getLogicalUnits().add(unit);
                }
            }
        }
    }

    protected void mapVolumeGroupFcp(StorageDomain model, storage_domains entity) {
        VolumeGroup vg = model.getStorage().getVolumeGroup();
        for (LUNs lun : getLunsByVgId(vg.getId())) {
            LogicalUnit unit = map(lun);
            vg.getLogicalUnits().add(unit);
        }
    }

    protected LogicalUnit map(LUNs lun) {
        return getMapper(LUNs.class, LogicalUnit.class).map(lun, null);
    }

    protected LogicalUnit map(StorageServerConnections cnx, LogicalUnit template) {
        return getMapper(StorageServerConnections.class, LogicalUnit.class).map(cnx, template);
    }

    protected StorageType map(org.ovirt.engine.api.model.StorageType type) {
        return getMapper(org.ovirt.engine.api.model.StorageType.class, StorageType.class).map(type, null);
    }

    protected org.ovirt.engine.api.model.StorageType map(StorageType type) {
        return getMapper(StorageType.class, org.ovirt.engine.api.model.StorageType.class).map(type, null);
    }

    private StorageDomains mapCollection(List<storage_domains> entities) {
        StorageDomains collection = new StorageDomains();
        for (storage_domains entity : entities) {
            StorageDomain storageDomain = map(entity);
            //status is only relevant in the context of a data-center, so it can either be 'Unattached' or null.
            if (StorageDomainSharedStatus.Unattached.equals(entity.getstorage_domain_shared_status())) {
                storageDomain.setStatus(StatusUtils.create(StorageDomainStatus.UNATTACHED));
            } else {
                storageDomain.setStatus(null);
            }
            collection.getStorageDomains().add(addLinks(storageDomain, getLinksToExclude(storageDomain)));
        }
        return collection;
    }

    protected StorageServerConnections mapToCnx(StorageDomain model) {
        return getMapper(StorageDomain.class,
                         StorageServerConnections.class).map(model, null);
    }

    private Guid getHostId(StorageDomain storageDomain) {
        // presence of host ID or name already validated
        return storageDomain.getHost().isSetId()
               ? new Guid(storageDomain.getHost().getId())
               : storageDomain.getHost().isSetName()
                 ? getEntity(VDS.class,
                             SearchType.VDS,
                             "Hosts: name=" + storageDomain.getHost().getName()).getId()
                 : null;
    }

    private String addStorageServerConnection(StorageServerConnections cnx, Guid hostId) {
        return performAction(VdcActionType.AddStorageServerConnection,
                             new StorageServerConnectionParametersBase(cnx, hostId),
                             String.class);
    }

    private StorageServerConnections getStorageServerConnection(String id) {
        return getEntity(StorageServerConnections.class,
                         VdcQueryType.GetStorageServerConnectionById,
                         new StorageServerConnectionQueryParametersBase(id),
                         "Storage server connection: id=" + id);
    }

    private List<LUNs> getLunsByVgId(String vgId) {
        return asCollection(LUNs.class,
                            getEntity(List.class,
                                      VdcQueryType.GetLunsByVgId,
                                      new GetLunsByVgIdParameters(vgId),
                                      "LUNs for volume group: id=" + vgId));
    }

    private storage_domains getExistingStorageDomain(Guid hostId,
                                                           StorageType storageType,
                                                           StorageDomainType domainType,
                                                           StorageServerConnections cnx) {
        List<storage_domains> existing =
            asCollection(storage_domains.class,
                         getEntity(ArrayList.class,
                                   VdcQueryType.GetExistingStorageDomainList,
                                   new GetExistingStorageDomainListParameters(hostId,
                                                                              storageType,
                                                                              domainType,
                                                                              cnx.getconnection()),
                                   "Existing storage domains: path=" + cnx.getconnection()));
        return existing.size() != 0 ? existing.get(0) : null;
    }

    private StorageDomainManagementParameter getAddParams(StorageDomainStatic entity, Guid hostId) {
        StorageDomainManagementParameter params = new StorageDomainManagementParameter(entity);
        params.setVdsId(hostId);
        return params;
    }

    private AddSANStorageDomainParameters getSanAddParams(StorageDomainStatic entity,
                                                          Guid hostId,
                                                          ArrayList<String> lunIds,
                                                          boolean force) {
        AddSANStorageDomainParameters params = new AddSANStorageDomainParameters(entity);
        params.setVdsId(hostId);
        params.setLunIds(lunIds);
        params.setForce(force);
        return params;
    }

    @Override
    protected Response performRemove(String id) {
        if (storageDomain.isSetDestroy() && storageDomain.isDestroy()) {
            StorageDomainParametersBase parameters = new StorageDomainParametersBase(asGuidOr404(id));
            parameters.setVdsId(getHostId(storageDomain));
            return performAction(VdcActionType.ForceRemoveStorageDomain, parameters);
        } else {
            RemoveStorageDomainParameters parameters = new RemoveStorageDomainParameters(asGuidOr404(id));
            parameters.setVdsId(getHostId(storageDomain));
            if (storageDomain.isSetFormat()) {
                parameters.setDoFormat(storageDomain.isFormat());
            }
            return performAction(VdcActionType.RemoveStorageDomain, parameters);
        }
    }

    @Override
    protected StorageDomain doPopulate(StorageDomain model, storage_domains entity) {
        return model;
    }
}
