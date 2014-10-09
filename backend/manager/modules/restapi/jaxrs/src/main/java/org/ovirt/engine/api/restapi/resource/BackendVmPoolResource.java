package org.ovirt.engine.api.restapi.resource;


import java.util.List;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.common.util.LinkHelper;
import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.VmPool;
import org.ovirt.engine.api.resource.ActionResource;
import org.ovirt.engine.api.resource.AssignedPermissionsResource;
import org.ovirt.engine.api.resource.CreationResource;
import org.ovirt.engine.api.resource.VmPoolResource;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.AddVmPoolWithVmsParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VmPoolUserParameters;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.vm_pools;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.GetPermissionsForObjectParameters;
import org.ovirt.engine.core.common.queries.GetVmByVmIdParameters;
import org.ovirt.engine.core.common.queries.GetVmPoolByIdParameters;
import org.ovirt.engine.core.common.queries.GetVmTemplatesDisksParameters;
import org.ovirt.engine.core.common.queries.GetVmTemplateParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.users.VdcUser;
import org.ovirt.engine.core.compat.Guid;

import static org.ovirt.engine.api.restapi.resource.BackendVmPoolsResource.SUB_COLLECTION;

public class BackendVmPoolResource
    extends AbstractBackendActionableResource<VmPool, vm_pools>
    implements VmPoolResource {

    private BackendVmPoolsResource parent;

    public BackendVmPoolResource(String id, BackendVmPoolsResource parent) {
        super(id, VmPool.class, vm_pools.class, SUB_COLLECTION);
        this.parent = parent;
    }

    @Override
    public VmPool get() {
        return performGet(VdcQueryType.GetVmPoolById, new GetVmPoolByIdParameters(guid));
    }

    @Override
    public VmPool update(VmPool incoming) {
        return performUpdate(incoming,
                             new QueryIdResolver<Guid>(VdcQueryType.GetVmPoolById,
                                                 GetVmPoolByIdParameters.class),
                             VdcActionType.UpdateVmPoolWithVms,
                             new UpdateParametersProvider());
    }

    @Override
    public AssignedPermissionsResource getPermissionsResource() {
        return inject(new BackendAssignedPermissionsResource(guid,
                                                             VdcQueryType.GetPermissionsForObject,
                                                             new GetPermissionsForObjectParameters(guid),
                                                             VmPool.class,
                                                             VdcObjectType.VmPool));
    }

    @Override
    protected VmPool deprecatedPopulate(VmPool pool, vm_pools entity) {
        return parent.deprecatedPopulate(pool, entity);
    }

    @Override
    protected VmPool doPopulate(VmPool pool, vm_pools entity) {
        return parent.doPopulate(pool, entity);
    }

    protected VM mapToVM(VmPool model) {
        return getMapper(VmPool.class, VM.class).map(model, null);
    }

    protected Guid getStorageDomainId(Guid templateId) {
        Guid storageDomainId = null;
        if (templateId != null) {
            List<DiskImage> images = asCollection(DiskImage.class,
                                                  getEntity(List.class,
                                                            VdcQueryType.GetVmTemplatesDisks,
                                                            new GetVmTemplatesDisksParameters(templateId),
                                                            templateId.toString()));
            if (images != null && images.size() > 0) {
                storageDomainId = images.get(0).getstorage_ids().get(0);
            }
        }
        return storageDomainId;
    }

    protected class UpdateParametersProvider implements ParametersProvider<VmPool, vm_pools> {
        @Override
        public VdcActionParametersBase getParameters(VmPool incoming, vm_pools current) {
            int currentVmCount = current.getvm_assigned_count();
            vm_pools entity = map(incoming, current);

            VM vm = mapToVM(map(entity));

            int size = incoming.isSetSize() && incoming.getSize() > currentVmCount
                       ? incoming.getSize() - currentVmCount
                       : 0;

            if (incoming.isSetTemplate()) {
                vm.setVmtGuid(new Guid(incoming.getTemplate().getId()));
            } else {
                VM existing = currentVmCount > 0
                              ? getEntity(VM.class, SearchType.VM, "Vms: pool=" + incoming.getName())
                              : null;
                if (existing != null) {
                    vm.setVmtGuid(existing.getVmtGuid());
                }
            }

            if (vm.getVmtGuid() != null) {
                VmTemplate template = getEntity(VmTemplate.class,
                                                VdcQueryType.GetVmTemplate,
                                                new GetVmTemplateParameters(vm.getId()),
                                                vm.getId().toString());
                vm.getStaticData().setMemSizeMb(template.getMemSizeMb());
            }

            AddVmPoolWithVmsParameters parameters = new AddVmPoolWithVmsParameters(entity, vm, size, -1);
            parameters.setStorageDomainId(getStorageDomainId(vm.getVmtGuid()));
            return parameters;
        }
    }

    @Override
    public Response allocatevm(Action action) {
        return doAction(VdcActionType.AttachUserToVmFromPoolAndRun,
                        new VmPoolUserParameters(guid,  getCurrent().get(VdcUser.class), false),
                        action,
                        new VmQueryIdResolver(VdcQueryType.GetVmByVmId,
                                              GetVmByVmIdParameters.class));

    }

    protected class VmQueryIdResolver extends EntityResolver {

        private VdcQueryType query;
        private Class<? extends VdcQueryParametersBase> queryParamsClass;

        public VmQueryIdResolver(VdcQueryType query, Class<? extends VdcQueryParametersBase> queryParamsClass) {
            this.query = query;
            this.queryParamsClass = queryParamsClass;
        }

        @Override
        public Object lookupEntity(Object id) throws BackendFailureException {
            VM vm = doGetEntity(VM.class,
                    query, getQueryParams(queryParamsClass, id), id.toString());
            org.ovirt.engine.api.model.VM model = new org.ovirt.engine.api.model.VM();
            model.setId(vm.getId().toString());
            return LinkHelper.addLinks(getUriInfo(), model);
        }
    }

    @Override
    public CreationResource getCreationSubresource(String ids) {
        return inject(new BackendCreationResource(ids));
    }

    @Override
    public ActionResource getActionSubresource(String action, String ids) {
        return inject(new BackendActionResource(action, ids));
    }

    public BackendVmPoolsResource getParent() {
        return parent;
    }
}
