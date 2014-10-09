package org.ovirt.engine.ui.uicommonweb.models.users;

import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.common.SelectionTreeNodeModel;
import org.ovirt.engine.ui.uicommonweb.validation.EmailValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class EventNotificationModel extends Model
{

    private UICommand privateExpandAllCommand;

    public UICommand getExpandAllCommand()
    {
        return privateExpandAllCommand;
    }

    private void setExpandAllCommand(UICommand value)
    {
        privateExpandAllCommand = value;
    }

    private UICommand privateCollapseAllCommand;

    public UICommand getCollapseAllCommand()
    {
        return privateCollapseAllCommand;
    }

    private void setCollapseAllCommand(UICommand value)
    {
        privateCollapseAllCommand = value;
    }

    private boolean privateIsNew;

    public boolean getIsNew()
    {
        return privateIsNew;
    }

    public void setIsNew(boolean value)
    {
        privateIsNew = value;
    }

    private EntityModel privateEmail;

    public EntityModel getEmail()
    {
        return privateEmail;
    }

    private void setEmail(EntityModel value)
    {
        privateEmail = value;
    }

    private String privateOldEmail;

    public String getOldEmail()
    {
        return privateOldEmail;
    }

    public void setOldEmail(String value)
    {
        privateOldEmail = value;
    }

    private ArrayList<SelectionTreeNodeModel> eventGroupModels;

    public ArrayList<SelectionTreeNodeModel> getEventGroupModels()
    {
        return eventGroupModels;
    }

    public void setEventGroupModels(ArrayList<SelectionTreeNodeModel> value)
    {
        if ((eventGroupModels == null && value != null)
                || (eventGroupModels != null && !eventGroupModels.equals(value)))
        {
            eventGroupModels = value;
            OnPropertyChanged(new PropertyChangedEventArgs("EventGroupModels")); //$NON-NLS-1$
        }
    }

    public EventNotificationModel()
    {
        setExpandAllCommand(new UICommand("ExpandAll", this)); //$NON-NLS-1$
        setCollapseAllCommand(new UICommand("CollapseAll", this)); //$NON-NLS-1$

        setEmail(new EntityModel());
    }

    public void ExpandAll()
    {
        // EventGroupModels.Each(a => a.IsExpanded = true);
        for (SelectionTreeNodeModel a : getEventGroupModels())
        {
            a.setIsExpanded(true);
        }
    }

    public void CollapseAll()
    {
        // EventGroupModels.Each(a => a.IsExpanded = false);
        for (SelectionTreeNodeModel a : getEventGroupModels())
        {
            a.setIsExpanded(false);
        }
    }

    public boolean Validate()
    {
        getEmail().ValidateEntity(new IValidation[] { new NotEmptyValidation(), new EmailValidation() });

        return getEmail().getIsValid();
    }

    @Override
    public void ExecuteCommand(UICommand command)
    {
        super.ExecuteCommand(command);

        if (command == getExpandAllCommand())
        {
            ExpandAll();
        }
        if (command == getCollapseAllCommand())
        {
            CollapseAll();
        }
    }

}
