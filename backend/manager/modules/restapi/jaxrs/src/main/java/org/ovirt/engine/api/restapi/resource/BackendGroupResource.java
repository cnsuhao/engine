package org.ovirt.engine.api.restapi.resource;

import org.ovirt.engine.api.model.Group;
import org.ovirt.engine.api.resource.AssignedPermissionsResource;
import org.ovirt.engine.api.resource.AssignedRolesResource;
import org.ovirt.engine.api.resource.AssignedTagsResource;
import org.ovirt.engine.api.resource.GroupResource;
import org.ovirt.engine.core.common.businessentities.LdapGroup;
import org.ovirt.engine.core.common.queries.GetAdGroupByIdParameters;
import org.ovirt.engine.core.common.queries.MultilevelAdministrationByAdElementIdParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;

import static org.ovirt.engine.api.restapi.resource.BackendGroupsResource.SUB_COLLECTIONS;

public class BackendGroupResource
        extends AbstractBackendSubResource<Group, LdapGroup>
        implements GroupResource {

    private BackendGroupsResource parent;

    public BackendGroupResource(String id, BackendGroupsResource parent) {
        super(id, Group.class, LdapGroup.class, SUB_COLLECTIONS);
        this.parent = parent;
    }

    @Override
    public Group get() {
        return performGet(VdcQueryType.GetAdGroupById, new GetAdGroupByIdParameters(guid));
    }

    @Override
    public AssignedRolesResource getRolesResource() {
        return inject(new BackendAssignedRolesResource(guid));
    }

    @Override
    public AssignedTagsResource getTagsResource() {
        return inject(new BackendGroupTagsResource(id));
    }

    @Override
    public AssignedPermissionsResource getPermissionsResource() {
        return inject(new BackendAssignedPermissionsResource(guid,
                                                             VdcQueryType.GetPermissionsByAdElementId,
                                                             new MultilevelAdministrationByAdElementIdParameters(guid),
                                                             Group.class));
    }

    public BackendGroupsResource getParent() {
        return parent;
    }

    @Override
    protected Group doPopulate(Group model, LdapGroup entity) {
        return model;
    }
}
