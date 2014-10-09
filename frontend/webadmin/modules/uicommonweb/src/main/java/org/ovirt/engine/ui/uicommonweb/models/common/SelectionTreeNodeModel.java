package org.ovirt.engine.ui.uicommonweb.models.common;

import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class SelectionTreeNodeModel extends EntityModel
{

    private String description;

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String value)
    {
        if (!StringHelper.stringsEqual(description, value))
        {
            description = value;
            OnPropertyChanged(new PropertyChangedEventArgs("Description")); //$NON-NLS-1$
        }
    }

    private Boolean isSelectedNullable;

    public Boolean getIsSelectedNullable()
    {
        return isSelectedNullable;
    }

    public void setIsSelectedNullable(Boolean value)
    {
        if (isSelectedNullable == null && value == null)
        {
            return;
        }
        if (isSelectedNullable == null || !isSelectedNullable.equals(value))
        {
            isSelectedNullable = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsSelectedNullable")); //$NON-NLS-1$
            if (!getIsSelectedNotificationPrevent())
            {
                IsSelectedChanged();
            }
        }
    }

    private boolean isExpanded;

    public boolean getIsExpanded()
    {
        return isExpanded;
    }

    public void setIsExpanded(boolean value)
    {
        isExpanded = value;
        OnPropertyChanged(new PropertyChangedEventArgs("IsExpanded")); //$NON-NLS-1$
    }

    private boolean isSelectedNotificationPrevent;

    public boolean getIsSelectedNotificationPrevent()
    {
        return isSelectedNotificationPrevent;
    }

    public void setIsSelectedNotificationPrevent(boolean value)
    {
        if (isSelectedNotificationPrevent != value)
        {
            isSelectedNotificationPrevent = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsSelectedNotificationPrevent")); //$NON-NLS-1$
        }
    }

    private SelectionTreeNodeModel parent;

    public SelectionTreeNodeModel getParent()
    {
        return parent;
    }

    public void setParent(SelectionTreeNodeModel value)
    {
        if (parent != value)
        {
            parent = value;
            OnPropertyChanged(new PropertyChangedEventArgs("Parent")); //$NON-NLS-1$
        }
    }

    private ArrayList<SelectionTreeNodeModel> children;

    public ArrayList<SelectionTreeNodeModel> getChildren()
    {
        return children;
    }

    public void setChildren(ArrayList<SelectionTreeNodeModel> value)
    {
        if ((children == null && value != null) || (children != null && !children.equals(value)))
        {
            children = value;
            OnPropertyChanged(new PropertyChangedEventArgs("Children")); //$NON-NLS-1$
        }
    }

    private String tooltip;

    public String getTooltip()
    {
        return tooltip;
    }

    public void setTooltip(String value)
    {
        if (!StringHelper.stringsEqual(tooltip, value))
        {
            tooltip = value;
            OnPropertyChanged(new PropertyChangedEventArgs("Tooltip")); //$NON-NLS-1$
        }
    }

    public SelectionTreeNodeModel()
    {
        setChildren(new ArrayList<SelectionTreeNodeModel>());
    }

    public void IsSelectedChanged()
    {
        if (getParent() != null)
        {
            UpdateParentSelection();
        }
        // Children.Each(a => a.IsSelectedNotificationPrevent = true);
        // Children.Each(a => a.IsSelected = IsSelected);
        // Children.Each(a => a.IsSelectedNotificationPrevent = false);
        for (SelectionTreeNodeModel child : getChildren())
        {
            child.setIsSelectedNotificationPrevent(true);
            child.setIsSelectedNullable(getIsSelectedNullable());
            child.setIsSelectedNotificationPrevent(false);
            for (SelectionTreeNodeModel grandChild : child.getChildren())
            {
                grandChild.setIsSelectedNotificationPrevent(true);
                grandChild.setIsSelectedNullable(getIsSelectedNullable());
                grandChild.setIsSelectedNotificationPrevent(false);
            }
        }
    }

    public void UpdateParentSelection()
    {
        // int selCount = Parent.Children.Count(a => a.IsSelected == true);
        if (getParent() == null || getParent().getChildren() == null)
        {
            return;
        }
        int selCount = 0, nullCount = 0;
        for (SelectionTreeNodeModel a : getParent().getChildren())
        {
            if (a.getIsSelectedNullable() != null && a.getIsSelectedNullable().equals(true))
            {
                selCount += 1;
            }
            else if (a.isSelectedNullable == null)
            {
                nullCount++;
            }

        }

        getParent().setIsSelectedNotificationPrevent(true);
        if (selCount == 0 && nullCount == 0)
        {
            getParent().setIsSelectedNullable(false);
        }
        else
        {
            // if (Parent.Children.Count() == selCount)
            if (getParent().getChildren().size() == selCount)
            {
                getParent().setIsSelectedNullable(true);
            }
            else
            {
                getParent().setIsSelectedNullable(null);
            }
        }
        getParent().setIsSelectedNotificationPrevent(false);

        getParent().UpdateParentSelection();
    }
}
