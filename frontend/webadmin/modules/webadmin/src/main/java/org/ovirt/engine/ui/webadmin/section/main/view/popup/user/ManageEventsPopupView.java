package org.ovirt.engine.ui.webadmin.section.main.view.popup.user;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.core.compat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextBoxEditor;
import org.ovirt.engine.ui.uicommonweb.models.common.SelectionTreeNodeModel;
import org.ovirt.engine.ui.uicommonweb.models.users.EventNotificationModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.user.ManageEventsPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.uicommon.model.ModelListTreeViewModel;
import org.ovirt.engine.ui.webadmin.uicommon.model.SimpleSelectionTreeNodeModel;
import org.ovirt.engine.ui.webadmin.widget.editor.EntityModelCellTree;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.cellview.client.TreeNode;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.inject.Inject;

public class ManageEventsPopupView extends AbstractModelBoundPopupView<EventNotificationModel>
        implements ManageEventsPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<EventNotificationModel, ManageEventsPopupView> {
        Driver driver = GWT.create(Driver.class);
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, ManageEventsPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<ManageEventsPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField(provided = true)
    @Ignore
    EntityModelCellTree<SelectionTreeNodeModel, SimpleSelectionTreeNodeModel> tree;

    @UiField
    @Path(value = "email.entity")
    @WithElementId
    EntityModelTextBoxEditor emailEditor;

    @UiField
    @Ignore
    Label titleLabel;

    @UiField
    @Ignore
    Button expandAllButton;

    @UiField
    @Ignore
    Button collapseAllButton;

    @Inject
    public ManageEventsPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources);
        initTree();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        initExpandButtons();
        ViewIdHandler.idHandler.generateAndSetIds(this);
        localize(constants);
        Driver.driver.initialize(this);
    }

    void localize(ApplicationConstants constants) {
        emailEditor.setLabel(constants.manageEventsPopupEmailLabel());
        titleLabel.setText(constants.manageEventsPopupTitleLabel());
        expandAllButton.setText(constants.treeExpandAll());
        collapseAllButton.setText(constants.treeCollapseAll());
    }

    private void initTree() {
        CellTree.Resources res = GWT.create(AssignTagTreeResources.class);
        tree = new EntityModelCellTree<SelectionTreeNodeModel, SimpleSelectionTreeNodeModel>(res);
    }

    private void initExpandButtons() {
        expandAllButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                expandTree();
            }
        });

        collapseAllButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                collapseTree();
            }
        });
    }

    @Override
    public void edit(EventNotificationModel object) {
        Driver.driver.edit(object);

        // Listen to Properties
        object.getPropertyChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                EventNotificationModel model = (EventNotificationModel) sender;
                String propertyName = ((PropertyChangedEventArgs) args).PropertyName;
                if ("EventGroupModels".equals(propertyName)) { //$NON-NLS-1$
                    updateTree(model);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void updateTree(EventNotificationModel model) {
        // Get tag node list
        ArrayList<SelectionTreeNodeModel> tagTreeNodes = model.getEventGroupModels();

        // Get tree view model
        ModelListTreeViewModel<SelectionTreeNodeModel, SimpleSelectionTreeNodeModel> modelListTreeViewModel =
                (ModelListTreeViewModel<SelectionTreeNodeModel, SimpleSelectionTreeNodeModel>) tree.getTreeViewModel();

        // Set root nodes
        List<SimpleSelectionTreeNodeModel> rootNodes = SimpleSelectionTreeNodeModel.fromList(tagTreeNodes);
        modelListTreeViewModel.setRoot(rootNodes);

        // Update tree data
        AsyncDataProvider<SimpleSelectionTreeNodeModel> asyncTreeDataProvider =
                modelListTreeViewModel.getAsyncTreeDataProvider();
        asyncTreeDataProvider.updateRowCount(rootNodes.size(), true);
        asyncTreeDataProvider.updateRowData(0, rootNodes);
    }

    private void expandTree() {
        if (tree != null) {
            expandTree(tree.getRootTreeNode(), true);
        }
    }

    private void collapseTree() {
        if (tree != null) {
            expandTree(tree.getRootTreeNode(), false);
        }
    }

    private void expandTree(TreeNode node, boolean collapse) {
        if (node == null) {
            return;
        }

        if (node.getChildCount() > 0) {
            for (int i = 0; i < node.getChildCount(); i++) {
                expandTree(node.setChildOpen(i, collapse), collapse);
            }
        }
    }

    @Override
    public EventNotificationModel flush() {
        return Driver.driver.flush();
    }

    interface AssignTagTreeResources extends CellTree.Resources {
        interface TableStyle extends CellTree.Style {
        }

        @Override
        @Source({ "org/ovirt/engine/ui/webadmin/css/AssignTagTree.css" })
        TableStyle cellTreeStyle();
    }

}
