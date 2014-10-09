package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.VdsGroupParametersBase;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.CustomLogField;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.CustomLogFields;

@CustomLogFields({ @CustomLogField("VdsGroupName") })
public abstract class VdsGroupCommandBase<T extends VdsGroupParametersBase> extends CommandBase<T> {
    private VDSGroup _vdsGroup;

    public VdsGroupCommandBase(T parameters) {
        super(parameters);
    }

    protected VdsGroupCommandBase(Guid commandId) {
        super(commandId);
    }

    @Override
    protected VDSGroup getVdsGroup() {
        if (_vdsGroup == null) {
            _vdsGroup = getVdsGroupDAO().get(getParameters().getVdsGroupId());
        }
        return _vdsGroup;
    }

    @Override
    public String getVdsGroupName() {
        if (getVdsGroup() != null) {
            return getVdsGroup().getname();
        } else {
            return null;
        }
    }

    @Override
    protected boolean canDoAction() {
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__CLUSTER);
        return true;
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        List<PermissionSubject> permissionList = new ArrayList<PermissionSubject>();
        permissionList.add(new PermissionSubject(getParameters().getVdsGroupId(),
                VdcObjectType.VdsGroups,
                getActionType().getActionGroup()));
        return permissionList;
    }
}
