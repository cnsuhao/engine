package org.ovirt.engine.ui.uicommonweb.models.datacenters;

import org.ovirt.engine.core.common.action.RecoveryStoragePoolParameters;
import org.ovirt.engine.core.common.action.StoragePoolManagementParameter;
import org.ovirt.engine.core.common.action.StoragePoolParametersBase;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.StorageDomainSharedStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.common.queries.GetQuotaByStoragePoolIdQueryParameters;
import org.ovirt.engine.core.common.queries.SearchParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.NotifyCollectionChangedEventArgs;
import org.ovirt.engine.core.compat.ObservableCollection;
import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.searchbackend.SearchObjects;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Cloner;
import org.ovirt.engine.ui.uicommonweb.ICommandTarget;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ISupportSystemTreeContext;
import org.ovirt.engine.ui.uicommonweb.models.ListWithDetailsModel;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemModel;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemType;
import org.ovirt.engine.ui.uicommonweb.models.configure.PermissionListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class DataCenterListModel extends ListWithDetailsModel implements ISupportSystemTreeContext
{

    private UICommand privateNewCommand;

    public UICommand getNewCommand()
    {
        return privateNewCommand;
    }

    private void setNewCommand(UICommand value)
    {
        privateNewCommand = value;
    }

    private UICommand privateEditCommand;

    public UICommand getEditCommand()
    {
        return privateEditCommand;
    }

    private void setEditCommand(UICommand value)
    {
        privateEditCommand = value;
    }

    private UICommand privateRemoveCommand;

    public UICommand getRemoveCommand()
    {
        return privateRemoveCommand;
    }

    private void setRemoveCommand(UICommand value)
    {
        privateRemoveCommand = value;
    }

    private UICommand privateForceRemoveCommand;

    public UICommand getForceRemoveCommand()
    {
        return privateForceRemoveCommand;
    }

    private void setForceRemoveCommand(UICommand value)
    {
        privateForceRemoveCommand = value;
    }

    private UICommand privateActivateCommand;

    public UICommand getActivateCommand()
    {
        return privateActivateCommand;
    }

    private void setActivateCommand(UICommand value)
    {
        privateActivateCommand = value;
    }

    private UICommand privateGuideCommand;

    public UICommand getGuideCommand()
    {
        return privateGuideCommand;
    }

    private void setGuideCommand(UICommand value)
    {
        privateGuideCommand = value;
    }

    private UICommand privateRecoveryStorageCommand;

    public UICommand getRecoveryStorageCommand()
    {
        return privateRecoveryStorageCommand;
    }

    private void setRecoveryStorageCommand(UICommand value)
    {
        privateRecoveryStorageCommand = value;
    }

    // get { return SelectedItems == null ? new object[0] : SelectedItems.Cast<storage_pool>().Select(a =>
    // a.id).Cast<object>().ToArray(); }
    protected Object[] getSelectedKeys()
    {
        if (getSelectedItems() == null)
        {
            return new Object[0];
        }
        else
        {
            ArrayList<Object> objL = new ArrayList<Object>();
            for (storage_pool a : Linq.<storage_pool> Cast(getSelectedItems()))
            {
                objL.add(a.getId());
            }
            return objL.toArray(new Object[] {});
        }
    }

    private Object privateGuideContext;

    public Object getGuideContext()
    {
        return privateGuideContext;
    }

    public void setGuideContext(Object value)
    {
        privateGuideContext = value;
    }

    DataCenterQuotaListModel quotaListModel;

    public DataCenterListModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().dataCentersTitle());

        setDefaultSearchString("DataCenter:"); //$NON-NLS-1$
        setSearchString(getDefaultSearchString());
        setSearchObjects(new String[] { SearchObjects.VDC_STORAGE_POOL_OBJ_NAME });
        setAvailableInModes(ApplicationMode.VirtOnly);

        setNewCommand(new UICommand("New", this)); //$NON-NLS-1$
        setEditCommand(new UICommand("Edit", this)); //$NON-NLS-1$
        setRemoveCommand(new UICommand("Remove", this)); //$NON-NLS-1$
        UICommand tempVar = new UICommand("ForceRemove", this); //$NON-NLS-1$
        tempVar.setIsExecutionAllowed(true);
        setForceRemoveCommand(tempVar);
        setRecoveryStorageCommand(new UICommand("RecoveryStorage", this)); //$NON-NLS-1$
        setActivateCommand(new UICommand("Activate", this)); //$NON-NLS-1$
        setGuideCommand(new UICommand("Guide", this)); //$NON-NLS-1$

        UpdateActionAvailability();

        getSearchNextPageCommand().setIsAvailable(true);
        getSearchPreviousPageCommand().setIsAvailable(true);
    }

    public void Guide()
    {
        DataCenterGuideModel model = new DataCenterGuideModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().newDataCenterGuideMeTitle());
        model.setHashName("new_data_center_-_guide_me"); //$NON-NLS-1$
        if (getGuideContext() == null) {
            storage_pool dataCenter = (storage_pool) getSelectedItem();
            setGuideContext(dataCenter.getId());
        }

        AsyncDataProvider.GetDataCenterById(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void OnSuccess(Object target, Object returnValue) {
                        DataCenterListModel dataCenterListModel = (DataCenterListModel) target;
                        DataCenterGuideModel model = (DataCenterGuideModel) dataCenterListModel.getWindow();
                        model.setEntity((storage_pool) returnValue);

                        UICommand tempVar = new UICommand("Cancel", dataCenterListModel); //$NON-NLS-1$
                        tempVar.setTitle(ConstantsManager.getInstance().getConstants().configureLaterTitle());
                        tempVar.setIsDefault(true);
                        tempVar.setIsCancel(true);
                        model.getCommands().add(tempVar);
                    }
                }), (Guid) getGuideContext());
    }

    @Override
    protected void InitDetailModels()
    {
        super.InitDetailModels();

        ObservableCollection<EntityModel> list = new ObservableCollection<EntityModel>();
        list.add(new DataCenterStorageListModel());
        list.add(new DataCenterNetworkListModel());
        list.add(new DataCenterClusterListModel());
        quotaListModel = new DataCenterQuotaListModel();
        quotaListModel.setIsAvailable(false);
        list.add(quotaListModel);
        list.add(new PermissionListModel());
        list.add(new DataCenterEventListModel());
        setDetailModels(list);
    }

    @Override
    public boolean IsSearchStringMatch(String searchString)
    {
        return searchString.trim().toLowerCase().startsWith("datacenter"); //$NON-NLS-1$
    }

    @Override
    protected void SyncSearch()
    {
        SearchParameters tempVar = new SearchParameters(getSearchString(), SearchType.StoragePool);
        tempVar.setMaxCount(getSearchPageSize());
        super.SyncSearch(VdcQueryType.Search, tempVar);

    }

    @Override
    protected void AsyncSearch()
    {
        super.AsyncSearch();

        setAsyncResult(Frontend.RegisterSearch(getSearchString(), SearchType.StoragePool, getSearchPageSize()));
        setItems(getAsyncResult().getData());
    }

    public void New()
    {
        if (getWindow() != null)
        {
            return;
        }

        DataCenterModel model = new DataCenterModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().newDataCenterTitle());
        model.setHashName("new_data_center"); //$NON-NLS-1$
        model.setIsNew(true);
        model.getStorageTypeList().setSelectedItem(StorageType.NFS);

        UICommand tempVar = new UICommand("OnSave", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    public void Edit()
    {
        storage_pool dataCenter = (storage_pool) getSelectedItem();

        if (getWindow() != null)
        {
            return;
        }

        final DataCenterModel model = new DataCenterModel();
        setWindow(model);
        model.setEntity(dataCenter);
        model.setDataCenterId(dataCenter.getId());
        model.setTitle(ConstantsManager.getInstance().getConstants().editDataCenterTitle());
        model.setHashName("edit_data_center"); //$NON-NLS-1$
        model.getName().setEntity(dataCenter.getname());

        if (getSystemTreeSelectedItem() != null
                && getSystemTreeSelectedItem().getType() == SystemTreeItemType.DataCenter)
        {
            model.getName().setIsChangable(false);
            model.getName().setInfo("Cannot edit Data Center's Name in tree context"); //$NON-NLS-1$
        }

        model.getDescription().setEntity(dataCenter.getdescription());
        model.setOriginalName(dataCenter.getname());

        AsyncDataProvider.GetStorageDomainList(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void OnSuccess(Object target, Object returnValue) {
                        List<storage_domains> storageDomainList = (List<storage_domains>) returnValue;

                        if (storageDomainList.size() != 0) {
                            model.getStorageTypeList().setIsChangable(false);
                            model.getStorageTypeList()
                                    .getChangeProhibitionReasons()
                                    .add("Cannot change Repository type with Storage Domains attached to it"); //$NON-NLS-1$
                        }

                    }
                }), dataCenter.getId());

        model.getStorageTypeList().setSelectedItem(dataCenter.getstorage_pool_type());

        model.getQuotaEnforceTypeListModel().setSelectedItem(dataCenter.getQuotaEnforcementType());

        UICommand tempVar = new UICommand("OnSave", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    public void remove()
    {
        if (getWindow() != null)
        {
            return;
        }

        ConfirmationModel model = new ConfirmationModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().removeDataCenterTitle());
        model.setHashName("remove_data_center"); //$NON-NLS-1$
        model.setMessage(ConstantsManager.getInstance().getConstants().dataCentersMsg());

        ArrayList<String> list = new ArrayList<String>();
        for (storage_pool a : Linq.<storage_pool> Cast(getSelectedItems()))
        {
            list.add(a.getname());
        }
        model.setItems(list);

        UICommand tempVar = new UICommand("OnRemove", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    public void ForceRemove()
    {
        ConfirmationModel model = new ConfirmationModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().forceRemoveDataCenterTitle());
        model.setHashName("force_remove_data_center"); //$NON-NLS-1$
        model.setMessage(ConstantsManager.getInstance().getConstants().dataCentersMsg());
        model.getLatch().setIsAvailable(true);
        model.getLatch().setIsChangable(true);

        ArrayList<String> list = new ArrayList<String>();
        for (storage_pool a : Linq.<storage_pool> Cast(getSelectedItems()))
        {
            list.add(a.getname());
        }
        model.setItems(list);

        UICommand tempVar = new UICommand("OnForceRemove", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    public void RecoveryStorage()
    {
        final ConfirmationModel windowModel = new ConfirmationModel();
        setWindow(windowModel);
        windowModel.setTitle(ConstantsManager.getInstance().getConstants().dataCenterReInitializeTitle());
        windowModel.setHashName("data_center_re-initialize"); //$NON-NLS-1$
        windowModel.getLatch().setIsAvailable(true);
        windowModel.getLatch().setIsChangable(true);

        windowModel.StartProgress(null);

        AsyncDataProvider.GetStorageDomainList(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object returnValue) {
                windowModel.StopProgress();
                List<storage_domains> storageDomainList = (List<storage_domains>) returnValue;
                List<EntityModel> models = new ArrayList<EntityModel>();
                for (storage_domains a : storageDomainList) {
                    if (a.getstorage_domain_type() == StorageDomainType.Data
                            && a.getstorage_type() == ((storage_pool) getSelectedItem()).getstorage_pool_type()
                            && (a.getstorage_domain_shared_status() == StorageDomainSharedStatus.Unattached)) {
                        EntityModel tempVar = new EntityModel();
                        tempVar.setEntity(a);
                        models.add(tempVar);
                    }
                }
                windowModel.setItems(models);

                if (models.size() > 0) {
                    EntityModel entityModel = models.size() != 0 ? models.get(0) : null;
                    if (entityModel != null) {
                        entityModel.setIsSelected(true);
                    }
                }

                if (models.isEmpty()) {
                    windowModel.setMessage(ConstantsManager.getInstance()
                            .getConstants()
                            .thereAreNoCompatibleStorageDomainsAttachThisDcMsg());

                    UICommand tempVar2 = new UICommand("Cancel", DataCenterListModel.this); //$NON-NLS-1$
                    tempVar2.setTitle(ConstantsManager.getInstance().getConstants().close());
                    tempVar2.setIsDefault(true);
                    tempVar2.setIsCancel(true);
                    windowModel.getCommands().add(tempVar2);
                } else {
                    UICommand tempVar3 = new UICommand("OnRecover", DataCenterListModel.this); //$NON-NLS-1$
                    tempVar3.setTitle(ConstantsManager.getInstance().getConstants().ok());
                    tempVar3.setIsDefault(true);
                    windowModel.getCommands().add(tempVar3);
                    UICommand tempVar4 = new UICommand("Cancel", DataCenterListModel.this); //$NON-NLS-1$
                    tempVar4.setTitle(ConstantsManager.getInstance().getConstants().cancel());
                    tempVar4.setIsCancel(true);
                    windowModel.getCommands().add(tempVar4);
                }

            }
        }));
    }

    public void OnRecover()
    {

        final ConfirmationModel windowModel = (ConfirmationModel) getWindow();
        if (!windowModel.Validate())
        {
            return;
        }

        AsyncDataProvider.GetStorageDomainList(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object model, Object returnValue) {
                storage_domains master = null;
                List<storage_domains> storageDomainList = (List<storage_domains>) returnValue;
                for (storage_domains a : storageDomainList)
                {
                    if (a.getstorage_domain_type() == StorageDomainType.Master)
                    {
                        master = a;
                        break;
                    }
                }
                List<storage_domains> items = new ArrayList<storage_domains>();
                for (EntityModel a : Linq.<EntityModel> Cast(windowModel.getItems()))
                {
                    if (a.getIsSelected())
                    {
                        items.add((storage_domains) a.getEntity());
                    }
                }
                if (items.size() > 0)
                {
                    if (windowModel.getProgress() != null)
                    {
                        return;
                    }
                    ArrayList<VdcActionParametersBase> parameters =
                            new ArrayList<VdcActionParametersBase>();
                    for (storage_domains a : items)
                    {
                        parameters.add(new RecoveryStoragePoolParameters(((storage_pool) getSelectedItem()).getId(),
                                a.getId()));
                    }
                    windowModel.StartProgress(null);
                    Frontend.RunMultipleAction(VdcActionType.RecoveryStoragePool, parameters,
                            new IFrontendMultipleActionAsyncCallback() {
                                @Override
                                public void Executed(FrontendMultipleActionAsyncResult result) {

                                    ConfirmationModel localModel = (ConfirmationModel) result.getState();
                                    localModel.StopProgress();
                                    Cancel();

                                }
                            }, windowModel);
                }
                else
                {
                    Cancel();
                }
            }
        }),
                ((storage_pool) getSelectedItem()).getId());
    }

    public void Activate()
    {
        // Frontend.RunMultipleActions(VdcActionType.ActivateStoragePool,
        // SelectedItems.Cast<storage_pool>()
        // .Select(a => (VdcActionParametersBase)new StoragePoolParametersBase(a.id))
        // .ToList()
        // );
    }

    public void OnRemove()
    {
        ConfirmationModel model = (ConfirmationModel) getWindow();

        if (model.getProgress() != null)
        {
            return;
        }

        ArrayList<VdcActionParametersBase> parameters = new ArrayList<VdcActionParametersBase>();
        for (storage_pool a : Linq.<storage_pool> Cast(getSelectedItems()))
        {
            parameters.add(new StoragePoolParametersBase(a.getId()));
        }

        model.StartProgress(null);

        Frontend.RunMultipleAction(VdcActionType.RemoveStoragePool, parameters,
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void Executed(FrontendMultipleActionAsyncResult result) {

                        ConfirmationModel localModel = (ConfirmationModel) result.getState();
                        localModel.StopProgress();
                        Cancel();

                    }
                }, model);
    }

    public void OnForceRemove()
    {
        ConfirmationModel model = (ConfirmationModel) getWindow();
        if (!model.Validate())
        {
            return;
        }
        VdcActionParametersBase parametersBase = new VdcActionParametersBase();
        StoragePoolParametersBase tempVar = new StoragePoolParametersBase(((storage_pool) getSelectedItem()).getId());
        tempVar.setForceDelete(true);
        parametersBase = tempVar;
        Frontend.RunAction(VdcActionType.RemoveStoragePool, parametersBase);
        Cancel();
    }

    public void Cancel()
    {
        CancelConfirmation();

        setGuideContext(null);
        setWindow(null);

        UpdateActionAvailability();
    }

    public void CancelConfirmation()
    {
        setConfirmWindow(null);
    }

    public void OnSave()
    {
        DataCenterModel model = (DataCenterModel) getWindow();

        if (!model.Validate())
        {
            return;
        }

        if ((model.getIsNew() || model.getEntity() == null)
                && model.getQuotaEnforceTypeListModel().getSelectedItem() != QuotaEnforcementTypeEnum.DISABLED) {
            promptNoQuotaInDCMessage();
        }
        else if (getSelectedItem() != null
                && !((Version) model.getVersion().getSelectedItem()).equals(((storage_pool) getSelectedItem()).getcompatibility_version()))
        {
            ConfirmationModel confirmModel = new ConfirmationModel();
            setConfirmWindow(confirmModel);
            confirmModel.setTitle(ConstantsManager.getInstance()
                    .getConstants()
                    .changeDataCenterCompatibilityVersionTitle());
            confirmModel.setHashName("change_data_center_compatibility_version"); //$NON-NLS-1$
            confirmModel.setMessage(ConstantsManager.getInstance()
                    .getConstants()
                    .youAreAboutChangeDcCompatibilityVersionMsg());

            UICommand tempVar = new UICommand("OnSaveInternal", this); //$NON-NLS-1$
            tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
            tempVar.setIsDefault(true);
            confirmModel.getCommands().add(tempVar);
            UICommand tempVar2 = new UICommand("CancelConfirmation", this); //$NON-NLS-1$
            tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
            tempVar2.setIsCancel(true);
            confirmModel.getCommands().add(tempVar2);
        }
        else if (getSelectedItem() != null
                && ((storage_pool) getSelectedItem()).getQuotaEnforcementType() == QuotaEnforcementTypeEnum.DISABLED
                && model.getQuotaEnforceTypeListModel().getSelectedItem() != QuotaEnforcementTypeEnum.DISABLED)
        {
            checkForQuotaInDC(model.getEntity(), this);
        }
        else
        {
            OnSaveInternal();
        }
    }

    private void checkForQuotaInDC(storage_pool storage_pool, final ICommandTarget commandTarget) {
        GetQuotaByStoragePoolIdQueryParameters parameters = new GetQuotaByStoragePoolIdQueryParameters(storage_pool.getId());
        Frontend.RunQuery(VdcQueryType.GetQuotaByStoragePoolId,
                parameters,
                new AsyncQuery(
                        quotaListModel,
                        new INewAsyncCallback() {

                            @Override
                            public void OnSuccess(Object model, Object returnValue) {
                                if (((ArrayList<Quota>) ((VdcQueryReturnValue) returnValue).getReturnValue()).size() == 0) {
                                    promptNoQuotaInDCMessage();
                                } else {
                                    OnSaveInternal();
                                }
                            }
                        }));
    }

    private void promptNoQuotaInDCMessage() {
        ConfirmationModel confirmModel = new ConfirmationModel();
        setConfirmWindow(confirmModel);
        confirmModel.setTitle(ConstantsManager.getInstance()
                .getConstants()
                .changeDCQuotaEnforcementModeTitle());
        confirmModel.setHashName("change_data_center_quota_enforcement_mode"); //$NON-NLS-1$
        confirmModel.setMessage(ConstantsManager.getInstance()
                .getConstants()
                .youAreAboutChangeDCQuotaEnforcementMsg());

        UICommand tempVar = new UICommand("OnSaveInternal", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        getConfirmWindow().getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("CancelConfirmation", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        getConfirmWindow().getCommands().add(tempVar2);
    }

    public void OnSaveInternal()
    {
        DataCenterModel model = (DataCenterModel) getWindow();

        if (model.getProgress() != null)
        {
            return;
        }

        storage_pool dataCenter =
                model.getIsNew() ? new storage_pool() : (storage_pool) Cloner.clone(getSelectedItem());

        // cancel confirm window if there is
        CancelConfirmation();

        // Save changes.
        dataCenter.setname((String) model.getName().getEntity());
        dataCenter.setdescription((String) model.getDescription().getEntity());
        dataCenter.setstorage_pool_type((StorageType) model.getStorageTypeList().getSelectedItem());
        dataCenter.setcompatibility_version((Version) model.getVersion().getSelectedItem());
        dataCenter.setQuotaEnforcementType((QuotaEnforcementTypeEnum) model.getQuotaEnforceTypeListModel()
                .getSelectedItem());

        model.StartProgress(null);


        if (model.getIsNew()) {
            // When adding a data center use sync action to be able present a Guide Me dialog afterwards.
            Frontend.RunAction(VdcActionType.AddEmptyStoragePool,
                new StoragePoolManagementParameter(dataCenter),
                new IFrontendActionAsyncCallback() {
                    @Override
                    public void Executed(FrontendActionAsyncResult result) {
                        DataCenterListModel localModel = (DataCenterListModel) result.getState();
                        localModel.PostOnSaveInternal(result.getReturnValue());
                    }
                },
                this);
        } else {
            // Otherwise use async action in order to close dialog immediately.
            Frontend.RunMultipleAction(VdcActionType.UpdateStoragePool,
                new ArrayList<VdcActionParametersBase>(Arrays.asList(
                    new StoragePoolManagementParameter(dataCenter))
                ),
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void Executed(FrontendMultipleActionAsyncResult result) {
                        DataCenterListModel localModel = (DataCenterListModel) result.getState();
                        localModel.PostOnSaveInternal(result.getReturnValue().get(0));
                    }
                },
                this);
        }
    }

    public void PostOnSaveInternal(VdcReturnValueBase returnValue)
    {
        DataCenterModel model = (DataCenterModel) getWindow();

        model.StopProgress();

        Cancel();

        if (model.getIsNew() && returnValue != null && returnValue.getSucceeded()) {

            setGuideContext(returnValue.getActionReturnValue());
            UpdateActionAvailability();
            getGuideCommand().Execute();
        }
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

    @Override
    protected void ItemsCollectionChanged(Object sender, NotifyCollectionChangedEventArgs e)
    {
        super.ItemsCollectionChanged(sender, e);

        // Try to select an item corresponding to the system tree selection.
        if (getSystemTreeSelectedItem() != null
                && getSystemTreeSelectedItem().getType() == SystemTreeItemType.DataCenter)
        {
            storage_pool dataCenter = (storage_pool) getSystemTreeSelectedItem().getEntity();

            setSelectedItem(Linq.FirstOrDefault(Linq.<storage_pool> Cast(getItems()),
                    new Linq.DataCenterPredicate(dataCenter.getId())));
        }
    }

    @Override
    protected void SelectedItemPropertyChanged(Object sender, PropertyChangedEventArgs e)
    {
        super.SelectedItemPropertyChanged(sender, e);

        if (e.PropertyName.equals("status")) //$NON-NLS-1$
        {
            UpdateActionAvailability();
        }
    }

    @Override
    protected void UpdateDetailsAvailability() {
        super.UpdateDetailsAvailability();
        if (getSelectedItem() != null
                && ((storage_pool) getSelectedItem()).getQuotaEnforcementType() != QuotaEnforcementTypeEnum.DISABLED) {
            quotaListModel.setIsAvailable(true);
        } else {
            quotaListModel.setIsAvailable(false);
        }
    }

    private void UpdateActionAvailability()
    {
        ArrayList<storage_pool> items =
                getSelectedItems() != null ? new ArrayList<storage_pool>(Linq.<storage_pool> Cast(getSelectedItems()))
                        : new ArrayList<storage_pool>();

        boolean isAllDown = true;
        for (storage_pool item : items)
        {
            if (item.getstatus() == StoragePoolStatus.Up || item.getstatus() == StoragePoolStatus.Contend)
            {
                isAllDown = false;
                break;
            }
        }

        getEditCommand().setIsExecutionAllowed(getSelectedItem() != null && items.size() == 1);
        getRemoveCommand().setIsExecutionAllowed(items.size() > 0 && isAllDown);

        storage_pool storagePoolItem = (storage_pool) getSelectedItem();

        getForceRemoveCommand().setIsExecutionAllowed(storagePoolItem != null
                && items.size() == 1
                && storagePoolItem.getstatus() != StoragePoolStatus.Up);

        getGuideCommand().setIsExecutionAllowed(getGuideContext() != null
                || (getSelectedItem() != null && getSelectedItems() != null && getSelectedItems().size() == 1));

        getActivateCommand().setIsExecutionAllowed(items.size() > 0);
        if (getActivateCommand().getIsExecutionAllowed())
        {
            for (storage_pool a : items)
            {
                if (a.getstatus() == StoragePoolStatus.Up || a.getstatus() == StoragePoolStatus.Uninitialized)
                {
                    getActivateCommand().setIsExecutionAllowed(false);
                    break;
                }
            }
        }

        getRecoveryStorageCommand().setIsExecutionAllowed(items != null && items.size() == 1
                && !items.iterator().next().getstorage_pool_type().equals(StorageType.LOCALFS));

        // System tree dependent actions.
        boolean isAvailable =
                !(getSystemTreeSelectedItem() != null && getSystemTreeSelectedItem().getType() == SystemTreeItemType.DataCenter);

        getNewCommand().setIsAvailable(isAvailable);
        getRemoveCommand().setIsAvailable(isAvailable);
    }

    @Override
    public void ExecuteCommand(UICommand command)
    {
        super.ExecuteCommand(command);

        if (command == getNewCommand())
        {
            New();
        }
        else if (command == getEditCommand())
        {
            Edit();
        }
        else if (command == getRemoveCommand())
        {
            remove();
        }
        else if (command == getForceRemoveCommand())
        {
            ForceRemove();
        }
        else if (command == getActivateCommand())
        {
            Activate();
        }
        else if (command == getGuideCommand())
        {
            Guide();
        }
        else if (command == getRecoveryStorageCommand())
        {
            RecoveryStorage();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnSave")) //$NON-NLS-1$
        {
            OnSave();
        }
        else if (StringHelper.stringsEqual(command.getName(), "Cancel")) //$NON-NLS-1$
        {
            Cancel();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnRemove")) //$NON-NLS-1$
        {
            OnRemove();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnForceRemove")) //$NON-NLS-1$
        {
            OnForceRemove();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnSaveInternal")) //$NON-NLS-1$
        {
            OnSaveInternal();
        }
        else if (StringHelper.stringsEqual(command.getName(), "CancelConfirmation")) //$NON-NLS-1$
        {
            CancelConfirmation();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnRecover")) //$NON-NLS-1$
        {
            OnRecover();
        }
    }

    private SystemTreeItemModel systemTreeSelectedItem;

    @Override
    public SystemTreeItemModel getSystemTreeSelectedItem()
    {
        return systemTreeSelectedItem;
    }

    @Override
    public void setSystemTreeSelectedItem(SystemTreeItemModel value)
    {
        if (systemTreeSelectedItem != value)
        {
            systemTreeSelectedItem = value;
            OnSystemTreeSelectedItemChanged();
        }
    }

    private void OnSystemTreeSelectedItemChanged()
    {
        UpdateActionAvailability();
    }

    @Override
    protected String getListName() {
        return "DataCenterListModel"; //$NON-NLS-1$
    }
}
