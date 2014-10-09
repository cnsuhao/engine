package org.ovirt.engine.core.bll;

import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.ActionGroupsToRoleParameter;
import org.ovirt.engine.core.common.action.RoleWithActionGroupsParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.RoleType;
import org.ovirt.engine.core.common.businessentities.Role;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@NonTransactiveCommandAttribute(forceCompensation = true)
public class AddRoleWithActionGroupsCommand<T extends RoleWithActionGroupsParameters> extends
        RolesOperationCommandBase<T> {
    private static final long serialVersionUID = -114509092576083690L;

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    public AddRoleWithActionGroupsCommand(Guid commandId) {
        super(commandId);
    }

    public AddRoleWithActionGroupsCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected boolean canDoAction() {
        if (getParameters().getActionGroups().isEmpty()) {
            addCanDoActionMessage(VdcBllMessages.ACTION_LIST_CANNOT_BE_EMPTY);
            return false;
        }
        if (getRoleDao().getByName(getRoleName()) != null) {
            addCanDoActionMessage(VdcBllMessages.VAR__ACTION__ADD);
            addCanDoActionMessage(VdcBllMessages.ERROR_CANNOT_UPDATE_ROLE_NAME);
            return false;
        }
        RoleType roleType = getRole().getType();
        if (roleType == null) {
            addCanDoActionMessage(VdcBllMessages.ROLE_TYPE_CANNOT_BE_EMPTY);
            return false;
        }
        if (roleType != RoleType.ADMIN) {
            List<ActionGroup> actionGroups = getParameters().getActionGroups();
            for (ActionGroup group : actionGroups) {
                if (group.getRoleType() == RoleType.ADMIN) {
                    addCanDoActionMessage(VdcBllMessages.CANNOT_ADD_ACTION_GROUPS_TO_ROLE_TYPE);
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void executeCommand() {
        prepareRoleForCommand();
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
            @Override
            public Void runInTransaction() {
                getRoleDao().save(getRole());
                getCompensationContext().snapshotNewEntity(getRole());
                getCompensationContext().stateChanged();
                return null;
            }
        });

        VdcReturnValueBase attachAction = Backend.getInstance().runInternalAction(
                VdcActionType.AttachActionGroupsToRole,
                new ActionGroupsToRoleParameter(getRole().getId(), getParameters().getActionGroups()));
        if (!attachAction.getCanDoAction() || !attachAction.getSucceeded()) {
            List<String> failedMsgs = getReturnValue().getExecuteFailedMessages();
            for (String msg : attachAction.getCanDoActionMessages()) {
                failedMsgs.add(msg);
            }
            setSucceeded(false);
            return;
        }
        setSucceeded(true);
        getReturnValue().setActionReturnValue(getRole().getId());
    }

    /**
     *
     */
    protected void prepareRoleForCommand() {
        // Note that the role is take from the parameters
        Role role = getRole();
        role.setId(Guid.NewGuid());
        role.setAllowsViewingChildren(false);

        for (ActionGroup group : getParameters().getActionGroups()) {
            if (group.allowsViewingChildren()) {
                role.setAllowsViewingChildren(true);
                break;
            }
        }
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_ADD_ROLE_WITH_ACTION_GROUP
                : AuditLogType.USER_ADD_ROLE_WITH_ACTION_GROUP_FAILED;
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(MultiLevelAdministrationHandler.SYSTEM_OBJECT_ID,
                VdcObjectType.System,
                getActionType().getActionGroup()));
    }
}
