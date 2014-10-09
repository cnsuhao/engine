package org.ovirt.engine.core.bll.network.dc;

import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.bll.MultiLevelAdministrationHandler;
import org.ovirt.engine.core.bll.PredefinedRoles;
import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.AddNetworkStoragePoolParameters;
import org.ovirt.engine.core.common.businessentities.permissions;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;

@SuppressWarnings("serial")
public class AddNetworkCommand<T extends AddNetworkStoragePoolParameters> extends NetworkCommon<T> {
    public AddNetworkCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected void executeCommand() {
        getNetwork().setId(Guid.NewGuid());
        getNetworkDAO().save(getNetwork());
        addPermissions();
        getReturnValue().setActionReturnValue(getNetwork().getId());
        setSucceeded(true);
    }

    @Override
    protected void setActionMessageParameters() {
        super.setActionMessageParameters();
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__ADD);
    }

    @Override
    protected boolean canDoAction() {
        return validate(storagePoolExists())
                && validate(vmNetworkSetCorrectly())
                && validate(stpForVmNetworkOnly())
                && validate(mtuValid())
                && validate(networkPrefixValid())
                && validate(networkDoesNotExist())
                && validate(vlanIsFree());
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.NETWORK_ADD_NETWORK : AuditLogType.NETWORK_ADD_NETWORK_FAILED;
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(CreateEntity.class);
        return super.getValidationGroups();
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(getStoragePoolId() == null ? null
                : getStoragePoolId().getValue(),
                VdcObjectType.StoragePool, getActionType().getActionGroup()));
    }

    private void addPermissions() {
        addPermissionOnNetwork(getCurrentUser().getUserId(), PredefinedRoles.NETWORK_ADMIN);

        // if the Network is for public use, set EVERYONE as a NETWORK_USER.
        if (getParameters().isPublicUse()) {
            addPermissionOnNetwork(MultiLevelAdministrationHandler.EVERYONE_OBJECT_ID, PredefinedRoles.NETWORK_USER);
        }
    }

    private void addPermissionOnNetwork(Guid userId, PredefinedRoles role) {
        permissions perms = new permissions();
        perms.setad_element_id(userId);
        perms.setObjectType(VdcObjectType.Network);
        perms.setObjectId(getNetwork().getId());
        perms.setrole_id(role.getId());
        MultiLevelAdministrationHandler.addPermission(perms);
    }

    private ValidationResult networkDoesNotExist() {
        return getNetworkByName(getNetworks()) == null
                ? ValidationResult.VALID
                : new ValidationResult(VdcBllMessages.NETWORK_NAME_ALREADY_EXISTS);
    }

    private Network getNetworkByName(List<Network> networks) {
        String networkName = getNetworkName();
        for (Network network : networks) {
            if (network.getName().equals(networkName)) {
                return network;
            }
        }
        return null;
    }
}
