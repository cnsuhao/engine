package org.ovirt.engine.ui.webadmin.section.main.presenter.tab.disk;

import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.ui.common.presenter.AbstractSubTabPresenter;
import org.ovirt.engine.ui.common.uicommon.model.DetailModelProvider;
import org.ovirt.engine.ui.common.widget.tab.ModelBoundTabData;
import org.ovirt.engine.ui.uicommonweb.models.disks.DiskGeneralModel;
import org.ovirt.engine.ui.uicommonweb.models.disks.DiskListModel;
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

public class SubTabDiskGeneralPresenter extends AbstractSubTabPresenter<Disk, DiskListModel, DiskGeneralModel, SubTabDiskGeneralPresenter.ViewDef, SubTabDiskGeneralPresenter.ProxyDef> {

    @ProxyCodeSplit
    @NameToken(ApplicationPlaces.diskGeneralSubTabPlace)
    public interface ProxyDef extends TabContentProxyPlace<SubTabDiskGeneralPresenter> {
    }

    public interface ViewDef extends AbstractSubTabPresenter.ViewDef<Disk> {
    }

    @TabInfo(container = DiskSubTabPanelPresenter.class)
    static TabData getTabData(ClientGinjector ginjector) {
        return new ModelBoundTabData(ginjector.getApplicationConstants().diskGeneralSubTabLabel(), 0,
                ginjector.getSubTabDiskGeneralModelProvider());
    }

    @Inject
    public SubTabDiskGeneralPresenter(EventBus eventBus, ViewDef view, ProxyDef proxy,
            PlaceManager placeManager,
            DetailModelProvider<DiskListModel, DiskGeneralModel> modelProvider) {
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
