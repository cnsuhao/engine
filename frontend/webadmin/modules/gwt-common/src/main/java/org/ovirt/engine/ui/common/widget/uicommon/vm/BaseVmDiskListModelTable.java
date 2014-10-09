package org.ovirt.engine.ui.common.widget.uicommon.vm;

import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.Disk.DiskStorageType;
import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.uicommon.model.SearchableTableModelProvider;
import org.ovirt.engine.ui.common.widget.uicommon.AbstractModelBoundTableWidget;
import org.ovirt.engine.ui.common.widget.uicommon.disks.DisksViewColumns;
import org.ovirt.engine.ui.common.widget.uicommon.disks.DisksViewRadioGroup;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmDiskListModelBase;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.RadioButton;

public class BaseVmDiskListModelTable<T extends VmDiskListModelBase> extends AbstractModelBoundTableWidget<Disk, T> {

    private CommonApplicationConstants constants;
    private DisksViewRadioGroup disksViewRadioGroup;

    public BaseVmDiskListModelTable(
            SearchableTableModelProvider<Disk, T> modelProvider,
            EventBus eventBus,
            ClientStorage clientStorage) {
        super(modelProvider, eventBus, clientStorage, false);

        disksViewRadioGroup = new DisksViewRadioGroup();
    }

    final ClickHandler clickHandler = new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
            if (((RadioButton) event.getSource()).getValue()) {
                handleRadioButtonClick(event);
            }
        }
    };

    void initTableOverhead() {
        disksViewRadioGroup.setClickHandler(clickHandler);
        getTable().setTableOverhead(disksViewRadioGroup);
        getTable().setTableTopMargin(20);
    }

    @Override
    public void initTable(CommonApplicationConstants constants) {
        this.constants = constants;

        initTableOverhead();
        handleRadioButtonClick(null);

        getModel().getItemsChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                disksViewRadioGroup.setDiskStorageType((DiskStorageType) getModel().getDiskViewType().getEntity());
            }
        });
    }

    void handleRadioButtonClick(ClickEvent event) {
        boolean all = disksViewRadioGroup.getAllButton().getValue();
        boolean images = disksViewRadioGroup.getImagesButton().getValue();
        boolean luns = disksViewRadioGroup.getLunsButton().getValue();

        getTable().getSelectionModel().clear();
        getModel().getDiskViewType().setEntity(disksViewRadioGroup.getDiskStorageType());
        getModel().setItems(null);
        getModel().Search();

        getTable().ensureColumnPresent(
                DisksViewColumns.diskStatusColumn, constants.empty(), all || images || luns, "30px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.aliasColumn, constants.aliasDisk(), all || images || luns);

        getTable().ensureColumnPresent(
                DisksViewColumns.bootableDiskColumn,
                DisksViewColumns.bootableDiskColumn.getHeaderHtml(), all || images || luns, "30px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.shareableDiskColumn,
                DisksViewColumns.shareableDiskColumn.getHeaderHtml(), all || images || luns, "30px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.lunDiskColumn,
                DisksViewColumns.lunDiskColumn.getHeaderHtml(), all, "30px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.sizeColumn, constants.provisionedSizeDisk(), all || images || luns, "100px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.actualSizeColumn, constants.sizeDisk(), images, "100px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.allocationColumn, constants.allocationDisk(), images, "100px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.storageDomainsColumn, constants.storageDomainDisk(), images, "120px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.dateCreatedColumn, constants.creationDateDisk(), images, "150px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.lunIdColumn, constants.lunIdSanStorage(), luns);

        getTable().ensureColumnPresent(
                DisksViewColumns.lunSerialColumn, constants.serialSanStorage(), luns);

        getTable().ensureColumnPresent(
                DisksViewColumns.lunVendorIdColumn, constants.vendorIdSanStorage(), luns);

        getTable().ensureColumnPresent(
                DisksViewColumns.lunProductIdColumn, constants.productIdSanStorage(), luns);

        getTable().ensureColumnPresent(
                DisksViewColumns.diskContainersColumn, constants.attachedToDisk(), all || images || luns, "120px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.interfaceColumn, constants.interfaceDisk(), all || images || luns, "100px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.statusColumn, constants.statusDisk(), images, "100px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.descriptionColumn, constants.descriptionDisk(), all || images || luns);

    }
}
