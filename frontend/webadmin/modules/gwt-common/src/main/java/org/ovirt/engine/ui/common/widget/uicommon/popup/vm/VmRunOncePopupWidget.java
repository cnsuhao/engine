package org.ovirt.engine.ui.common.widget.uicommon.popup.vm;

import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.ComboBox;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelRadioButtonEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.form.key_value.KeyValueWidget;
import org.ovirt.engine.ui.common.widget.uicommon.popup.AbstractModelBoundPopupWidget;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.BootSequenceModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.RunOnceModel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class VmRunOncePopupWidget extends AbstractModelBoundPopupWidget<RunOnceModel> {

    interface Driver extends SimpleBeanEditorDriver<RunOnceModel, VmRunOncePopupWidget> {
        Driver driver = GWT.create(Driver.class);
    }

    interface ViewUiBinder extends UiBinder<FlowPanel, VmRunOncePopupWidget> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<VmRunOncePopupWidget> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    interface Style extends CssResource {
        String attachImageCheckBoxLabel();

        String attachImageSelectBoxLabel();

        String attachImageSelectbox();
    }

    @UiField
    Style style;

    @UiField
    @WithElementId
    DisclosurePanel generalBootOptionsPanel;

    @UiField
    @WithElementId
    DisclosurePanel linuxBootOptionsPanel;

    @UiField
    @WithElementId
    DisclosurePanel initialRunPanel;

    @UiField
    @WithElementId
    DisclosurePanel displayProtocolPanel;

    @UiField
    @WithElementId
    DisclosurePanel customPropertiesPanel;

    @UiField
    @Ignore
    Label initialRunLabel;

    @UiField
    @Path(value = "floppyImage.selectedItem")
    @WithElementId("floppyImage")
    ListModelListBoxEditor<Object> floppyImageEditor;

    @UiField
    @Ignore
    KeyValueWidget customPropertiesSheetEditor;

    @UiField
    @Path(value = "isoImage.selectedItem")
    @WithElementId("isoImage")
    ListModelListBoxEditor<Object> isoImageEditor;

    @UiField(provided = true)
    @Path(value = "attachFloppy.entity")
    @WithElementId("attachFloppy")
    EntityModelCheckBoxEditor attachFloppyEditor;

    @UiField(provided = true)
    @Path(value = "attachIso.entity")
    @WithElementId("attachIso")
    EntityModelCheckBoxEditor attachIsoEditor;

    @UiField(provided = true)
    @Path(value = "runAsStateless.entity")
    @WithElementId("runAsStateless")
    EntityModelCheckBoxEditor runAsStatelessEditor;

    @UiField(provided = true)
    @Path(value = "runAndPause.entity")
    @WithElementId("runAndPause")
    EntityModelCheckBoxEditor runAndPauseEditor;

    @UiField
    @Path(value = "Kernel_path.entity")
    @WithElementId("kernelPath")
    EntityModelTextBoxEditor kernelPathEditor;

    @UiField
    @Path(value = "Initrd_path.entity")
    @WithElementId("initrdPath")
    EntityModelTextBoxEditor initrdPathEditor;

    @UiField
    @Path(value = "Kernel_parameters.entity")
    @WithElementId("kernelParameters")
    EntityModelTextBoxEditor kernelParamsEditor;

    @UiField(provided = true)
    @WithElementId("sysPrepDomainNameComboBox")
    ComboBox sysPrepDomainNameComboBox;

    @Path(value = "sysPrepDomainName.selectedItem")
    @WithElementId("sysPrepDomainNameListBox")
    ListModelListBoxEditor<Object> sysPrepDomainNameListBoxEditor;

    @Path(value = "SysPrepSelectedDomainName.entity")
    @WithElementId("sysPrepDomainNameTextBox")
    EntityModelTextBoxEditor sysPrepDomainNameTextBoxEditor;

    @UiField(provided = true)
    @Path(value = "useAlternateCredentials.entity")
    @WithElementId("useAlternateCredentials")
    EntityModelCheckBoxEditor useAlternateCredentialsEditor;

    @UiField
    @Path(value = "sysPrepUserName.entity")
    @WithElementId("sysPrepUserName")
    EntityModelTextBoxEditor sysPrepUserNameEditor;

    @UiField
    @Path(value = "sysPrepPassword.entity")
    @WithElementId("sysPrepPassword")
    EntityModelTextBoxEditor sysPrepPasswordEditor;

    @UiField(provided = true)
    @Path(value = "displayConsole_Vnc_IsSelected.entity")
    @WithElementId("displayConsoleVnc")
    EntityModelRadioButtonEditor displayConsoleVncEditor;

    @UiField(provided = true)
    @Path(value = "displayConsole_Spice_IsSelected.entity")
    @WithElementId("displayConsoleSpice")
    EntityModelRadioButtonEditor displayConsoleSpiceEditor;

    @UiField
    @WithElementId
    ButtonBase bootSequenceUpButton;

    @UiField
    @WithElementId
    ButtonBase bootSequenceDownButton;

    @UiField
    AbsolutePanel bootSequencePanel;

    @UiField
    @Ignore
    Label bootSequenceLabel;

    @WithElementId("bootSequence")
    ListBox bootSequenceBox;

    private BootSequenceModel bootSequenceModel;

    private CommonApplicationResources resources;

    private final CommonApplicationConstants constants;

    @UiFactory
    protected DisclosurePanel createPanel(String label)
    {
        return new DisclosurePanel(resources.decreaseIcon(), resources.increaseIcon(), label);
    }

    public VmRunOncePopupWidget(CommonApplicationConstants constants, CommonApplicationResources resources) {
        this.constants = constants;
        this.resources = resources;
        initCheckBoxEditors();
        initRadioButtonEditors();
        initComboBox();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        initBootSequenceBox();

        localize();
        addStyles();
        ViewIdHandler.idHandler.generateAndSetIds(this);

        Driver.driver.initialize(this);
    }

    void localize() {
        // Boot Options
        runAsStatelessEditor.setLabel(constants.runOncePopupRunAsStatelessLabel());
        runAndPauseEditor.setLabel(constants.runOncePopupRunAndPauseLabel());
        attachFloppyEditor.setLabel(constants.runOncePopupAttachFloppyLabel());
        attachIsoEditor.setLabel(constants.runOncePopupAttachIsoLabel());
        bootSequenceLabel.setText(constants.runOncePopupBootSequenceLabel());

        // Linux Boot Options
        kernelPathEditor.setLabel(constants.runOncePopupKernelPathLabel());
        initrdPathEditor.setLabel(constants.runOncePopupInitrdPathLabel());
        kernelParamsEditor.setLabel(constants.runOncePopupKernelParamsLabel());

        // WindowsSysprep
        initialRunLabel.setText(constants.runOncePopupInitialRunLabel());
        sysPrepDomainNameListBoxEditor.setLabel(constants.runOncePopupSysPrepDomainNameLabel());
        useAlternateCredentialsEditor.setLabel(constants.runOnceUseAlternateCredentialsLabel());
        sysPrepUserNameEditor.setLabel(constants.runOncePopupSysPrepUserNameLabel());
        sysPrepPasswordEditor.setLabel(constants.runOncePopupSysPrepPasswordLabel());

        // Display Protocol
        displayConsoleVncEditor.setLabel(constants.runOncePopupDisplayConsoleVncLabel());
        displayConsoleSpiceEditor.setLabel(constants.runOncePopupDisplayConsoleSpiceLabel());
    }

    void initCheckBoxEditors() {
        attachFloppyEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
        attachIsoEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
        runAsStatelessEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
        runAndPauseEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
        useAlternateCredentialsEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
    }

    void initRadioButtonEditors() {
        displayConsoleVncEditor = new EntityModelRadioButtonEditor("1"); //$NON-NLS-1$
        displayConsoleSpiceEditor = new EntityModelRadioButtonEditor("1"); //$NON-NLS-1$
    }

    void initComboBox() {
        sysPrepDomainNameListBoxEditor = new ListModelListBoxEditor<Object>();
        sysPrepDomainNameTextBoxEditor = new EntityModelTextBoxEditor();

        sysPrepDomainNameComboBox = new ComboBox(sysPrepDomainNameListBoxEditor, sysPrepDomainNameTextBoxEditor);
    }

    void initBootSequenceBox() {
        bootSequenceBox = new ListBox(false);
        bootSequenceBox.setWidth("370px"); //$NON-NLS-1$
        bootSequenceBox.setHeight("60px"); //$NON-NLS-1$

        VerticalPanel boxPanel = new VerticalPanel();
        boxPanel.setWidth("100%"); //$NON-NLS-1$
        boxPanel.add(bootSequenceBox);
        bootSequencePanel.add(boxPanel);

        localizeBootSequenceButtons();
    }

    void addStyles() {
        linuxBootOptionsPanel.setVisible(false);
        initialRunPanel.setVisible(true);
        attachFloppyEditor.addContentWidgetStyleName(style.attachImageCheckBoxLabel());
        attachIsoEditor.addContentWidgetStyleName(style.attachImageCheckBoxLabel());
        floppyImageEditor.addLabelStyleName(style.attachImageSelectBoxLabel());
        isoImageEditor.addLabelStyleName(style.attachImageSelectBoxLabel());
        floppyImageEditor.addContentWidgetStyleName(style.attachImageSelectbox());
        isoImageEditor.addContentWidgetStyleName(style.attachImageSelectbox());
    }

    @Override
    public void edit(final RunOnceModel object) {
        Driver.driver.edit(object);

        object.getCustomPropertySheet().getKeyValueLines().getItemsChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                customPropertiesSheetEditor.edit(object.getCustomPropertySheet());
            }
        });

        // Update Linux options panel
        final EntityModel isLinuxOptionsAvailable = object.getIsLinuxOptionsAvailable();
        object.getIsLinuxOptionsAvailable().getEntityChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                boolean toShow = (Boolean) isLinuxOptionsAvailable.getEntity();
                linuxBootOptionsPanel.setVisible(toShow);
            }
        });

        // Update BootSequence ListBox
        bootSequenceModel = object.getBootSequence();
        UpdateBootSequenceListBox();
    }

    @UiHandler("bootSequenceUpButton")
    void handleBootSequenceUpButtonClick(ClickEvent event) {
        if (bootSequenceModel != null) {
            bootSequenceModel.ExecuteCommand(bootSequenceModel.getMoveItemUpCommand());
        }
    }

    @UiHandler("bootSequenceDownButton")
    void handleBootSequenceDownButtonClick(ClickEvent event) {
        if (bootSequenceModel != null) {
            bootSequenceModel.ExecuteCommand(bootSequenceModel.getMoveItemDownCommand());
        }
    }

    private void UpdateBootSequenceListBox() {
        // Update Items
        updateBootSequenceItems();

        // Items change handling
        bootSequenceModel.getItems().getCollectionChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                updateBootSequenceItems();

                // Update selected item
                bootSequenceBox.setSelectedIndex(bootSequenceModel.getSelectedItemIndex());
            }
        });

        // Attach CD change handling
        bootSequenceModel.getCdromOption().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                boolean isEnabled = bootSequenceModel.getCdromOption().getIsChangable();
                String itemName = bootSequenceModel.getCdromOption().getTitle();
                updateItemAvailability(itemName, isEnabled);
            }
        });

        // NIC change handling
        bootSequenceModel.getNetworkOption().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                boolean isEnabled = bootSequenceModel.getNetworkOption().getIsChangable();
                String itemName = bootSequenceModel.getNetworkOption().getTitle();
                updateItemAvailability(itemName, isEnabled);
            }
        });

        // Change boot option handling
        bootSequenceBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                int selectedIndex = bootSequenceBox.getSelectedIndex();
                bootSequenceModel.setSelectedItem(bootSequenceModel.getItems().get(selectedIndex));

                bootSequenceUpButton.setEnabled(bootSequenceModel.getMoveItemUpCommand().getIsExecutionAllowed());
                bootSequenceDownButton.setEnabled(bootSequenceModel.getMoveItemDownCommand().getIsExecutionAllowed());

                // the setEnabled resets the label for some reason, so need to set it back
                localizeBootSequenceButtons();
            }

        });
    }

    protected void localizeBootSequenceButtons() {
        bootSequenceUpButton.setText(constants.bootSequenceUpButtonLabel());
        bootSequenceDownButton.setText(constants.bootSequenceDownButtonLabel());
    }

    private void updateBootSequenceItems() {
        // Update list box
        bootSequenceBox.clear();
        bootSequenceBox.setVisibleItemCount(bootSequenceModel.getItems().size());

        // Set items
        for (EntityModel bootItem : bootSequenceModel.getItems()) {
            bootSequenceBox.addItem(bootItem.getTitle());
            updateItemAvailability(bootItem.getTitle(), bootItem.getIsChangable());
        }
    }

    private void updateItemAvailability(String itemName, boolean isEnabled) {
        for (int i = 0; i < bootSequenceBox.getItemCount(); i++) {
            if (bootSequenceBox.getItemText(i).equals(itemName)) {
                NodeList<Element> options = bootSequenceBox.getElement().getElementsByTagName("option"); //$NON-NLS-1$
                if (!isEnabled) {
                    options.getItem(i).setAttribute("disabled", "disabled"); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    options.getItem(i).removeAttribute("disabled"); //$NON-NLS-1$
                }
            }
        }
    }

    @Override
    public RunOnceModel flush() {
        return Driver.driver.flush();
    }

}
