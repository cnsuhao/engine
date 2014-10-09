package org.ovirt.engine.ui.webadmin.section.main.presenter.popup.cluster;

import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.NewNetworkModel;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.datacenter.NewNetworkPopupPresenterWidget;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class NewClusterNetworkPopupPresenterWidget extends NewNetworkPopupPresenterWidget {

    public interface ViewDef extends NewNetworkPopupPresenterWidget.ViewDef {
        void setDataCenterName(String name);
    }

    @Inject
    public NewClusterNetworkPopupPresenterWidget(EventBus eventBus, ViewDef view) {
        super(eventBus, view);
    }

    @Override
    public void init(final NewNetworkModel model) {
        // Let the parent do its work
        super.init(model);

        model.getDataCenters().getSelectedItemChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                ((ViewDef) getView()).setDataCenterName(((storage_pool) model.getDataCenters().getSelectedItem()).getname());
            }
        });
    }

}
