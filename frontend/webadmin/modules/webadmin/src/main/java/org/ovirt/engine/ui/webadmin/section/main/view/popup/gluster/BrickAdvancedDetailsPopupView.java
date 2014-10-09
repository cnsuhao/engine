package org.ovirt.engine.ui.webadmin.section.main.view.popup.gluster;

import org.ovirt.engine.core.common.businessentities.gluster.GlusterClientInfo;
import org.ovirt.engine.core.common.businessentities.gluster.Mempool;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTab;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.editor.EntityModelLabelEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextAreaLabelEditor;
import org.ovirt.engine.ui.common.widget.parser.EntityModelParser;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.common.widget.table.column.EntityModelTextColumn;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.gluster.BrickAdvancedDetailsModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.gluster.BrickAdvancedDetailsPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class BrickAdvancedDetailsPopupView extends AbstractModelBoundPopupView<BrickAdvancedDetailsModel> implements BrickAdvancedDetailsPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<BrickAdvancedDetailsModel, BrickAdvancedDetailsPopupView> {
        Driver driver = GWT.create(Driver.class);
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, BrickAdvancedDetailsPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<BrickAdvancedDetailsPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField
    WidgetStyle style;

    @UiField
    @WithElementId
    DialogTab generalTab;

    @UiField
    @Path(value = "brick.entity")
    @WithElementId
    EntityModelLabelEditor brickEditor;

    @UiField(provided = true)
    @Path(value = "brickProperties.status.entity")
    @WithElementId
    EntityModelLabelEditor statusEditor;

    @UiField
    @Path(value = "brickProperties.port.entity")
    @WithElementId
    EntityModelLabelEditor portEditor;

    @UiField
    @Path(value = "brickProperties.pid.entity")
    @WithElementId
    EntityModelLabelEditor pidEditor;

    @UiField
    @Path(value = "brickProperties.totalSize.entity")
    @WithElementId
    EntityModelLabelEditor totalSizeEditor;

    @UiField
    @Path(value = "brickProperties.freeSize.entity")
    @WithElementId
    EntityModelLabelEditor freeSizeEditor;

    @UiField
    @Path(value = "brickProperties.device.entity")
    @WithElementId
    EntityModelLabelEditor deviceEditor;

    @UiField
    @Path(value = "brickProperties.blockSize.entity")
    @WithElementId
    EntityModelLabelEditor blockSizeEditor;

    @UiField
    @Path(value = "brickProperties.mountOptions.entity")
    @WithElementId
    EntityModelTextAreaLabelEditor mountOptionsEditor;

    @UiField
    @Path(value = "brickProperties.fileSystem.entity")
    @WithElementId
    EntityModelLabelEditor fileSystemEditor;

    @UiField
    @WithElementId
    DialogTab clientsTab;

    @UiField(provided = true)
    @Ignore
    @WithElementId
    EntityModelCellTable<ListModel> clientsTable;

    @UiField
    @WithElementId
    DialogTab memoryStatsTab;

    @UiField
    @Path(value = "memoryStatistics.totalAllocated.entity")
    @WithElementId
    EntityModelLabelEditor totalAllocatedEditor;

    @UiField
    @Path(value = "memoryStatistics.freeBlocks.entity")
    @WithElementId
    EntityModelLabelEditor freeBlocksEditor;

    @UiField
    @Path(value = "memoryStatistics.freeFastbin.entity")
    @WithElementId
    EntityModelLabelEditor freeFastbinBlocksEditor;

    @UiField
    @Path(value = "memoryStatistics.mmappedBlocks.entity")
    @WithElementId
    EntityModelLabelEditor mmappedBlocksEditor;

    @UiField
    @Path(value = "memoryStatistics.spaceAllocatedMmapped.entity")
    @WithElementId
    EntityModelLabelEditor spaceAllocatedMmappedEditor;

    @UiField
    @Path(value = "memoryStatistics.maxTotalAllocated.entity")
    @WithElementId
    EntityModelLabelEditor maxTotalAllocatedEditor;

    @UiField
    @Path(value = "memoryStatistics.spaceFreedFastbin.entity")
    @WithElementId
    EntityModelLabelEditor spaceFreedFastbinEditor;

    @UiField
    @Path(value = "memoryStatistics.totalAllocatedSpace.entity")
    @WithElementId
    EntityModelLabelEditor totalAllocatedSpaceEditor;

    @UiField
    @Path(value = "memoryStatistics.totalFreeSpace.entity")
    @WithElementId
    EntityModelLabelEditor totalFreeSpaceEditor;

    @UiField
    @Path(value = "memoryStatistics.releasableFreeSpace.entity")
    @WithElementId
    EntityModelLabelEditor releasableFreeSpaceEditor;

    @UiField
    @WithElementId
    DialogTab memoryPoolsTab;

    @UiField(provided = true)
    @Ignore
    @WithElementId
    EntityModelCellTable<ListModel> memoryPoolsTable;

    @UiField
    @Ignore
    Label messageLabel;

    @Inject
    public BrickAdvancedDetailsPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources);
        initEditors();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        addStyles();
        initTableColumns(constants);
        localize(constants);
        Driver.driver.initialize(this);
    }

    private void initEditors() {
        statusEditor = new EntityModelLabelEditor(new EnumRenderer(), new EntityModelParser());
        clientsTable = new EntityModelCellTable<ListModel>(false, true);
        memoryPoolsTable = new EntityModelCellTable<ListModel>(false, true);
    }

    private void addStyles() {
        brickEditor.addContentWidgetStyleName(style.generalValue());
        statusEditor.addContentWidgetStyleName(style.generalValue());
        portEditor.addContentWidgetStyleName(style.generalValue());
        pidEditor.addContentWidgetStyleName(style.generalValue());
        totalSizeEditor.addContentWidgetStyleName(style.generalValue());
        freeSizeEditor.addContentWidgetStyleName(style.generalValue());
        deviceEditor.addContentWidgetStyleName(style.generalValue());
        blockSizeEditor.addContentWidgetStyleName(style.generalValue());
        mountOptionsEditor.addContentWidgetStyleName(style.generalValue());
        fileSystemEditor.addContentWidgetStyleName(style.generalValue());

        totalAllocatedEditor.addLabelStyleName(style.memStatLabel());
        freeBlocksEditor.addLabelStyleName(style.memStatLabel());
        freeFastbinBlocksEditor.addLabelStyleName(style.memStatLabel());
        mmappedBlocksEditor.addLabelStyleName(style.memStatLabel());
        spaceAllocatedMmappedEditor.addLabelStyleName(style.memStatLabel());
        maxTotalAllocatedEditor.addLabelStyleName(style.memStatLabel());
        spaceFreedFastbinEditor.addLabelStyleName(style.memStatLabel());
        totalAllocatedSpaceEditor.addLabelStyleName(style.memStatLabel());
        totalFreeSpaceEditor.addLabelStyleName(style.memStatLabel());
        releasableFreeSpaceEditor.addLabelStyleName(style.memStatLabel());

        totalAllocatedEditor.addContentWidgetStyleName(style.memStatValue());
        freeBlocksEditor.addContentWidgetStyleName(style.memStatValue());
        freeFastbinBlocksEditor.addContentWidgetStyleName(style.memStatValue());
        mmappedBlocksEditor.addContentWidgetStyleName(style.memStatValue());
        spaceAllocatedMmappedEditor.addContentWidgetStyleName(style.memStatValue());
        maxTotalAllocatedEditor.addContentWidgetStyleName(style.memStatValue());
        spaceFreedFastbinEditor.addContentWidgetStyleName(style.memStatValue());
        totalAllocatedSpaceEditor.addContentWidgetStyleName(style.memStatValue());
        totalFreeSpaceEditor.addContentWidgetStyleName(style.memStatValue());
        releasableFreeSpaceEditor.addContentWidgetStyleName(style.memStatValue());
    }

    private void initTableColumns(ApplicationConstants constants) {
        clientsTable.addEntityModelColumn(new EntityModelTextColumn<GlusterClientInfo>() {
            @Override
            public String getText(GlusterClientInfo entity) {
                return entity.getHostname();
            }
        }, constants.clientBrickAdvancedLabel());

        clientsTable.addEntityModelColumn(new EntityModelTextColumn<GlusterClientInfo>() {
            @Override
            public String getText(GlusterClientInfo entity) {
                return String.valueOf(entity.getBytesRead());
            }
        }, constants.bytesReadBrickAdvancedLabel());

        clientsTable.addEntityModelColumn(new EntityModelTextColumn<GlusterClientInfo>() {
            @Override
            public String getText(GlusterClientInfo entity) {
                return String.valueOf(entity.getBytesWritten());
            }
        }, constants.bytesWrittenBrickAdvancedLabel());

        memoryPoolsTable.addEntityModelColumn(new EntityModelTextColumn<Mempool>() {
            @Override
            public String getText(Mempool entity) {
                return entity.getName();
            }
        }, constants.nameBrickAdvancedLabel());

        memoryPoolsTable.addEntityModelColumn(new EntityModelTextColumn<Mempool>() {
            @Override
            public String getText(Mempool entity) {
                return String.valueOf(entity.getHotCount());
            }
        }, constants.hotCountBrickAdvancedLabel());

        memoryPoolsTable.addEntityModelColumn(new EntityModelTextColumn<Mempool>() {
            @Override
            public String getText(Mempool entity) {
                return String.valueOf(entity.getColdCount());
            }
        }, constants.coldCountBrickAdvancedLabel());

        memoryPoolsTable.addEntityModelColumn(new EntityModelTextColumn<Mempool>() {
            @Override
            public String getText(Mempool entity) {
                return String.valueOf(entity.getPadddedSize());
            }
        }, constants.paddedSizeBrickAdvancedLabel());

        memoryPoolsTable.addEntityModelColumn(new EntityModelTextColumn<Mempool>() {
            @Override
            public String getText(Mempool entity) {
                return String.valueOf(entity.getAllocCount());
            }
        }, constants.allocatedCountBrickAdvancedLabel());

        memoryPoolsTable.addEntityModelColumn(new EntityModelTextColumn<Mempool>() {
            @Override
            public String getText(Mempool entity) {
                return String.valueOf(entity.getMaxAlloc());
            }
        }, constants.maxAllocatedBrickAdvancedLabel());

        memoryPoolsTable.addEntityModelColumn(new EntityModelTextColumn<Mempool>() {
            @Override
            public String getText(Mempool entity) {
                return String.valueOf(entity.getPoolMisses());
            }
        }, constants.poolMissesBrickAdvancedLabel());

        memoryPoolsTable.addEntityModelColumn(new EntityModelTextColumn<Mempool>() {
            @Override
            public String getText(Mempool entity) {
                return String.valueOf(entity.getMaxStdAlloc());
            }
        }, constants.maxStdAllocatedBrickAdvancedLabel());
    }

    private void localize(ApplicationConstants constants) {
        generalTab.setLabel(constants.generalBrickAdvancedPopupLabel());
        brickEditor.setLabel(constants.brickAdvancedLabel());
        statusEditor.setLabel(constants.statusBrickAdvancedLabel());
        portEditor.setLabel(constants.portBrickAdvancedLabel());
        pidEditor.setLabel(constants.pidBrickAdvancedLabel());
        totalSizeEditor.setLabel(constants.totalSizeBrickAdvancedLabel());
        freeSizeEditor.setLabel(constants.freeSizeBrickAdvancedLabel());
        deviceEditor.setLabel(constants.deviceBrickAdvancedLabel());
        blockSizeEditor.setLabel(constants.blockSizeBrickAdvancedLabel());
        mountOptionsEditor.setLabel(constants.mountOptionsBrickAdvancedLabel());
        fileSystemEditor.setLabel(constants.fileSystemBrickAdvancedLabel());

        clientsTab.setLabel(constants.clientsBrickAdvancedPopupLabel());

        memoryStatsTab.setLabel(constants.memoryStatsBrickAdvancedPopupLabel());
        totalAllocatedEditor.setLabel(constants.totalAllocatedBrickAdvancedLabel());
        freeBlocksEditor.setLabel(constants.freeBlocksBrickAdvancedLabel());
        freeFastbinBlocksEditor.setLabel(constants.freeFastbinBlocksBrickAdvancedLabel());
        mmappedBlocksEditor.setLabel(constants.mmappedBlocksBrickAdvancedLabel());
        spaceAllocatedMmappedEditor.setLabel(constants.allocatedInMmappedBlocksBrickAdvancedLabel());
        maxTotalAllocatedEditor.setLabel(constants.maxTotalAllocatedSpaceBrickAdvancedLabel());
        spaceFreedFastbinEditor.setLabel(constants.spaceInFreedFasbinBlocksBrickAdvancedLabel());
        totalAllocatedSpaceEditor.setLabel(constants.totalAllocatedSpaceBrickAdvancedLabel());
        totalFreeSpaceEditor.setLabel(constants.totalFreeSpaceBrickAdvancedLabel());
        releasableFreeSpaceEditor.setLabel(constants.releasableFreeSpaceBrickAdvancedLabel());

        memoryPoolsTab.setLabel(constants.memoryPoolsBrickAdvancedPopupLabel());
    }

    @Override
    public void edit(BrickAdvancedDetailsModel object) {
        Driver.driver.edit(object);
        clientsTable.edit(object.getClients());
        memoryPoolsTable.edit(object.getMemoryPools());
    }

    @Override
    public BrickAdvancedDetailsModel flush() {
        return Driver.driver.flush();
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(message);
        messageLabel.setText(message);
    }

    interface WidgetStyle extends CssResource {
        String memStatLabel();

        String memStatValue();

        String generalValue();
    }
}
