package org.ovirt.engine.ui.common.widget.uicommon.storage;

import java.util.List;

import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.widget.HasValidation;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTab;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTabPanel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.IscsiStorageModel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class IscsiStorageView extends AbstractStorageView<IscsiStorageModel> implements HasValidation {

    interface Driver extends SimpleBeanEditorDriver<IscsiStorageModel, IscsiStorageView> {
        Driver driver = GWT.create(Driver.class);
    }

    interface ViewUiBinder extends UiBinder<Widget, IscsiStorageView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField
    WidgetStyle style;

    @UiField
    @Path(value = "GetLUNsFailure")
    Label message;

    @UiField
    @Path(value = "selectedLunWarning")
    Label warning;

    @UiField
    DialogTab lunToTargetsTab;

    @UiField
    DialogTab targetsToLunTab;

    @UiField
    DialogTabPanel dialogTabPanel;

    @UiField
    SimplePanel lunsListPanel;

    @UiField
    SimplePanel targetsToLunsPanel;

    @UiField
    FlowPanel targetsToLunsTabContentPanel;

    @UiField
    FlowPanel lunsToTargetsTabContentPanel;

    @Ignore
    IscsiTargetToLunView iscsiTargetToLunView;

    @Ignore
    IscsiLunToTargetView iscsiLunToTargetView;

    double treeCollapsedHeight = 208, treeExpandedHeight = 307, lunsTreeHeight = 345;
    double tabPanelHeight = 368, tabContentHeight = 340, tabHeight = 175;
    double textTop = 80, textLeft = -84, textWidth = 100;

    protected static final CommonApplicationConstants constants = GWT.create(CommonApplicationConstants.class);

    public IscsiStorageView(boolean multiSelection) {
        this.multiSelection = multiSelection;
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        localize(constants);
        addStyles();
        Driver.driver.initialize(this);
    }

    public IscsiStorageView(boolean multiSelection,
            double treeCollapsedHeight, double treeExpandedHeight, double lunsTreeHeight,
            double tabPanelHeight, double tabContentHeight, double tabHeight,
            double textTop, double textLeft) {
        this(multiSelection);

        this.treeCollapsedHeight = treeCollapsedHeight;
        this.treeExpandedHeight = treeExpandedHeight;
        this.lunsTreeHeight = lunsTreeHeight;
        this.tabPanelHeight = tabPanelHeight;
        this.tabContentHeight = tabContentHeight;
        this.tabHeight = tabHeight;
        this.textTop = textTop;
        this.textLeft = textLeft;
    }

    void addStyles() {
        dialogTabPanel.addBarStyle(style.bar());
        lunToTargetsTab.setTabLabelStyle(style.dialogTab());
        targetsToLunTab.setTabLabelStyle(style.dialogTab());
    }

    void localize(CommonApplicationConstants constants) {
        lunToTargetsTab.setLabel(constants.storageIscsiPopupLunToTargetsTabLabel());
        targetsToLunTab.setLabel(constants.storageIscsiPopupTargetsToLunTabLabel());
    }

    @Override
    public void edit(final IscsiStorageModel object) {
        Driver.driver.edit(object);

        initLists(object);

        // Add event handlers
        object.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                String propName = ((PropertyChangedEventArgs) args).PropertyName;
                if (propName.equals("IsValid")) { //$NON-NLS-1$
                    onIsValidPropertyChange(object);
                }
                else if (propName.equals("IsGrouppedByTarget")) { //$NON-NLS-1$
                    updateListByGropping(object);
                }
            }
        });

        // Edit sub-views
        iscsiTargetToLunView.edit(object);
        iscsiLunToTargetView.edit(object);

        // Add click handlers
        targetsToLunTab.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                iscsiLunToTargetView.disableItemsUpdate();
                object.setIsGrouppedByTarget(true);
            }
        });

        lunToTargetsTab.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                iscsiTargetToLunView.disableItemsUpdate();
                object.setIsGrouppedByTarget(false);
            }
        });

        // Update selected tab and list
        dialogTabPanel.switchTab(object.getIsGrouppedByTarget() ? targetsToLunTab : lunToTargetsTab);
        updateListByGropping(object);

        // Set tree style
        iscsiLunToTargetView.setTreeContainerStyleName(style.expandedlunsListPanel());
    }

    void initLists(IscsiStorageModel object) {
        // Create discover panel and storage lists
        iscsiTargetToLunView = new IscsiTargetToLunView(treeCollapsedHeight, treeExpandedHeight, false, multiSelection);
        iscsiLunToTargetView = new IscsiLunToTargetView(lunsTreeHeight, multiSelection);

        // Update Style
        dialogTabPanel.getElement().getStyle().setHeight(tabContentHeight, Unit.PX);
        updateStyle(targetsToLunsTabContentPanel, targetsToLunTab);
        updateStyle(lunsToTargetsTabContentPanel, lunToTargetsTab);

        // Add view widgets to panel
        lunsListPanel.add(iscsiLunToTargetView);
        targetsToLunsPanel.add(iscsiTargetToLunView);
    }

    void updateStyle(FlowPanel tabContentPanel, DialogTab dialogTab) {
        tabContentPanel.getElement().getStyle().setHeight(tabPanelHeight, Unit.PX);
        dialogTab.getElement().getStyle().setHeight(tabHeight, Unit.PX);
        dialogTab.getTabLabel().getElement().getStyle().setTop(textTop, Unit.PX);
        dialogTab.getTabLabel().getElement().getStyle().setLeft(textLeft, Unit.PX);
        dialogTab.getTabLabel().getElement().getStyle().setWidth(tabHeight, Unit.PX);
        dialogTab.getTabLabel().setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    }

    void updateListByGropping(IscsiStorageModel object) {
        // Update view by 'IsGrouppedByTarget' flag
        if (object.getIsGrouppedByTarget()) {
            iscsiTargetToLunView.activateItemsUpdate();
        }
        else {
            iscsiLunToTargetView.activateItemsUpdate();
        }

    }

    void onIsValidPropertyChange(EntityModel model) {
        if (model.getIsValid()) {
            markAsValid();
        } else {
            markAsInvalid(model.getInvalidityReasons());
        }
    }

    @Override
    public void markAsValid() {
        markValidation(false, null);
    }

    @Override
    public void markAsInvalid(List<String> validationHints) {
        markValidation(true, validationHints);
    }

    private void markValidation(boolean isValid, List<String> validationHints) {
        String oldStyle = isValid ? style.validTabContentPanel() : style.invalidTabContentPanel();
        String newStyle = isValid ? style.invalidTabContentPanel() : style.validTabContentPanel();

        targetsToLunsTabContentPanel.removeStyleName(oldStyle);
        lunsToTargetsTabContentPanel.removeStyleName(oldStyle);
        targetsToLunsTabContentPanel.addStyleName(newStyle);
        lunsToTargetsTabContentPanel.addStyleName(newStyle);

        targetsToLunsTabContentPanel.setTitle(getValidationTitle(validationHints));
        lunsToTargetsTabContentPanel.setTitle(getValidationTitle(validationHints));
    }

    private String getValidationTitle(List<String> validationHints) {
        return validationHints != null && validationHints.size() > 0 ? validationHints.get(0) : null;
    }

    @Override
    public boolean isSubViewFocused() {
        return iscsiTargetToLunView.isDiscoverPanelFocused();
    }

    @Override
    public IscsiStorageModel flush() {
        return Driver.driver.flush();
    }

    @Override
    public void focus() {
    }

    interface WidgetStyle extends CssResource {
        String bar();

        String dialogTab();

        String expandedlunsListPanel();

        String validTabContentPanel();

        String invalidTabContentPanel();
    }

}
