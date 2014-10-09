package org.ovirt.engine.ui.uicommonweb.models;

import org.ovirt.engine.core.compat.ObservableCollection;
import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicommonweb.UICommand;

import java.util.List;

@SuppressWarnings("unused")
public class GuideModel extends EntityModel
{

    private List<UICommand> compulsoryActions;

    public List<UICommand> getCompulsoryActions()
    {
        return compulsoryActions;
    }

    public void setCompulsoryActions(List<UICommand> value)
    {
        if (compulsoryActions != value)
        {
            compulsoryActions = value;
            OnPropertyChanged(new PropertyChangedEventArgs("CompulsoryActions")); //$NON-NLS-1$
        }
    }

    private List<UICommand> optionalActions;

    public List<UICommand> getOptionalActions()
    {
        return optionalActions;
    }

    public void setOptionalActions(List<UICommand> value)
    {
        if (optionalActions != value)
        {
            optionalActions = value;
            OnPropertyChanged(new PropertyChangedEventArgs("OptionalActions")); //$NON-NLS-1$
        }
    }

    public GuideModel()
    {
        setCompulsoryActions(new ObservableCollection<UICommand>());
        setOptionalActions(new ObservableCollection<UICommand>());
    }
}
