package org.ovirt.engine.ui.uicommonweb.models.userportal;

import java.util.ArrayList;

import org.ovirt.engine.core.common.VdcActionUtils;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmTemplateStatus;
import org.ovirt.engine.core.compat.ObservableCollection;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.configure.UserPortalPermissionListModel;
import org.ovirt.engine.ui.uicommonweb.models.templates.TemplateListModel;
import org.ovirt.engine.ui.uicommonweb.models.templates.UserPortalTemplateDiskListModel;
import org.ovirt.engine.ui.uicommonweb.models.templates.UserPortalTemplateEventListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.TemplateVmModelBehavior;
import org.ovirt.engine.ui.uicommonweb.models.vms.UserPortalTemplateVmModelBehavior;

public class UserPortalTemplateListModel extends TemplateListModel
{
    @Override
    protected void SyncSearch()
    {
        AsyncDataProvider.GetAllVmTemplates(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object returnValue) {
                ((UserPortalTemplateListModel) model).setItems((Iterable) returnValue);
            }
        }));
    }

    @Override
    protected void UpdateActionAvailability()
    {
        VmTemplate item = (VmTemplate) getSelectedItem();
        if (item != null)
        {
            ArrayList items = new ArrayList();
            items.add(item);
            getEditCommand().setIsExecutionAllowed(
                    item.getstatus() != VmTemplateStatus.Locked &&
                            !isBlankTemplateSelected());
            getRemoveCommand().setIsExecutionAllowed(
                    VdcActionUtils.CanExecute(items, VmTemplate.class,
                            VdcActionType.RemoveVmTemplate) &&
                            !isBlankTemplateSelected()
                    );
        }
        else
        {
            getEditCommand().setIsExecutionAllowed(false);
            getRemoveCommand().setIsExecutionAllowed(false);
        }
    }

    @Override
    protected void addCustomModelsDetailModelList(ObservableCollection<EntityModel> list) {
        list.add(2, new UserPortalTemplateDiskListModel());
        list.add(new UserPortalTemplateEventListModel());
        list.add(new UserPortalPermissionListModel());
    }

    @Override
    protected TemplateVmModelBehavior createBehavior(VmTemplate template) {
        return new UserPortalTemplateVmModelBehavior(template);
    }
}
