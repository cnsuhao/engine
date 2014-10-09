package org.ovirt.engine.ui.uicommonweb.models.storage;

import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.EventDefinition;
import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;

import java.util.List;

@SuppressWarnings("unused")
public class SanTargetModel extends EntityModel
{

    public static EventDefinition LoggedInEventDefinition;
    private Event privateLoggedInEvent;

    public Event getLoggedInEvent()
    {
        return privateLoggedInEvent;
    }

    private void setLoggedInEvent(Event value)
    {
        privateLoggedInEvent = value;
    }

    private UICommand privateLoginCommand;

    public UICommand getLoginCommand()
    {
        return privateLoginCommand;
    }

    public void setLoginCommand(UICommand value)
    {
        privateLoginCommand = value;
    }

    private String address;

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String value)
    {
        if (!StringHelper.stringsEqual(address, value))
        {
            address = value;
            OnPropertyChanged(new PropertyChangedEventArgs("Address")); //$NON-NLS-1$
        }
    }

    private String port;

    public String getPort()
    {
        return port;
    }

    public void setPort(String value)
    {
        if (!StringHelper.stringsEqual(port, value))
        {
            port = value;
            OnPropertyChanged(new PropertyChangedEventArgs("Port")); //$NON-NLS-1$
        }
    }

    private String name;

    public String getName()
    {
        return name;
    }

    public void setName(String value)
    {
        if (!StringHelper.stringsEqual(name, value))
        {
            name = value;
            OnPropertyChanged(new PropertyChangedEventArgs("Name")); //$NON-NLS-1$
        }
    }

    private boolean isLoggedIn;

    public boolean getIsLoggedIn()
    {
        return isLoggedIn;
    }

    public void setIsLoggedIn(boolean value)
    {
        if (isLoggedIn != value)
        {
            isLoggedIn = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsLoggedIn")); //$NON-NLS-1$
        }
    }

    private List<LunModel> luns;

    public List<LunModel> getLuns()
    {
        return luns;
    }

    public void setLuns(List<LunModel> value)
    {
        if (luns != value)
        {
            luns = value;
            OnPropertyChanged(new PropertyChangedEventArgs("Luns")); //$NON-NLS-1$
            getLunsList().setItems(luns);
        }
    }

    private ListModel lunsList;

    public ListModel getLunsList()
    {
        return lunsList;
    }

    public void setLunsList(ListModel value)
    {
        if (lunsList != value)
        {
            lunsList = value;
            OnPropertyChanged(new PropertyChangedEventArgs("LunsList")); //$NON-NLS-1$
        }
    }

    static
    {
        LoggedInEventDefinition = new EventDefinition("LoggedIn", SanTargetModel.class); //$NON-NLS-1$
    }

    public SanTargetModel()
    {
        setLoggedInEvent(new Event(LoggedInEventDefinition));
        setLoginCommand(new UICommand("Login", this)); //$NON-NLS-1$
        setLunsList(new ListModel());
    }

    private void Login()
    {
        getLoggedInEvent().raise(this, EventArgs.Empty);
    }

    @Override
    public void ExecuteCommand(UICommand command)
    {
        super.ExecuteCommand(command);

        if (command == getLoginCommand())
        {
            Login();
        }
    }
}
