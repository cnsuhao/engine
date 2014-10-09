package org.ovirt.engine.ui.webadmin.section.main.view.popup.storage;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.uicommonweb.models.storage.RemoveStorageModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.storage.StorageRemovePopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class StorageRemovePopupView extends AbstractModelBoundPopupView<RemoveStorageModel>
        implements StorageRemovePopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<RemoveStorageModel, StorageRemovePopupView> {
        Driver driver = GWT.create(Driver.class);
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, StorageRemovePopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<StorageRemovePopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField
    WidgetStyle style;

    @UiField(provided = true)
    @Path(value = "hostList.selectedItem")
    @WithElementId("hostList")
    ListModelListBoxEditor<Object> hostListEditor;

    @UiField(provided = true)
    @Path(value = "format.entity")
    @WithElementId("format")
    EntityModelCheckBoxEditor formatEditor;

    @UiField
    Label message;

    @Inject
    public StorageRemovePopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources);
        initListBoxEditors();
        initCheckBoxEditors();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        localize(constants);
        addStyles();
        Driver.driver.initialize(this);
    }

    void initListBoxEditors() {
        hostListEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((VDS) object).getvds_name();
            }
        });
    }

    void initCheckBoxEditors() {
        formatEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
    }

    void addStyles() {
        formatEditor.setLabelStyleName(style.formatLabel());
        formatEditor.addContentWidgetStyleName(style.formatContentWidget());
    }

    void localize(ApplicationConstants constants) {
        hostListEditor.setLabel(constants.storageRemovePopupHostLabel());
        formatEditor.setLabel(constants.storageRemovePopupFormatLabel());
    }

    @Override
    public void edit(RemoveStorageModel object) {
        Driver.driver.edit(object);
    }

    @Override
    public RemoveStorageModel flush() {
        return Driver.driver.flush();
    }

    interface WidgetStyle extends CssResource {
        String formatLabel();

        String formatContentWidget();
    }

}
