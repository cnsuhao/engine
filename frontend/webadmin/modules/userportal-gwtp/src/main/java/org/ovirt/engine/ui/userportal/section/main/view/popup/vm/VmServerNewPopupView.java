package org.ovirt.engine.ui.userportal.section.main.view.popup.vm;

import org.ovirt.engine.ui.common.CommonApplicationMessages;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.view.popup.AbstractVmPopupView;
import org.ovirt.engine.ui.common.widget.uicommon.popup.vm.VmServerNewPopupWidget;
import org.ovirt.engine.ui.uicommonweb.models.vms.UnitVmModel;
import org.ovirt.engine.ui.userportal.ApplicationConstants;
import org.ovirt.engine.ui.userportal.ApplicationResources;
import org.ovirt.engine.ui.userportal.section.main.presenter.popup.vm.VmServerNewPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class VmServerNewPopupView extends AbstractVmPopupView implements VmServerNewPopupPresenterWidget.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<VmServerNewPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public VmServerNewPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants, CommonApplicationMessages messages) {
        super(eventBus, resources, new VmServerNewPopupWidget(constants, resources, messages) {
            @Override
            protected void setupHostTabAvailability(UnitVmModel model) {
                hostTab.setVisible(false);
            }
        });
        ViewIdHandler.idHandler.generateAndSetIds(this);
    }

}
