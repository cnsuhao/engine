package org.ovirt.engine.ui.webadmin.section.main.view.popup.host;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.FlowPanel;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.HasUiCommandClickHandlers;
import org.ovirt.engine.ui.common.widget.UiCommandButton;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTab;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTabPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelPasswordBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.uicommonweb.models.ApplicationModeHelper;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.HostModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.host.HostPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

import java.util.List;

public class HostPopupView extends AbstractModelBoundPopupView<HostModel> implements HostPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<HostModel, HostPopupView> {

        Driver driver = GWT.create(Driver.class);
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, HostPopupView> {

        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<HostPopupView> {

        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField
    Style style;

    @UiField
    DialogTabPanel tabPanel;

    @UiField
    @WithElementId
    DialogTab generalTab;

    @UiField
    @WithElementId
    DialogTab powerManagementTab;

    @UiField(provided = true)
    @Path(value = "dataCenter.selectedItem")
    @WithElementId("dataCenter")
    ListModelListBoxEditor<Object> dataCenterEditor;

    @UiField(provided = true)
    @Path(value = "cluster.selectedItem")
    @WithElementId("cluster")
    ListModelListBoxEditor<Object> clusterEditor;

    @UiField
    @Path(value = "name.entity")
    @WithElementId("name")
    EntityModelTextBoxEditor nameEditor;

    @UiField
    @Path(value = "host.entity")
    @WithElementId("host")
    EntityModelTextBoxEditor hostAddressEditor;

    @UiField
    @Path(value = "rootPassword.entity")
    @WithElementId("rootPassword")
    EntityModelPasswordBoxEditor rootPasswordEditor;

    @UiField
    @Path(value = "OverrideIpTables.entity")
    @WithElementId("overrideIpTables")
    EntityModelCheckBoxEditor overrideIpTablesEditor;

    @UiField(provided = true)
    @Path(value = "isPm.entity")
    @WithElementId("isPm")
    EntityModelCheckBoxEditor pmEnabledEditor;

    @UiField(provided = true)
    @Path(value = "pmVariants.selectedItem")
    @WithElementId("pmVariants")
    ListModelListBoxOnlyEditor<Object> pmVariantsEditor;

    @UiField
    @Path(value = "pmSecondaryConcurrent.entity")
    @WithElementId("pmSecondaryConcurrent")
    EntityModelCheckBoxEditor pmSecondaryConcurrentEditor;

    @UiField
    FlowPanel pmPrimaryPanel;

    @UiField
    @Path(value = "managementIp.entity")
    @WithElementId("managementIp")
    EntityModelTextBoxEditor pmAddressEditor;

    @UiField
    @Path(value = "pmUserName.entity")
    @WithElementId("pmUserName")
    EntityModelTextBoxEditor pmUserNameEditor;

    @UiField
    @Path(value = "pmPassword.entity")
    @WithElementId("pmPassword")
    EntityModelPasswordBoxEditor pmPasswordEditor;

    @UiField(provided = true)
    @Path(value = "pmType.selectedItem")
    @WithElementId("pmType")
    ListModelListBoxEditor<Object> pmTypeEditor;

    @UiField
    @Path(value = "pmPort.entity")
    @WithElementId("pmPort")
    EntityModelTextBoxEditor pmPortEditor;

    @UiField
    @Path(value = "pmSlot.entity")
    @WithElementId("pmSlot")
    EntityModelTextBoxEditor pmSlotEditor;

    @UiField
    @Path(value = "pmOptions.entity")
    @WithElementId("pmOptions")
    EntityModelTextBoxEditor pmOptionsEditor;

    @UiField
    @Ignore
    Label pmOptionsExplanationLabel;

    @UiField
    @Path(value = "pmSecure.entity")
    @WithElementId("pmSecure")
    EntityModelCheckBoxEditor pmSecureEditor;

    @UiField
    FlowPanel pmSecondaryPanel;

    @UiField
    @Path(value = "pmSecondaryIp.entity")
    @WithElementId("pmSecondaryIp")
    EntityModelTextBoxEditor pmSecondaryAddressEditor;

    @UiField
    @Path(value = "pmSecondaryUserName.entity")
    @WithElementId("pmSecondaryUserName")
    EntityModelTextBoxEditor pmSecondaryUserNameEditor;

    @UiField
    @Path(value = "pmSecondaryPassword.entity")
    @WithElementId("pmSecondaryPassword")
    EntityModelPasswordBoxEditor pmSecondaryPasswordEditor;

    @UiField(provided = true)
    @Path(value = "pmSecondaryType.selectedItem")
    @WithElementId("pmSecondaryType")
    ListModelListBoxEditor<Object> pmSecondaryTypeEditor;

    @UiField
    @Path(value = "pmSecondaryPort.entity")
    @WithElementId("pmSecondaryPort")
    EntityModelTextBoxEditor pmSecondaryPortEditor;

    @UiField
    @Path(value = "pmSecondarySlot.entity")
    @WithElementId("pmSecondarySlot")
    EntityModelTextBoxEditor pmSecondarySlotEditor;

    @UiField
    @Path(value = "pmSecondaryOptions.entity")
    @WithElementId("pmSecondaryOptions")
    EntityModelTextBoxEditor pmSecondaryOptionsEditor;

    @UiField
    @Ignore
    Label pmSecondaryOptionsExplanationLabel;

    @UiField
    @Path(value = "pmSecondarySecure.entity")
    @WithElementId("pmSecondarySecure")
    EntityModelCheckBoxEditor pmSecondarySecureEditor;

    @UiField
    UiCommandButton testButton;

    @UiField
    UiCommandButton upButton;

    @UiField
    UiCommandButton downButton;

    @UiField
    @Ignore
    Label testMessage;

    @UiField
    @Ignore
    Label sourceLabel;

    @UiField
    ListBox proxyListBox;

    @UiField
    @Ignore
    DialogTab spmTab;

    @UiField()
    @Ignore
    VerticalPanel spmPanel;

    @Inject
    public HostPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources);
        initEditors();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        localize(constants);
        addStyles();

        Driver.driver.initialize(this);
        applyModeCustomizations();
    }

    private void addStyles() {
        overrideIpTablesEditor.addContentWidgetStyleName(style.overrideIpStyle());
    }

    private void initEditors() {
        // List boxes
        dataCenterEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((storage_pool) object).getname();
            }
        });

        clusterEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((VDSGroup) object).getname();
            }
        });

        pmVariantsEditor = new ListModelListBoxOnlyEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            protected String renderNullSafe(Object object) {
                return (String) object;
            }
        });

        pmTypeEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            protected String renderNullSafe(Object object) {
                return (String) object;
            }
        });

        pmSecondaryTypeEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            protected String renderNullSafe(Object object) {
                return (String) object;
            }
        });

        // Check boxes
        pmEnabledEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
    }

    void localize(ApplicationConstants constants) {
        // General tab
        generalTab.setLabel(constants.hostPopupGeneralTabLabel());
        dataCenterEditor.setLabel(constants.hostPopupDataCenterLabel());
        clusterEditor.setLabel(constants.hostPopupClusterLabel());
        nameEditor.setLabel(constants.hostPopupNameLabel());
        hostAddressEditor.setLabel(constants.hostPopupHostAddressLabel());
        rootPasswordEditor.setLabel(constants.hostPopupRootPasswordLabel());
        overrideIpTablesEditor.setLabel(constants.hostPopupOverrideIpTablesLabel());

        // Power Management tab
        powerManagementTab.setLabel(constants.hostPopupPowerManagementTabLabel());
        pmEnabledEditor.setLabel(constants.hostPopupPmEnabledLabel());
        pmSecondaryConcurrentEditor.setLabel(constants.hostPopupPmConcurrent());
        testButton.setLabel(constants.hostPopupTestButtonLabel());
        upButton.setLabel(constants.hostPopupUpButtonLabel());
        downButton.setLabel(constants.hostPopupDownButtonLabel());
        sourceLabel.setText(constants.hostPopupSourceText());

        // Primary
        pmAddressEditor.setLabel(constants.hostPopupPmAddressLabel());
        pmUserNameEditor.setLabel(constants.hostPopupPmUserNameLabel());
        pmPasswordEditor.setLabel(constants.hostPopupPmPasswordLabel());
        pmTypeEditor.setLabel(constants.hostPopupPmTypeLabel());
        pmPortEditor.setLabel(constants.hostPopupPmPortLabel());
        pmSlotEditor.setLabel(constants.hostPopupPmSlotLabel());
        pmOptionsEditor.setLabel(constants.hostPopupPmOptionsLabel());
        pmOptionsExplanationLabel.setText(constants.hostPopupPmOptionsExplanationLabel());
        pmSecureEditor.setLabel(constants.hostPopupPmSecureLabel());

        // Secondary
        pmSecondaryAddressEditor.setLabel(constants.hostPopupPmAddressLabel());
        pmSecondaryUserNameEditor.setLabel(constants.hostPopupPmUserNameLabel());
        pmSecondaryPasswordEditor.setLabel(constants.hostPopupPmPasswordLabel());
        pmSecondaryTypeEditor.setLabel(constants.hostPopupPmTypeLabel());
        pmSecondaryPortEditor.setLabel(constants.hostPopupPmPortLabel());
        pmSecondarySlotEditor.setLabel(constants.hostPopupPmSlotLabel());
        pmSecondaryOptionsEditor.setLabel(constants.hostPopupPmOptionsLabel());
        pmSecondaryOptionsExplanationLabel.setText(constants.hostPopupPmOptionsExplanationLabel());
        pmSecondarySecureEditor.setLabel(constants.hostPopupPmSecureLabel());

        // SPM tab
        spmTab.setLabel(constants.spmTestButtonLabel());
    }

    private void applyModeCustomizations() {
        if (ApplicationModeHelper.getUiMode() == ApplicationMode.GlusterOnly)
        {
            spmTab.setVisible(false);
            powerManagementTab.setVisible(false);
        }
    }

    @Override
    public void setMessage(String message) {
        testMessage.setText(message);
    }

    @Override
    public void edit(final HostModel object) {
        Driver.driver.edit(object);

        // TODO should be handled in a more generic way
        object.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                String propName = ((PropertyChangedEventArgs) args).PropertyName;

                if ("IsGeneralTabValid".equals(propName)) { //$NON-NLS-1$
                    if (object.getIsGeneralTabValid()) {
                        generalTab.markAsValid();
                    } else {
                        generalTab.markAsInvalid(null);
                    }
                } else if ("IsPowerManagementTabValid".equals(propName)) { //$NON-NLS-1$
                    if (object.getIsPowerManagementTabValid()) {
                        powerManagementTab.markAsValid();
                    } else {
                        powerManagementTab.markAsInvalid(null);
                    }
                }
            }
        });

        testButton.setCommand(object.getTestCommand());

        // Bind proxy commands.
        upButton.setCommand(object.getProxyUpCommand());
        upButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                object.getProxyUpCommand().Execute();
            }
        });

        downButton.setCommand(object.getProxyDownCommand());
        downButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                object.getProxyDownCommand().Execute();
            }
        });

        // Bind proxy list.
        object.getPmProxyPreferencesList().getItemsChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                proxyListBox.clear();

                for (Object item : object.getPmProxyPreferencesList().getItems()) {
                    proxyListBox.addItem((String) item);
                }
            }
        });

        object.getPmProxyPreferencesList().getSelectedItemChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {

                List items = (List) object.getPmProxyPreferencesList().getItems();
                int selectedItemIndex = items.indexOf(object.getPmProxyPreferencesList().getSelectedItem());

                proxyListBox.setSelectedIndex(selectedItemIndex);
            }
        });

        object.getPmProxyPreferencesList().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                PropertyChangedEventArgs e = (PropertyChangedEventArgs) args;
                if (e.PropertyName == "IsChangable") {  //$NON-NLS-1$
                    proxyListBox.setEnabled(object.getPmProxyPreferencesList().getIsChangable());
                }
            }
        });
        proxyListBox.setEnabled(object.getPmProxyPreferencesList().getIsChangable());

        proxyListBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                List items = (List) object.getPmProxyPreferencesList().getItems();

                Object selectedItem = proxyListBox.getSelectedIndex() >= 0
                    ? items.get(proxyListBox.getSelectedIndex())
                    : null;

                object.getPmProxyPreferencesList().setSelectedItem(selectedItem);
            }
        });

        // Create SPM related controls.
        IEventListener spmListener = new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {

                createSpmControls(object);
            }
        };

        object.getSpmPriority().getItemsChangedEvent().addListener(spmListener);
        object.getSpmPriority().getSelectedItemChangedEvent().addListener(spmListener);

        createSpmControls(object);


        // Wire events on power management related controls.
        object.getPmVariants().getSelectedItemChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {

                ListModel model = (ListModel) sender;
                List items = (List) model.getItems();
                Object selectedItem = model.getSelectedItem();

                updatePmPanelsVisibility(items.indexOf(selectedItem) == 0);
            }
        });

        updatePmPanelsVisibility(true);
    }

    private void updatePmPanelsVisibility(boolean primary) {

        pmPrimaryPanel.setVisible(primary);
        pmSecondaryPanel.setVisible(!primary);
    }

    private void createSpmControls(final HostModel object) {

        spmPanel.clear();

        Iterable items = object.getSpmPriority().getItems();
        if (items == null) {
            return;
        }

        // Recreate SPM related controls.
        for (Object item : items) {

            final EntityModel model = (EntityModel) item;

            RadioButton rb = new RadioButton("spm"); // $//$NON-NLS-1$
            rb.setText(model.getTitle());
            rb.setValue(object.getSpmPriority().getSelectedItem() == model);
            rb.addStyleName(style.radioButton());

            rb.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> e) {
                    object.getSpmPriority().setSelectedItem(model);
                }
            });

            spmPanel.add(rb);
        }
    }

    @Override
    public HostModel flush() {
        return Driver.driver.flush();
    }

    @Override
    public void focusInput() {
        nameEditor.setFocus(true);
    }

    @Override
    public HasUiCommandClickHandlers getTestButton() {
        return testButton;
    }

    @Override
    public void showPowerManagement() {
        tabPanel.switchTab(powerManagementTab);
    }

    interface Style extends CssResource {

        String radioButton();

        String overrideIpStyle();
    }
}
