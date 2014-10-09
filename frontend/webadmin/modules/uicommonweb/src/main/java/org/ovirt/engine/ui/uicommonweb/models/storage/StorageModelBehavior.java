package org.ovirt.engine.ui.uicommonweb.models.storage;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.hosts.HostModel;

public abstract class StorageModelBehavior extends Model
{
    private StorageModel privateModel;

    public StorageModel getModel()
    {
        return privateModel;
    }

    public void setModel(StorageModel value)
    {
        privateModel = value;
    }

    private String privateHash;

    public String getHash()
    {
        return privateHash;
    }

    public void setHash(String value)
    {
        privateHash = value;
    }

    public List<storage_pool> FilterDataCenter(List<storage_pool> source)
    {
        return Linq.ToList(Linq.Where(source, new Linq.DataCenterNotStatusPredicate(StoragePoolStatus.NotOperational)));
    }

    public void UpdateItemsAvailability()
    {
        if (!Frontend.getQueryStartedEvent().getListeners().contains(this))
            Frontend.getQueryStartedEvent().addListener(this);
        if (!Frontend.getQueryCompleteEvent().getListeners().contains(this))
            Frontend.getQueryCompleteEvent().addListener(this);
    }

    public void FilterUnSelectableModels()
    {
        // Filter UnSelectable models from AvailableStorageItems list
        ArrayList<Object> filterredItems = new ArrayList<Object>();
        ArrayList<IStorageModel> items = Linq.<IStorageModel> Cast(getModel().getItems());
        for (IStorageModel model : items)
        {
            if (((Model) model).getIsSelectable())
            {
                filterredItems.add(model);
            }
        }

        getModel().getAvailableStorageItems().setItems(filterredItems);
    }

    public void OnStorageModelUpdated(IStorageModel model)
    {
        // Update models list (the list is used for checking update completion)
        getModel().UpdatedStorageModels.add(model);

        // Filter UnSelectable model from AvailableStorageItems list
        if (getModel().UpdatedStorageModels.size() == Linq.<IStorageModel> Cast(getModel().getItems()).size())
        {
            getModel().UpdatedStorageModels.clear();

            getModel().getHost().setItems(new ArrayList<HostModel>());
            getModel().getHost().setSelectedItem(null);

            FilterUnSelectableModels();

            if (getModel().getSelectedItem() != null) {
                getModel().UpdateFormat();
            }
        }
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args)
    {
        super.eventRaised(ev, sender, args);

        if (ev.equals(Frontend.QueryStartedEventDefinition)
                && StringHelper.stringsEqual(Frontend.getCurrentContext(), getHash()))
        {
            getModel().Frontend_QueryStarted();
        }
        else if (ev.equals(Frontend.QueryCompleteEventDefinition)
                && StringHelper.stringsEqual(Frontend.getCurrentContext(), getHash()))
        {
            getModel().Frontend_QueryComplete();
        }
    }
}
