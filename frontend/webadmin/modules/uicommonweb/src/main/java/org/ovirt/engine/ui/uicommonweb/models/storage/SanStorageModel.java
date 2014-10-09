package org.ovirt.engine.ui.uicommonweb.models.storage;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.LUNs;
import org.ovirt.engine.core.common.businessentities.LunStatus;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.queries.GetDeviceListQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.core.compat.ObservableCollection;
import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicompat.Constants;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Messages;

@SuppressWarnings("unused")
public abstract class SanStorageModel extends SanStorageModelBase
{
    private boolean isGrouppedByTarget;

    /**
     * Gets or sets the value determining whether the items containing target/LUNs or LUN/targets.
     */
    public boolean getIsGrouppedByTarget()
    {
        return isGrouppedByTarget;
    }

    public void setIsGrouppedByTarget(boolean value)
    {
        if (isGrouppedByTarget != value)
        {
            isGrouppedByTarget = value;
            IsGrouppedByTargetChanged();
            OnPropertyChanged(new PropertyChangedEventArgs("IsGrouppedByTarget")); //$NON-NLS-1$
        }
    }

    private String getLUNsFailure;

    public String getGetLUNsFailure()
    {
        return getLUNsFailure;
    }

    public void setGetLUNsFailure(String value)
    {
        if (!StringHelper.stringsEqual(getLUNsFailure, value))
        {
            getLUNsFailure = value;
            OnPropertyChanged(new PropertyChangedEventArgs("GetLUNsFailure")); //$NON-NLS-1$
        }
    }

    private storage_domains storageDomain;

    public storage_domains getStorageDomain() {
        return storageDomain;
    }

    public void setStorageDomain(storage_domains storageDomain) {
        this.storageDomain = storageDomain;
    }

    private boolean force;

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    private final List<LunModel> includedLUNs;
    private final ArrayList<SanTargetModel> lastDiscoveredTargets;
    private boolean isTargetModelList;

    protected SanStorageModel()
    {
        includedLUNs = new ArrayList<LunModel>();
        lastDiscoveredTargets = new ArrayList<SanTargetModel>();

        InitializeItems(null, null);
    }

    @Override
    protected void PostDiscoverTargets(ArrayList<SanTargetModel> newItems)
    {
        super.PostDiscoverTargets(newItems);

        InitializeItems(null, newItems);

        // Remember all discovered targets.
        lastDiscoveredTargets.clear();
        lastDiscoveredTargets.addAll(newItems);
    }

    @Override
    protected void Update()
    {
        lastDiscoveredTargets.clear();

        super.Update();
    }

    @Override
    protected void UpdateInternal()
    {
        super.UpdateInternal();

        if (!getContainer().isStorageActive()) {
            return;
        }

        VDS host = (VDS) getContainer().getHost().getSelectedItem();
        if (host == null)
        {
            ProposeDiscover();
            return;
        }

        ClearItems();
        InitializeItems(null, null);

        AsyncQuery asyncQuery = new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object target, Object returnValue) {
                SanStorageModel model = (SanStorageModel) target;
                VdcQueryReturnValue response = (VdcQueryReturnValue) returnValue;
                if (response.getSucceeded()) {
                    model.ApplyData((ArrayList<LUNs>) response.getReturnValue(), false);
                    model.setGetLUNsFailure(""); //$NON-NLS-1$
                }
                else {
                    model.setGetLUNsFailure(
                            ConstantsManager.getInstance().getConstants().couldNotRetrieveLUNsLunsFailure());
                }
            }
        }, true);
        asyncQuery.setContext(getHash());
        Frontend.RunQuery(VdcQueryType.GetDeviceList,
                new GetDeviceListQueryParameters(host.getId(), getType()),
                asyncQuery);
    }

    private void ClearItems()
    {
        if (getItems() == null)
        {
            return;
        }

        if (getIsGrouppedByTarget())
        {
            List<SanTargetModel> items = (List<SanTargetModel>) getItems();

            for (SanTargetModel target : Linq.ToList(items))
            {
                boolean found = false;

                // Ensure remove targets that are not in last dicovered targets list.
                if (Linq.FirstOrDefault(lastDiscoveredTargets, new Linq.TargetPredicate(target)) != null)
                {
                    found = true;
                }
                else
                {
                    // Ensure remove targets that are not contain already included LUNs.
                    for (LunModel lun : target.getLuns())
                    {
                        LunModel foundItem = Linq.FirstOrDefault(includedLUNs, new Linq.LunPredicate(lun));
                        if (foundItem == null)
                        {
                            found = true;
                            break;
                        }
                    }
                }

                if (!found)
                {
                    items.remove(target);
                }
            }
        }
        else
        {
            List<LunModel> items = (List<LunModel>) getItems();

            // Ensure remove targets that are not contain already included LUNs.
            for (LunModel lun : Linq.ToList(items))
            {
                LunModel foundItem = Linq.FirstOrDefault(includedLUNs, new Linq.LunPredicate(lun));
                if (foundItem == null)
                {
                    items.remove(lun);
                }
            }
        }
    }

    /**
     * Creates model items from the provided list of business entities.
     */
    public void ApplyData(List<LUNs> source, boolean isIncluded)
    {
        ArrayList<LunModel> newItems = new ArrayList<LunModel>();

        for (LUNs a : source)
        {
            if (a.getLunType() == getType() || a.getLunType() == StorageType.UNKNOWN)
            {
                ArrayList<SanTargetModel> targets = new ArrayList<SanTargetModel>();

                if (a.getLunConnections() != null)
                {
                    for (StorageServerConnections b : a.getLunConnections())
                    {
                        SanTargetModel tempVar = new SanTargetModel();
                        tempVar.setAddress(b.getconnection());
                        tempVar.setPort(b.getport());
                        tempVar.setName(b.getiqn());
                        tempVar.setIsSelected(true);
                        tempVar.setIsLoggedIn(true);
                        tempVar.setLuns(new ObservableCollection<LunModel>());
                        SanTargetModel model = tempVar;
                        model.getLoginCommand().setIsExecutionAllowed(false);

                        targets.add(model);
                    }
                }

                LunModel lunModel = new LunModel();
                lunModel.setLunId(a.getLUN_id());
                lunModel.setVendorId(a.getVendorId());
                lunModel.setProductId(a.getProductId());
                lunModel.setSerial(a.getSerial());
                lunModel.setMultipathing(a.getPathCount());
                lunModel.setTargets(targets);
                lunModel.setSize(a.getDeviceSize());
                lunModel.setIsAccessible(a.getAccessible());
                lunModel.setStatus(a.getStatus());
                lunModel.setIsIncluded(isIncluded);
                lunModel.setIsSelected(isIncluded);
                lunModel.setEntity(a);

                // Add LunModel
                newItems.add(lunModel);

                // Update isGrayedOut and grayedOutReason properties
                UpdateGrayedOut(lunModel);

                // Remember included LUNs to prevent their removal while updating items.
                if (isIncluded)
                {
                    includedLUNs.add(lunModel);
                }
            }
        }

        InitializeItems(newItems, null);
        ProposeDiscover();
    }

    private void UpdateGrayedOut(LunModel lunModel) {
        Constants constants = ConstantsManager.getInstance().getConstants();
        Messages messages = ConstantsManager.getInstance().getMessages();

        LUNs lun = (LUNs) lunModel.getEntity();
        boolean nonEmpty = lun.getStorageDomainId() != null || lun.getDiskId() != null ||
                lun.getStatus() == LunStatus.Unusable;

        // Graying out LUNs
        lunModel.setIsGrayedOut(isIgnoreGrayedOut() ? lun.getDiskId() != null : nonEmpty);

        // Adding 'GrayedOutReasons'
        if (lun.getStorageDomainId() != null) {
            lunModel.getGrayedOutReasons().add(
                    messages.lunAlreadyPartOfStorageDomainWarning(lun.getStorageDomainName()));
        }
        else if (lun.getDiskId() != null) {
            lunModel.getGrayedOutReasons().add(
                    messages.lunUsedByDiskWarning(lun.getDiskAlias()));
        }
        else if (lun.getStatus() == LunStatus.Unusable) {
            lunModel.getGrayedOutReasons().add(
                    constants.lunUnusable());
        }
    }

    private void IsGrouppedByTargetChanged()
    {
        InitializeItems(null, null);
    }

    /**
     * Organizes items according to the current groupping flag. When new items provided takes them in account and add to
     * the Items collection.
     */
    private void InitializeItems(List<LunModel> newLuns, List<SanTargetModel> newTargets)
    {
        if (getIsGrouppedByTarget())
        {
            if (getItems() == null)
            {
                setItems(new ObservableCollection<SanTargetModel>());
                isTargetModelList = true;
            }
            else
            {
                // Convert to list of another type as neccessary.
                if (!isTargetModelList)
                {
                    setItems(ToTargetModelList((List<LunModel>) getItems()));
                }
            }

            ArrayList<SanTargetModel> items = new ArrayList<SanTargetModel>();
            items.addAll((List<SanTargetModel>) getItems());

            // Add new targets.
            if (newTargets != null)
            {
                for (SanTargetModel newItem : newTargets)
                {
                    if (Linq.FirstOrDefault(items, new Linq.TargetPredicate(newItem)) == null)
                    {
                        items.add(newItem);
                    }
                }
            }

            // Merge luns into targets.
            if (newLuns != null)
            {
                MergeLunsToTargets(newLuns, items);
            }

            setItems(items);

            UpdateLoginAllAvailability();
        }
        else
        {
            if (getItems() == null)
            {
                setItems(new ObservableCollection<LunModel>());
                isTargetModelList = false;
            }
            else
            {
                // Convert to list of another type as neccessary.
                if (isTargetModelList)
                {
                    setItems(ToLunModelList((List<SanTargetModel>) getItems()));
                }
            }

            ArrayList<LunModel> items = new ArrayList<LunModel>();
            items.addAll((List<LunModel>) getItems());

            // Add new LUNs.
            if (newLuns != null)
            {
                for (LunModel newItem : newLuns)
                {
                    LunModel existingItem = Linq.FirstOrDefault(items, new Linq.LunPredicate(newItem));
                    if (existingItem == null)
                    {
                        items.add(newItem);
                    }
                    else
                    {
                        existingItem.setIsIncluded(existingItem.getIsIncluded() || newItem.getIsIncluded());
                    }
                }
            }

            setItems(items);
        }
    }

    private void MergeLunsToTargets(List<LunModel> newLuns, List<SanTargetModel> targets)
    {
        for (LunModel lun : newLuns)
        {
            for (SanTargetModel target : lun.getTargets())
            {
                SanTargetModel item = Linq.FirstOrDefault(targets, new Linq.TargetPredicate(target));
                if (item == null)
                {
                    item = target;
                    targets.add(item);
                }

                LunModel currLun = Linq.FirstOrDefault(item.getLuns(), new Linq.LunPredicate(lun));
                if (currLun == null) {
                    item.getLuns().add(lun);
                } else {
                    currLun.setLunId(lun.getLunId());
                    currLun.setVendorId(lun.getVendorId());
                    currLun.setProductId(lun.getProductId());
                    currLun.setSerial(lun.getSerial());
                    currLun.setMultipathing(lun.getMultipathing());
                    currLun.setTargets((ArrayList) targets);
                    currLun.setSize(lun.getSize());
                    currLun.setIsAccessible(lun.getIsAccessible());
                    currLun.setStatus(lun.getStatus());
                    currLun.setIsIncluded(lun.getIsIncluded());
                    currLun.setIsSelected(lun.getIsSelected());
                    currLun.setEntity(lun.getEntity());
                }
            }

            // Adding PropertyEventListener to LunModel
            if (!isMultiSelection()) {
                lun.getPropertyChangedEvent().removeListener(lunModelEventListener);
                lun.getPropertyChangedEvent().addListener(lunModelEventListener);
            }
        }
    }

    final IEventListener lunModelEventListener = new IEventListener() {
        @Override
        public void eventRaised(Event ev, Object sender, EventArgs args) {
            String propName = ((PropertyChangedEventArgs) args).PropertyName;
            if (propName.equals("IsSelected")) { //$NON-NLS-1$
                LunModel selectedLunModel = (LunModel) sender;

                if (!selectedLunModel.getIsSelected() || !getItems().iterator().hasNext()) {
                    return;
                }

                // Clear LUNs selection
                for (Model model : (List<Model>) getItems()) {
                    if (model instanceof LunModel) {
                        LunModel lunModel = (LunModel) model;
                        if (!lunModel.equals(selectedLunModel)) {
                            lunModel.setIsSelected(false);
                        }
                    }
                    else {
                        SanTargetModel sanTargetModel = (SanTargetModel) model;
                        boolean isIncludeSelected = false;

                        for (LunModel lunModel : sanTargetModel.getLuns()) {
                            if (!lunModel.equals(selectedLunModel)) {
                                lunModel.setIsSelected(false);
                            }
                            else {
                                isIncludeSelected = true;
                            }
                        }

                        if (!isIncludeSelected && sanTargetModel.getLunsList().getSelectedItem() != null) {
                            sanTargetModel.getLunsList().setSelectedItem(null);
                        }
                    }
                }
            }
        }
    };

    private List<SanTargetModel> ToTargetModelList(List<LunModel> source)
    {
        ObservableCollection<SanTargetModel> list = new ObservableCollection<SanTargetModel>();

        for (LunModel lun : source)
        {
            for (SanTargetModel target : lun.getTargets())
            {
                SanTargetModel item = Linq.FirstOrDefault(list, new Linq.TargetPredicate(target));
                if (item == null)
                {
                    item = target;
                    list.add(item);
                }

                if (Linq.FirstOrDefault(item.getLuns(), new Linq.LunPredicate(lun)) == null)
                {
                    item.getLuns().add(lun);
                }
            }
        }

        // Merge with last discovered targets list.
        for (SanTargetModel target : lastDiscoveredTargets)
        {
            if (Linq.FirstOrDefault(list, new Linq.TargetPredicate(target)) == null)
            {
                list.add(target);
            }
        }

        isTargetModelList = true;

        return list;
    }

    private List<LunModel> ToLunModelList(List<SanTargetModel> source)
    {
        ObservableCollection<LunModel> list = new ObservableCollection<LunModel>();

        for (SanTargetModel target : source)
        {
            for (LunModel lun : target.getLuns())
            {
                LunModel item = Linq.FirstOrDefault(list, new Linq.LunPredicate(lun));
                if (item == null)
                {
                    item = lun;
                    list.add(item);
                }

                if (Linq.FirstOrDefault(item.getTargets(), new Linq.TargetPredicate(target)) == null)
                {
                    item.getTargets().add(target);
                }
            }
        }

        isTargetModelList = false;

        return list;
    }

    private void ProposeDiscover()
    {
        boolean proposeDiscover =
                !getProposeDiscoverTargets() && (getItems() == null || Linq.Count(getItems()) == 0);

        setProposeDiscoverTargets(proposeDiscover);
    }

    @Override
    protected void IsAllLunsSelectedChanged()
    {
        if (!getIsGrouppedByTarget())
        {
            List<LunModel> items = (List<LunModel>) getItems();
            for (LunModel lun : items)
            {
                if (!lun.getIsIncluded() && lun.getIsAccessible())
                {
                    lun.setIsSelected(getIsAllLunsSelected());
                }
            }
        }
    }

    public ArrayList<LunModel> getAddedLuns()
    {
        ArrayList<LunModel> luns = new ArrayList<LunModel>();
        if (getIsGrouppedByTarget())
        {
            List<SanTargetModel> items = (List<SanTargetModel>) getItems();
            for (SanTargetModel item : items)
            {
                for (LunModel lun : item.getLuns())
                {
                    if (lun.getIsSelected() && !lun.getIsIncluded()
                            && Linq.FirstOrDefault(luns, new Linq.LunPredicate(lun)) == null)
                    {
                        luns.add(lun);
                    }
                }
            }
        }
        else
        {
            List<LunModel> items = (List<LunModel>) getItems();
            for (LunModel lun : items)
            {
                if (lun.getIsSelected() && !lun.getIsIncluded()
                        && Linq.FirstOrDefault(luns, new Linq.LunPredicate(lun)) == null)
                {
                    luns.add(lun);
                }
            }
        }

        return luns;
    }

    public ArrayList<String> getUsedLunsMessages() {
        ArrayList<String> usedLunsMessages = new ArrayList<String>();
        Messages messages = ConstantsManager.getInstance().getMessages();

        for (LunModel lunModel : getAddedLuns()) {
            if (lunModel.getStatus() == LunStatus.Used) {
                String reason = null;
                LUNs lun = (LUNs) lunModel.getEntity();

                if (lun.getvolume_group_id() != null && !lun.getvolume_group_id().isEmpty()) {
                    reason = messages.lunUsedByVG(lun.getvolume_group_id());
                }

                usedLunsMessages.add(reason == null ? lunModel.getLunId() :
                        lunModel.getLunId() + " (" + reason + ")"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return usedLunsMessages;
    }

    public ArrayList<String> getPartOfSdLunsMessages() {
        ArrayList<String> partOfSdLunsMessages = new ArrayList<String>();
        Messages messages = ConstantsManager.getInstance().getMessages();

        for (LunModel lunModel : getAddedLuns()) {
            String reason = null;
            LUNs lun = (LUNs) lunModel.getEntity();

            if (lun.getStorageDomainId() != null) {
                reason = messages.lunAlreadyPartOfStorageDomainWarning(lun.getStorageDomainName());
                partOfSdLunsMessages.add(lunModel.getLunId() + " (" + reason + ")"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return partOfSdLunsMessages;
    }

    @Override
    public boolean Validate()
    {
        boolean isValid = getAddedLuns().size() > 0 || includedLUNs.size() > 0;

        if (!isValid)
        {
            getInvalidityReasons().add(ConstantsManager.getInstance().getConstants().noLUNsSelectedInvalidReason());
        }

        setIsValid(isValid);

        return super.Validate() && getIsValid();
    }
}
