package org.ovirt.engine.ui.webadmin.section.main.view.popup.host;

import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkBootProtocol;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.core.compat.KeyValuePairCompat;
import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelLabelEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.HostBondInterfaceModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.host.HostBondPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.widget.editor.EnumRadioEditor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class HostBondPopupView extends AbstractModelBoundPopupView<HostBondInterfaceModel> implements HostBondPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<HostBondInterfaceModel, HostBondPopupView> {
        Driver driver = GWT.create(Driver.class);
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, HostBondPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField(provided = true)
    @Path(value = "bond.selectedItem")
    ListModelListBoxEditor<Object> bondEditor;

    @UiField(provided = true)
    @Path(value = "network.selectedItem")
    ListModelListBoxEditor<Object> networkEditor;

    @UiField(provided = true)
    @Path(value = "bondingOptions.selectedItem")
    ListModelListBoxEditor<Object> bondingModeEditor;

    @UiField
    @Ignore
    EntityModelTextBoxEditor customEditor;

    @UiField(provided = true)
    EnumRadioEditor<NetworkBootProtocol> bootProtocol;

    @UiField
    @Ignore
    EntityModelLabelEditor bootProtocolLabel;

    @UiField
    @Path(value = "address.entity")
    EntityModelTextBoxEditor address;

    @UiField
    @Path(value = "subnet.entity")
    EntityModelTextBoxEditor subnet;

    @UiField
    @Path(value = "gateway.entity")
    EntityModelTextBoxEditor gateway;

    @UiField(provided = true)
    @Path(value = "checkConnectivity.entity")
    EntityModelCheckBoxEditor checkConnectivity;

    @UiField
    @Ignore
    Label message;

    @UiField
    @Ignore
    HTML info;

    @UiField
    @Ignore
    DockLayoutPanel layoutPanel;

    @UiField
    @Ignore
    VerticalPanel mainPanel;

    @UiField
    @Ignore
    VerticalPanel infoPanel;

    @UiField(provided = true)
    @Path(value = "commitChanges.entity")
    EntityModelCheckBoxEditor commitChanges;

    @UiField
    Style style;

    @Inject
    public HostBondPopupView(EventBus eventBus, ApplicationResources resources, final ApplicationConstants constants) {
        super(eventBus, resources);

        bondEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            protected String renderNullSafe(Object object) {
                return ((VdsNetworkInterface) object).getName();
            }

        });
        networkEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            protected String renderNullSafe(Object object) {
                return ((Network) object).getName();
            }

        });
        bondingModeEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @SuppressWarnings("unchecked")
            @Override
            protected String renderNullSafe(Object object) {
                KeyValuePairCompat<String, EntityModel> pair = (KeyValuePairCompat<String, EntityModel>) object;
                String key = pair.getKey();
                if ("custom".equals(key)) { //$NON-NLS-1$
                    return constants.customHostPopup() + ":"; //$NON-NLS-1$
                }
                EntityModel value = pair.getValue();
                return (String) value.getEntity();
            }
        });
        bootProtocol = new EnumRadioEditor<NetworkBootProtocol>(NetworkBootProtocol.class, eventBus);

        checkConnectivity = new EntityModelCheckBoxEditor(Align.RIGHT);
        commitChanges = new EntityModelCheckBoxEditor(Align.RIGHT);

        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));

        // Set Styles
        checkConnectivity.setContentWidgetStyleName(style.checkCon());

        // Localize
        bondEditor.setLabel(constants.bondNameHostPopup() + ":"); //$NON-NLS-1$
        networkEditor.setLabel(constants.networkHostPopup() + ":"); //$NON-NLS-1$
        bondingModeEditor.setLabel(constants.bondingModeHostPopup() + ":"); //$NON-NLS-1$
        customEditor.setLabel(constants.customModeHostPopup() + ":"); //$NON-NLS-1$
        bootProtocolLabel.setLabel(constants.bootProtocolHostPopup() + ":"); //$NON-NLS-1$
        bootProtocolLabel.asEditor().getSubEditor().setValue("   "); //$NON-NLS-1$
        address.setLabel(constants.ipHostPopup() + ":"); //$NON-NLS-1$
        subnet.setLabel(constants.subnetMaskHostPopup() + ":"); //$NON-NLS-1$
        gateway.setLabel(constants.defaultGwHostPopup() + ":"); //$NON-NLS-1$
        checkConnectivity.setLabel(constants.checkConHostPopup());
        info.setHTML(constants.changesTempHostPopup());
        commitChanges.setLabel(constants.saveNetConfigHostPopup());

        Driver.driver.initialize(this);
    }

    @Override
    public void edit(final HostBondInterfaceModel object) {
        Driver.driver.edit(object);
        if (!object.getBootProtocolAvailable()) {
            bootProtocol.asWidget().setVisible(false);
            bootProtocolLabel.setVisible(false);
        }
        bootProtocol.setEnabled(NetworkBootProtocol.NONE, object.getNoneBootProtocolAvailable());
        updateBondOptions(object.getBondingOptions());

        object.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                HostBondInterfaceModel model = (HostBondInterfaceModel) sender;
                String propertyName = ((PropertyChangedEventArgs) args).PropertyName;
                if ("NoneBootProtocolAvailable".equals(propertyName)) { //$NON-NLS-1$
                    bootProtocol.setEnabled(NetworkBootProtocol.NONE, model.getNoneBootProtocolAvailable());
                }
                else if ("Message".equals(propertyName)) { //$NON-NLS-1$
                    message.setText(model.getMessage());
                }
            }
        });

        object.getBondingOptions().getSelectedItemChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                ListModel list = (ListModel) sender;
                updateBondOptions(list);
            }
        });

        customEditor.asValueBox().addValueChangeHandler(new ValueChangeHandler<Object>() {
            @SuppressWarnings("unchecked")
            @Override
            public void onValueChange(ValueChangeEvent<Object> event) {
                for (Object item : object.getBondingOptions().getItems()) {
                    KeyValuePairCompat<String, EntityModel> pair = (KeyValuePairCompat<String, EntityModel>) item;
                    if ("custom".equals(pair.getKey())) { //$NON-NLS-1$
                        pair.getValue().setEntity(event.getValue());
                    }
                }
            }
        });

        bondingModeEditor.setVisible(true);
        bondingModeEditor.asWidget().setVisible(true);

        if (object.isCompactMode()) {
            // hide widgets
            info.setVisible(false);
            message.setVisible(false);
            checkConnectivity.setVisible(false);
            commitChanges.setVisible(false);
            // resize
            layoutPanel.remove(infoPanel);
            layoutPanel.setWidgetSize(mainPanel, 300);
            asPopupPanel().setPixelSize(400, 400);
        }

    }

    @Override
    public HostBondInterfaceModel flush() {
        return Driver.driver.flush();
    }

    @Override
    public void focusInput() {
        networkEditor.setFocus(true);
    }

    @Override
    public void setMessage(String message) {
    }

    private void updateBondOptions(ListModel list) {
        @SuppressWarnings("unchecked")
        KeyValuePairCompat<String, EntityModel> pair =
                (KeyValuePairCompat<String, EntityModel>) list.getSelectedItem();
        if ("custom".equals(pair.getKey())) { //$NON-NLS-1$
            customEditor.setVisible(true);
            Object entity = pair.getValue().getEntity();
            customEditor.asEditor().getSubEditor().setValue(entity == null ? "" : entity); //$NON-NLS-1$
        } else {
            customEditor.setVisible(false);
        }
    }

    interface Style extends CssResource {

        String checkCon();
    }

}
