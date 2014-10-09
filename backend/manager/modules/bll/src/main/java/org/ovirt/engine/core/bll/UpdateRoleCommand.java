package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.RolesOperationsParameters;
import org.ovirt.engine.core.common.businessentities.Role;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.dal.VdcBllMessages;

public class UpdateRoleCommand<T extends RolesOperationsParameters> extends RolesOperationCommandBase<T> {
    private static final long serialVersionUID = -803075031377664047L;

    public UpdateRoleCommand(T parameters) {
        super(parameters);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_UPDATE_ROLE : AuditLogType.USER_UPDATE_ROLE_FAILED;
    }

    @Override
    protected boolean canDoAction() {
        boolean returnValue = true;
        Role oldRole = getRoleDao().get(getRole().getId());
        if (oldRole == null) {
            addCanDoActionMessage(VdcBllMessages.ERROR_CANNOT_UPDATE_ROLE_ID);
            returnValue = false;
        } else {
            if (checkIfRoleIsReadOnly(getReturnValue().getCanDoActionMessages())) {
                returnValue = false;
                addCanDoActionMessage(VdcBllMessages.VAR__ACTION__UPDATE);
            } else if (!StringHelper.EqOp(getRole().getname(), oldRole.getname())
                    && getRoleDao().getByName(getRole().getname()) != null) {
                addCanDoActionMessage(VdcBllMessages.ERROR_CANNOT_UPDATE_ROLE_NAME);
                returnValue = false;
            } // changing role type isn't allowed
            else if (getRole().getType() != oldRole.getType()) {
                addCanDoActionMessage(VdcBllMessages.ERROR_CANNOT_UPDATE_ROLE_TYPE);
                returnValue = false;
            }
        }
        if (!returnValue) {
            addCanDoActionMessage(VdcBllMessages.VAR__TYPE__ROLE);
            addCanDoActionMessage(VdcBllMessages.VAR__ACTION__UPDATE);

        }
        return returnValue;
    }

    @Override
    protected void executeCommand() {
        getRoleDao().update(getRole());
        setSucceeded(true);
    }
}
