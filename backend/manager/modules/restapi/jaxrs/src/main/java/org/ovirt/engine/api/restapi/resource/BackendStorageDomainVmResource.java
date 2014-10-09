package org.ovirt.engine.api.restapi.resource;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.VM;
import org.ovirt.engine.api.model.VMs;
import org.ovirt.engine.api.resource.ActionResource;
import org.ovirt.engine.api.resource.StorageDomainContentResource;
import org.ovirt.engine.core.common.action.ImportVmParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.queries.GetVmByVmIdParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendStorageDomainVmResource
    extends AbstractBackendStorageDomainContentResource<VMs, VM, org.ovirt.engine.core.common.businessentities.VM>
    implements StorageDomainContentResource<VM> {

    public BackendStorageDomainVmResource(BackendStorageDomainVmsResource parent, String vmId) {
        super(vmId, parent, VM.class, org.ovirt.engine.core.common.businessentities.VM.class);
    }

    @Override
    protected VM getFromDataDomain() {
        return performGet(VdcQueryType.GetVmByVmId, new GetVmByVmIdParameters(guid));
    }

    @Override
    public Response doImport(Action action) {
        validateParameters(action, "cluster.id|name");

        Guid destStorageDomainId = getDestStorageDomainId(action);

        ImportVmParameters params = new ImportVmParameters(getEntity(),
                                                           parent.getStorageDomainId(),
                                                           destStorageDomainId,
                                                           parent.getDataCenterId(destStorageDomainId),
                                                           getClusterId(action));
        params.setImageToDestinationDomainMap(getDiskToDestinationMap(action));
        params.setForceOverride(action.isSetExclusive() ? action.isExclusive() : false);

        if (action.isSetVm() && action.getVm().isSetSnapshots()
                && action.getVm().getSnapshots().isSetCollapseSnapshots()) {
            params.setCopyCollapse(action.getVm().getSnapshots().isCollapseSnapshots());
        }

        if (action.isSetClone()) {
            params.setImportAsNewEntity(action.isClone());
            if(action.isSetVm() && action.getVm().isSetName()) {
                params.getVm().setVmName(action.getVm().getName());
            }
        }

        return doAction(VdcActionType.ImportVm, params, action);
    }

    @Override
    public ActionResource getActionSubresource(String action, String ids) {
        return inject(new BackendActionResource(action, ids));
    }

    @Override
    protected VM addParents(VM vm) {
        vm.setStorageDomain(parent.getStorageDomainModel());
        return vm;
    }

    protected org.ovirt.engine.core.common.businessentities.VM getEntity() {
        for (org.ovirt.engine.core.common.businessentities.VM entity : parent.getEntitiesFromExportDomain()) {
            if (guid.equals(entity.getId())) {
                return entity;
            }
        }
        return entityNotFound();
    }

    @Override
    protected VM doPopulate(VM model, org.ovirt.engine.core.common.businessentities.VM entity) {
        return model;
    }

}
