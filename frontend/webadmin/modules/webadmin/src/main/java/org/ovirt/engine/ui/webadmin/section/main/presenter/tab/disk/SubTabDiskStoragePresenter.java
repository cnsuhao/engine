package org.ovirt.engine.ui.webadmin.section.main.presenter.tab.disk;

import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.ui.common.presenter.AbstractSubTabPresenter;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.tab.ModelBoundTabData;
import org.ovirt.engine.ui.uicommonweb.models.disks.DiskListModel;
import org.ovirt.engine.ui.uicommonweb.models.disks.DiskStorageListModel;
import org.ovirt.engine.ui.webadmin.gin.ClientGinjector;
import org.ovirt.engine.ui.webadmin.place.ApplicationPlaces;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.DiskSelectionChangeEvent;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.TabData;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.ProxyEvent;
import com.gwtplatform.mvp.client.annotations.TabInfo;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.TabContentProxyPlace;

public class SubTabDiskStoragePresenter extends AbstractSubTabPresenter<Disk, DiskListModel, DiskStorageListModel, SubTabDiskStoragePresenter.ViewDef, SubTabDiskStoragePresenter.ProxyDef> {

    @ProxyCodeSplit
    @NameToken(ApplicationPlaces.diskStorageSubTabPlace)
    public interface ProxyDef extends TabContentProxyPlace<SubTabDiskStoragePresenter> {
    }

    public interface ViewDef extends AbstractSubTabPresenter.ViewDef<Disk> {
    }

    @TabInfo(container = DiskSubTabPanelPresenter.class)
    static TabData getTabData(ClientGinjector ginjector) {
        return new ModelBoundTabData(ginjector.getApplicationConstants().diskStorageSubTabLabel(), 3,
                ginjector.getSubTabDiskStorageModelProvider());
    }

    @Inject
    public SubTabDiskStoragePresenter(EventBus eventBus, ViewDef view, ProxyDef proxy,
            PlaceManager placeManager,
            SearchableDetailModelProvider<storage_domains, DiskListModel, DiskStorageListModel> modelProvider) {
        super(eventBus, view, proxy, placeManager, modelProvider);
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, DiskSubTabPanelPresenter.TYPE_SetTabContent, this);
    }

    @Override
    protected PlaceRequest getMainTabRequest() {
        return new PlaceRequest(ApplicationPlaces.diskMainTabPlace);
    }

    @ProxyEvent
    public void onDiskSelectionChange(DiskSelectionChangeEvent event) {
        updateMainTabSelection(event.getSelectedItems());
    }

}
