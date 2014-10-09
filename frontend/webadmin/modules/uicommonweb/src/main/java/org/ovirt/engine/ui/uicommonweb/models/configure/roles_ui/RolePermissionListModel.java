package org.ovirt.engine.ui.uicommonweb.models.configure.roles_ui;

import org.ovirt.engine.core.common.action.PermissionsOperationsParametes;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.permissions;
import org.ovirt.engine.core.common.businessentities.Role;
import org.ovirt.engine.core.common.queries.MultilevelAdministrationByRoleIdParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class RolePermissionListModel extends SearchableListModel
{

    private UICommand privateRemoveCommand;

    public UICommand getRemoveCommand()
    {
        return privateRemoveCommand;
    }

    private void setRemoveCommand(UICommand value)
    {
        privateRemoveCommand = value;
    }

    @Override
    public Role getEntity()
    {
        return (Role) super.getEntity();
    }

    public void setEntity(Role value)
    {
        super.setEntity(value);
    }

    public RolePermissionListModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().rolesPermissionsTitle());

        setRemoveCommand(new UICommand("Remove", this)); //$NON-NLS-1$

        setSearchPageSize(1000);

        UpdateActionAvailability();
    }

    @Override
    protected void SyncSearch()
    {
        super.SyncSearch();

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object ReturnValue)
            {
                RolePermissionListModel permissionListModel = (RolePermissionListModel) model;
                permissionListModel.setItems((Iterable) ((VdcQueryReturnValue) ReturnValue).getReturnValue());
            }
        };

        MultilevelAdministrationByRoleIdParameters tempVar =
                new MultilevelAdministrationByRoleIdParameters(getEntity().getId());
        tempVar.setRefresh(getIsQueryFirstTime());
        Frontend.RunQuery(VdcQueryType.GetPermissionByRoleId, tempVar, _asyncQuery);
    }

    @Override
    protected void OnEntityChanged()
    {
        super.OnEntityChanged();
        Search();
    }

    @Override
    protected void AsyncSearch()
    {
        super.AsyncSearch();

        if (getEntity() == null)
        {
            return;
        }

        setAsyncResult(Frontend.RegisterQuery(VdcQueryType.GetPermissionByRoleId,
                new MultilevelAdministrationByRoleIdParameters(getEntity().getId())));
        setItems(getAsyncResult().getData());
    }

    private void UpdateActionAvailability()
    {
        getRemoveCommand().setIsExecutionAllowed(getSelectedItem() != null
                || (getSelectedItems() != null && getSelectedItems().size() > 0));

    }

    @Override
    protected void OnSelectedItemChanged()
    {
        super.OnSelectedItemChanged();
        UpdateActionAvailability();
    }

    @Override
    protected void SelectedItemsChanged()
    {
        super.SelectedItemsChanged();
        UpdateActionAvailability();
    }

    public void Cancel()
    {
        setWindow(null);
    }

    private void remove()
    {
        if (getWindow() != null)
        {
            return;
        }

        ConfirmationModel model = new ConfirmationModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().removePermissionTitle());
        model.setHashName("remove_permission"); //$NON-NLS-1$
        model.setMessage(ConstantsManager.getInstance().getConstants().permissionMsg());

        ArrayList<String> items = new ArrayList<String>();
        for (Object a : getSelectedItems())
        {
            items.add("Role " + ((permissions) a).getRoleName() + " on User " + ((permissions) a).getOwnerName()); //$NON-NLS-1$ //$NON-NLS-2$
        }
        model.setItems(items);

        UICommand tempVar = new UICommand("OnRemove", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    private void OnRemove()
    {
        if (getSelectedItems() != null && getSelectedItems().size() > 0)
        {
            ConfirmationModel model = (ConfirmationModel) getWindow();

            if (model.getProgress() != null)
            {
                return;
            }

            ArrayList<VdcActionParametersBase> list = new ArrayList<VdcActionParametersBase>();
            for (Object perm : getSelectedItems())
            {
                PermissionsOperationsParametes tempVar = new PermissionsOperationsParametes();
                tempVar.setPermission((permissions) perm);
                list.add(tempVar);
            }

            model.StartProgress(null);

            Frontend.RunMultipleAction(VdcActionType.RemovePermission, list,
                    new IFrontendMultipleActionAsyncCallback() {
                        @Override
                        public void Executed(FrontendMultipleActionAsyncResult result) {

                            ConfirmationModel localModel = (ConfirmationModel) result.getState();
                            localModel.StopProgress();
                            Cancel();

                        }
                    }, model);
        }
    }

    @Override
    public void ExecuteCommand(UICommand command)
    {
        super.ExecuteCommand(command);

        if (command == getRemoveCommand())
        {
            remove();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnRemove")) //$NON-NLS-1$
        {
            OnRemove();
        }
        else if (StringHelper.stringsEqual(command.getName(), "Cancel")) //$NON-NLS-1$
        {
            Cancel();
        }
    }

    @Override
    protected String getListName() {
        return "RolePermissionListModel"; //$NON-NLS-1$
    }
}
