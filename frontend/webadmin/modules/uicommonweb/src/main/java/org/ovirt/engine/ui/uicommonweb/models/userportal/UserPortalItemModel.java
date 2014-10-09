package org.ovirt.engine.ui.uicommonweb.models.userportal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmOsType;
import org.ovirt.engine.core.common.businessentities.VmPoolType;
import org.ovirt.engine.core.common.businessentities.vm_pools;
import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.configure.ChangeCDModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ConsoleModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public class UserPortalItemModel extends EntityModel
{


    public ConsoleProtocol getSelectedProtocol() {
        return userSelectedDisplayProtocolManager.resolveSelectedProtocol(this);
    }

    public void setSelectedProtocol(ConsoleProtocol selectedProtocol) {
        userSelectedDisplayProtocolManager.setSelectedProtocol(selectedProtocol, this);
    }

    private UICommand runCommand;

    public UICommand getRunCommand()
    {
        return runCommand;
    }

    private void setRunCommand(UICommand value)
    {
        getCommands().remove(runCommand);
        getCommands().add(value);
        runCommand = value;
    }

    private UICommand pauseCommand;

    public UICommand getPauseCommand()
    {
        return pauseCommand;
    }

    private void setPauseCommand(UICommand value)
    {
        getCommands().remove(pauseCommand);
        getCommands().add(value);
        pauseCommand = value;
    }

    private UICommand stopCommand;

    public UICommand getStopCommand()
    {
        return stopCommand;
    }

    private void setStopCommand(UICommand value)
    {
        getCommands().remove(stopCommand);
        getCommands().add(value);
        stopCommand = value;
    }

    private UICommand shutdownCommand;

    public UICommand getShutdownCommand()
    {
        return shutdownCommand;
    }

    private void setShutdownCommand(UICommand value)
    {
        getCommands().remove(shutdownCommand);
        getCommands().add(value);
        shutdownCommand = value;
    }

    private UICommand takeVmCommand;

    public UICommand getTakeVmCommand()
    {
        return takeVmCommand;
    }

    private void setTakeVmCommand(UICommand value)
    {
        getCommands().remove(takeVmCommand);
        getCommands().add(value);
        takeVmCommand = value;
    }

    private UICommand returnVmCommand;

    public UICommand getReturnVmCommand()
    {
        return returnVmCommand;
    }

    private void setReturnVmCommand(UICommand value)
    {
        getCommands().remove(returnVmCommand);
        getCommands().add(value);
        returnVmCommand = value;
    }

    private IVmPoolResolutionService privateResolutionService;

    public IVmPoolResolutionService getResolutionService()
    {
        return privateResolutionService;
    }

    private void setResolutionService(IVmPoolResolutionService value)
    {
        privateResolutionService = value;
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

    private boolean isPool;

    public boolean getIsPool()
    {
        return isPool;
    }

    public void setIsPool(boolean value)
    {
        if (isPool != value)
        {
            isPool = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsPool")); //$NON-NLS-1$
        }
    }

    private boolean isServer;

    public boolean getIsServer()
    {
        return isServer;
    }

    public void setIsServer(boolean value)
    {
        if (isServer != value)
        {
            isServer = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsServer")); //$NON-NLS-1$
        }
    }

    private boolean isFromPool;

    public boolean getIsFromPool()
    {
        return isFromPool;
    }

    public void setIsFromPool(boolean value)
    {
        if (isFromPool != value)
        {
            isFromPool = value;
            OnPropertyChanged(new PropertyChangedEventArgs("IsFromPool")); //$NON-NLS-1$
        }
    }

    private VmPoolType poolType = VmPoolType.values()[0];

    public VmPoolType getPoolType()
    {
        return poolType;
    }

    public void setPoolType(VmPoolType value)
    {
        if (poolType != value)
        {
            poolType = value;
            OnPropertyChanged(new PropertyChangedEventArgs("PoolType")); //$NON-NLS-1$
        }
    }

    private VMStatus status = VMStatus.values()[0];

    public VMStatus getStatus()
    {
        return status;
    }

    public void setStatus(VMStatus value)
    {
        if (status != value)
        {
            status = value;
            OnPropertyChanged(new PropertyChangedEventArgs("Status")); //$NON-NLS-1$
        }
    }

    private ConsoleModel defaultConsole;

    public ConsoleModel getDefaultConsole()
    {
        return defaultConsole;
    }

    public void setDefaultConsole(ConsoleModel value)
    {
        if (defaultConsole != value)
        {
            defaultConsole = value;
            OnPropertyChanged(new PropertyChangedEventArgs("DefaultConsole")); //$NON-NLS-1$
        }
    }

    private ConsoleModel additionalConsole;

    public ConsoleModel getAdditionalConsole()
    {
        return additionalConsole;
    }

    public void setAdditionalConsole(ConsoleModel value)
    {
        if (additionalConsole != value)
        {
            additionalConsole = value;
            OnPropertyChanged(new PropertyChangedEventArgs("AdditionalConsole")); //$NON-NLS-1$
        }
    }

    private boolean hasAdditionalConsole;

    public boolean getHasAdditionalConsole()
    {
        return hasAdditionalConsole;
    }

    public void setHasAdditionalConsole(boolean value)
    {
        if (hasAdditionalConsole != value)
        {
            hasAdditionalConsole = value;
            OnPropertyChanged(new PropertyChangedEventArgs("HasAdditionalConsole")); //$NON-NLS-1$
        }
    }

    private List<ChangeCDModel> cdImages;

    public List<ChangeCDModel> getCdImages()
    {
        return cdImages;
    }

    public void setCdImages(List<ChangeCDModel> value)
    {
        if (cdImages != value)
        {
            cdImages = value;
            OnPropertyChanged(new PropertyChangedEventArgs("CdImages")); //$NON-NLS-1$
        }
    }

    private VmOsType osType = VmOsType.values()[0];

    public VmOsType getOsType()
    {
        return osType;
    }

    public void setOsType(VmOsType value)
    {
        if (osType != value)
        {
            osType = value;
            OnPropertyChanged(new PropertyChangedEventArgs("OsType")); //$NON-NLS-1$
        }
    }

    private Version spiceDriverVersion;

    public Version getSpiceDriverVersion() {
        return spiceDriverVersion;
    }

    public void setSpiceDriverVersion(Version spiceDriverVersion) {
        if (this.spiceDriverVersion != spiceDriverVersion) {
            this.spiceDriverVersion = spiceDriverVersion;
            OnPropertyChanged(new PropertyChangedEventArgs("spiceDriverVersion")); //$NON-NLS-1$
        }
    }

    private ItemBehavior behavior;
    private final UserSelectedDisplayProtocolManager userSelectedDisplayProtocolManager;

    public UserPortalItemModel(IVmPoolResolutionService resolutionService, UserSelectedDisplayProtocolManager userSelectedDisplayManager)
    {
        this.userSelectedDisplayProtocolManager = userSelectedDisplayManager;
        setResolutionService(resolutionService);

        setRunCommand(new UICommand("Run", this)); //$NON-NLS-1$
        setPauseCommand(new UICommand("Pause", this)); //$NON-NLS-1$
        setStopCommand(new UICommand("Stop", this)); //$NON-NLS-1$
        setShutdownCommand(new UICommand("Shutdown", this)); //$NON-NLS-1$
        setTakeVmCommand(new UICommand("TakeVm", this)); //$NON-NLS-1$
        setReturnVmCommand(new UICommand("ReturnVm", this)); //$NON-NLS-1$

        ChangeCDModel tempVar = new ChangeCDModel();
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().retrievingCDsTitle());
        setCdImages(new ArrayList<ChangeCDModel>(Arrays.asList(new ChangeCDModel[] { tempVar })));
    }

    @Override
    protected void OnEntityChanged()
    {
        // Change behavior to match entity type.
        if (getEntity() instanceof VM)
        {
            behavior = new VmItemBehavior(this);
        }
        else if (getEntity() instanceof vm_pools)
        {
            behavior = new PoolItemBehavior(this);
        }
        else
        {
            throw new UnsupportedOperationException();
        }

        behavior.OnEntityChanged();
    }

    public boolean IsVmUp()
    {
        switch (getStatus())
        {
        case WaitForLaunch:
        case PoweringUp:
        case RebootInProgress:
        case RestoringState:
        case MigratingFrom:
        case MigratingTo:
        case Up:
            return true;

        default:
            return false;
        }
    }

    // to simpler integration with the editor framework
    public boolean getIsVmUp() {
        return IsVmUp();
    }

    @Override
    protected void EntityPropertyChanged(Object sender, PropertyChangedEventArgs e)
    {
        super.EntityPropertyChanged(sender, e);
        behavior.EntityPropertyChanged(e);
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args)
    {
        super.eventRaised(ev, sender, args);
        behavior.eventRaised(ev, sender, args);
    }

    @Override
    public void ExecuteCommand(UICommand command)
    {
        super.ExecuteCommand(command);
        behavior.ExecuteCommand(command);
    }
}
