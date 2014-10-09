package org.ovirt.engine.ui.uicommonweb.models.clusters;

import java.util.ArrayList;
import java.util.Map;

import org.ovirt.engine.core.common.action.AddVdsActionParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.gluster.RemoveGlusterServerParameters;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.gluster.DetachGlusterHostsModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.HostDetailModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.MultipleHostsModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;

public class ClusterGeneralModel extends EntityModel {

    public static Integer lowLimitPowerSaving = null;
    public static Integer highLimitPowerSaving = null;
    public static Integer highLimitEvenlyDistributed = null;

    private Integer noOfVolumesTotal;
    private Integer noOfVolumesUp;
    private Integer noOfVolumesDown;

    public String getNoOfVolumesTotal() {
        return Integer.toString(noOfVolumesTotal);
    }

    public void setNoOfVolumesTotal(Integer noOfVolumesTotal) {
        this.noOfVolumesTotal = noOfVolumesTotal;
    }

    public String getNoOfVolumesUp() {
        return Integer.toString(noOfVolumesUp);
    }

    public void setNoOfVolumesUp(Integer noOfVolumesUp) {
        this.noOfVolumesUp = noOfVolumesUp;
    }

    public String getNoOfVolumesDown() {
        return Integer.toString(noOfVolumesDown);
    }

    public void setNoOfVolumesDown(Integer noOfVolumesDown) {
        this.noOfVolumesDown = noOfVolumesDown;
    }

    private UICommand privateEditPolicyCommand;

    public UICommand getEditPolicyCommand()
    {
        return privateEditPolicyCommand;
    }

    public void setEditPolicyCommand(UICommand value)
    {
        privateEditPolicyCommand = value;
    }

    private boolean hasAnyAlert;

    public boolean getHasAnyAlert()
    {
        return hasAnyAlert;
    }

    public void setHasAnyAlert(boolean value)
    {
        if (hasAnyAlert != value)
        {
            hasAnyAlert = value;
            OnPropertyChanged(new PropertyChangedEventArgs("HasAnyAlert")); //$NON-NLS-1$
        }
    }

    private boolean hasGlusterHostsAlert;

    public boolean getHasNewGlusterHostsAlert()
    {
        return hasGlusterHostsAlert;
    }

    public void setHasNewGlusterHostsAlert(boolean value)
    {
        if (hasGlusterHostsAlert != value)
        {
            hasGlusterHostsAlert = value;
            OnPropertyChanged(new PropertyChangedEventArgs("HasNewGlusterHostsAlert")); //$NON-NLS-1$
        }
    }

    private UICommand importNewGlusterHostsCommand;

    public UICommand getImportNewGlusterHostsCommand()
    {
        return importNewGlusterHostsCommand;
    }

    private void setImportNewGlusterHostsCommand(UICommand value)
    {
        importNewGlusterHostsCommand = value;
    }

    private UICommand detachNewGlusterHostsCommand;

    public UICommand getDetachNewGlusterHostsCommand()
    {
        return detachNewGlusterHostsCommand;
    }

    private void setDetachNewGlusterHostsCommand(UICommand value)
    {
        detachNewGlusterHostsCommand = value;
    }

    @Override
    public VDSGroup getEntity()
    {
        return (VDSGroup) ((super.getEntity() instanceof VDSGroup) ? super.getEntity() : null);
    }

    public void setEntity(VDSGroup value)
    {
        super.setEntity(value);
    }

    public ClusterGeneralModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().generalTitle());
        setHashName("general"); //$NON-NLS-1$

        setNoOfVolumesTotal(0);
        setNoOfVolumesUp(0);
        setNoOfVolumesDown(0);

        setImportNewGlusterHostsCommand(new UICommand("ImportGlusterHosts", this)); //$NON-NLS-1$
        setDetachNewGlusterHostsCommand(new UICommand("DetachGlusterHosts", this)); //$NON-NLS-1$

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object result)
            {
                ClusterGeneralModel.highLimitEvenlyDistributed = (Integer) result;
            }
        };
        if (ClusterGeneralModel.highLimitEvenlyDistributed == null)
        {
            AsyncDataProvider.GetHighUtilizationForEvenDistribution(_asyncQuery);
        }
        _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object result)
            {
                ClusterGeneralModel.lowLimitPowerSaving = (Integer) result;
            }
        };
        if (ClusterGeneralModel.lowLimitPowerSaving == null)
        {
            AsyncDataProvider.GetLowUtilizationForPowerSave(_asyncQuery);
        }

        _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object result)
            {
                ClusterGeneralModel.highLimitPowerSaving = (Integer) result;
            }
        };
        if (ClusterGeneralModel.highLimitPowerSaving == null)
        {
            AsyncDataProvider.GetHighUtilizationForPowerSave(_asyncQuery);
        }
    }

    @Override
    protected void OnEntityChanged()
    {
        super.OnEntityChanged();

        if (getEntity() != null)
        {
            UpdateVolumeDetails();
            updateAlerts();
        }

        UpdateActionAvailability();
    }

    public void fetchAndImportNewGlusterHosts() {
        if (getWindow() != null)
        {
            return;
        }

        final MultipleHostsModel hostsModel = new MultipleHostsModel();
        setWindow(hostsModel);
        hostsModel.setTitle(ConstantsManager.getInstance().getConstants().addMultipleHostsTitle());
        hostsModel.setHashName("add_hosts"); //$NON-NLS-1$

        UICommand command = new UICommand("OnSaveHosts", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().ok());
        hostsModel.getCommands().add(command);
        hostsModel.getHosts().setItems(new ArrayList<EntityModel>());

        command = new UICommand("Cancel", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        command.setIsCancel(true);
        hostsModel.getCommands().add(command);

        hostsModel.StartProgress(null);

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object result)
            {
                Map<String, String> hostMap = (Map<String, String>) result;

                if (hostMap == null || hostMap.isEmpty())
                {
                    hostsModel.setMessage(ConstantsManager.getInstance().getConstants().emptyNewGlusterHosts());
                }
                else
                {
                    ArrayList<EntityModel> list = new ArrayList<EntityModel>();
                    for (Map.Entry<String, String> host : hostMap.entrySet())
                    {
                        HostDetailModel hostModel = new HostDetailModel(host.getKey(), host.getValue());
                        hostModel.setName(host.getKey());
                        hostModel.setPassword("");//$NON-NLS-1$
                        EntityModel entityModel = new EntityModel(hostModel);
                        list.add(entityModel);
                    }
                    hostsModel.getHosts().setItems(list);
                }
                hostsModel.StopProgress();
            }
        };
        AsyncDataProvider.GetGlusterHostsNewlyAdded(_asyncQuery, getEntity().getId(), true);

    }

    public void onSaveHosts() {
        final MultipleHostsModel hostsModel = (MultipleHostsModel) getWindow();
        if (hostsModel == null)
        {
            return;
        }
        if (!hostsModel.validate())
        {
            return;
        }

        hostsModel.StartProgress(null);
        ArrayList<VdcActionParametersBase> parametersList = new ArrayList<VdcActionParametersBase>();
        for (Object object : hostsModel.getHosts().getItems()) {
            HostDetailModel hostDetailModel = (HostDetailModel) ((EntityModel) object).getEntity();

            VDS host = new VDS();
            host.setvds_name(hostDetailModel.getName());
            host.sethost_name(hostDetailModel.getAddress());
            host.setSSHKeyFingerprint(hostDetailModel.getFingerprint());
            host.setport(54321);

            host.setvds_group_id(getEntity().getId());
            host.setpm_enabled(false);

            AddVdsActionParameters parameters = new AddVdsActionParameters();
            parameters.setVdsId(host.getId());
            parameters.setvds(host);
            parameters.setRootPassword(hostDetailModel.getPassword());
            parameters.setOverrideFirewall(false);

            parametersList.add(parameters);
        }

        Frontend.RunMultipleAction(VdcActionType.AddVds,
                parametersList,
                true,
                new IFrontendMultipleActionAsyncCallback() {

                    @Override
                    public void Executed(FrontendMultipleActionAsyncResult result) {
                        hostsModel.StopProgress();
                        boolean isAllCanDoPassed = true;
                        for (VdcReturnValueBase returnValueBase : result.getReturnValue())
                        {
                            isAllCanDoPassed = isAllCanDoPassed && returnValueBase.getCanDoAction();
                            if (!isAllCanDoPassed)
                            {
                                break;
                            }
                        }
                        if (isAllCanDoPassed)
                        {
                            updateAlerts();
                            Cancel();
                        }
                    }
                }, null);
    }

    public void detachNewGlusterHosts()
    {
        if (getWindow() != null)
        {
            return;
        }

        final DetachGlusterHostsModel hostsModel = new DetachGlusterHostsModel();
        setWindow(hostsModel);
        hostsModel.setTitle(ConstantsManager.getInstance().getConstants().detachGlusterHostsTitle());
        hostsModel.setHashName("detach_gluster_hosts"); //$NON-NLS-1$

        UICommand command = new UICommand("OnDetachGlusterHosts", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().ok());
        hostsModel.getCommands().add(command);
        hostsModel.getHosts().setItems(new ArrayList<EntityModel>());

        command = new UICommand("Cancel", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        command.setIsCancel(true);
        hostsModel.getCommands().add(command);

        hostsModel.StartProgress(null);

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object result)
            {
                Map<String, String> hostMap = (Map<String, String>) result;

                if (hostMap == null || hostMap.isEmpty())
                {
                    hostsModel.setMessage(ConstantsManager.getInstance().getConstants().emptyNewGlusterHosts());
                }
                else
                {
                    ArrayList<EntityModel> hostList = new ArrayList<EntityModel>();
                    for (String host : hostMap.keySet())
                    {
                        hostList.add(new EntityModel(host));
                    }
                    hostsModel.getHosts().setItems(hostList);
                }
                hostsModel.StopProgress();
            }
        };
        AsyncDataProvider.GetGlusterHostsNewlyAdded(_asyncQuery, getEntity().getId(), true);
    }

    public void onDetachNewGlusterHosts()
    {
        if (getWindow() == null)
        {
            return;
        }

        final DetachGlusterHostsModel hostsModel = (DetachGlusterHostsModel) getWindow();
        if (!hostsModel.validate())
        {
            return;
        }
        boolean force = (Boolean) hostsModel.getForce().getEntity();
        ArrayList<VdcActionParametersBase> parametersList = new ArrayList<VdcActionParametersBase>();
        for (Object model : hostsModel.getHosts().getSelectedItems()) {
            String host = (String) ((EntityModel) model).getEntity();
            parametersList.add(new RemoveGlusterServerParameters(getEntity().getId(), host, force));
        }
        Frontend.RunMultipleAction(VdcActionType.RemoveGlusterServer, parametersList);
        Cancel();
    }

    public void Cancel()
    {
        setWindow(null);
    }

    private void UpdateActionAvailability()
    {
        getEditPolicyCommand().setIsExecutionAllowed(getEntity() != null);
    }

    private void UpdateVolumeDetails()
    {
        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object result)
            {
                ClusterGeneralModel innerGeneralModel = (ClusterGeneralModel) model;
                ArrayList<GlusterVolumeEntity> volumeList = (ArrayList<GlusterVolumeEntity>) result;
                int volumesUp = 0;
                int volumesDown = 0;
                for (GlusterVolumeEntity volumeEntity : volumeList)
                {
                    if (volumeEntity.getStatus() == GlusterStatus.UP)
                    {
                        volumesUp++;
                    }
                    else
                    {
                        volumesDown++;
                    }
                }
                setNoOfVolumesTotal(volumeList.size());
                setNoOfVolumesUp(volumesUp);
                setNoOfVolumesDown(volumesDown);
            }
        };
        AsyncDataProvider.GetVolumeList(_asyncQuery, getEntity().getname());
    }

    private void updateAlerts()
    {
        if (getEntity().supportsGlusterService())
        {
            AsyncQuery _asyncQuery = new AsyncQuery();
            _asyncQuery.setModel(this);
            _asyncQuery.asyncCallback = new INewAsyncCallback() {
                @Override
                public void OnSuccess(Object model, Object result)
                {
                    ClusterGeneralModel innerGeneralModel = (ClusterGeneralModel) model;
                    Map<String, String> serverMap = (Map<String, String>) result;
                    if (!serverMap.isEmpty())
                    {
                        innerGeneralModel.setHasNewGlusterHostsAlert(true);
                        innerGeneralModel.setHasAnyAlert(true);
                    }
                    else
                    {
                        setHasNewGlusterHostsAlert(false);
                        setHasAnyAlert(false);
                    }
                }
            };
            AsyncDataProvider.GetGlusterHostsNewlyAdded(_asyncQuery, getEntity().getId(), false);
        }
        else
        {
            setHasNewGlusterHostsAlert(false);
            setHasAnyAlert(false);
        }
    }

    @Override
    public void ExecuteCommand(UICommand command)
    {
        super.ExecuteCommand(command);

        if (command == getImportNewGlusterHostsCommand())
        {
            fetchAndImportNewGlusterHosts();
        }
        else if (command == getDetachNewGlusterHostsCommand())
        {
            detachNewGlusterHosts();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnSaveHosts")) //$NON-NLS-1$
        {
            onSaveHosts();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnDetachGlusterHosts")) //$NON-NLS-1$
        {
            onDetachNewGlusterHosts();
        }
        else if (StringHelper.stringsEqual(command.getName(), "Cancel")) //$NON-NLS-1$
        {
            Cancel();
        }
    }
}
