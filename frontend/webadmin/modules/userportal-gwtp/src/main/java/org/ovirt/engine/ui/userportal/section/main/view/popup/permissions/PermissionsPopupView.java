package org.ovirt.engine.ui.userportal.section.main.view.popup.permissions;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.view.popup.permissions.AbstractPermissionsPopupView;
import org.ovirt.engine.ui.uicommonweb.models.users.UserPortalAdElementListModel;
import org.ovirt.engine.ui.userportal.ApplicationConstants;
import org.ovirt.engine.ui.userportal.ApplicationResources;
import org.ovirt.engine.ui.userportal.section.main.presenter.popup.permissions.PermissionsPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class PermissionsPopupView extends AbstractPermissionsPopupView<UserPortalAdElementListModel> implements PermissionsPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<UserPortalAdElementListModel, PermissionsPopupView> {
        Driver driver = GWT.create(Driver.class);
    }

    interface ViewIdHandler extends ElementIdHandler<PermissionsPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public PermissionsPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources, constants);
    }

    @Override
    protected void generateIds() {
        ViewIdHandler.idHandler.generateAndSetIds(this);
    }

    @Override
    protected void initDriver() {
        Driver.driver.initialize(this);
    }

    @Override
    public void edit(UserPortalAdElementListModel object) {
        super.edit(object);
        Driver.driver.edit(object);
    }

    @Override
    protected UserPortalAdElementListModel doFlush() {
        return Driver.driver.flush();
    }

}
