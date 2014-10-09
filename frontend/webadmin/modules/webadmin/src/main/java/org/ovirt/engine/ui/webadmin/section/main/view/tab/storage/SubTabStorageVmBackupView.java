package org.ovirt.engine.ui.webadmin.section.main.view.tab.storage;

import java.util.ArrayList;
import java.util.Date;

import org.ovirt.engine.core.common.businessentities.OriginType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.ui.common.SubTableResources;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.table.ActionCellTable;
import org.ovirt.engine.ui.common.widget.table.column.EnumColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.storage.StorageListModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.VmBackupModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.storage.SubTabStorageVmBackupPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractSubTabTableView;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.table.column.GeneralDateTimeColumn;
import org.ovirt.engine.ui.webadmin.widget.table.column.VmStatusColumn;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable.Resources;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AbstractDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.inject.Inject;

public class SubTabStorageVmBackupView extends AbstractSubTabTableView<storage_domains, VM, StorageListModel, VmBackupModel>
        implements SubTabStorageVmBackupPresenter.ViewDef {

    interface ViewUiBinder extends UiBinder<Widget, SubTabStorageVmBackupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField
    HorizontalPanel mainContainer;

    @UiField
    SimplePanel vmTableContainer;

    @UiField
    SimplePanel applicationsTableContainer;

    ActionCellTable<String> applicationsTable;

    @Inject
    public SubTabStorageVmBackupView(SearchableDetailModelProvider<VM, StorageListModel, VmBackupModel> modelProvider, ApplicationConstants constants) {
        super(modelProvider);
        initVmTable(constants);
        initApplicationsTable(constants);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));

        vmTableContainer.add(getTable());
        applicationsTableContainer.add(applicationsTable);

        mainContainer.setCellWidth(vmTableContainer, "50%"); //$NON-NLS-1$
        mainContainer.setCellWidth(applicationsTableContainer, "50%"); //$NON-NLS-1$
    }

    void initVmTable(ApplicationConstants constants) {

        getTable().addColumn(new VmStatusColumn(), constants.empty(), "30px"); //$NON-NLS-1$

        TextColumnWithTooltip<VM> nameColumn = new TextColumnWithTooltip<VM>() {
            @Override
            public String getValue(VM object) {
                return object.getVmName();
            }
        };
        getTable().addColumn(nameColumn, constants.nameVm());

        TextColumnWithTooltip<VM> templateColumn = new TextColumnWithTooltip<VM>() {
            @Override
            public String getValue(VM object) {
                return object.getVmtName();
            }
        };
        getTable().addColumn(templateColumn, constants.templateVm());

        TextColumnWithTooltip<VM> originColumn = new EnumColumn<VM, OriginType>() {
            @Override
            protected OriginType getRawValue(VM object) {
                return object.getOrigin();
            }
        };
        getTable().addColumn(originColumn, constants.originVm());

        TextColumnWithTooltip<VM> memoryColumn = new TextColumnWithTooltip<VM>() {
            @Override
            public String getValue(VM object) {
                return String.valueOf(object.getVmMemSizeMb()) + " MB"; //$NON-NLS-1$
            }
        };
        getTable().addColumn(memoryColumn, constants.memoryVm());

        TextColumnWithTooltip<VM> cpuColumn = new TextColumnWithTooltip<VM>() {
            @Override
            public String getValue(VM object) {
                return String.valueOf(object.getNumOfCpus());
            }
        };
        getTable().addColumn(cpuColumn, constants.cpusVm());

        TextColumnWithTooltip<VM> diskColumn = new TextColumnWithTooltip<VM>() {
            @Override
            public String getValue(VM object) {
                return String.valueOf(object.getDiskMap().size());
            }
        };
        getTable().addColumn(diskColumn, constants.disksVm());

        TextColumnWithTooltip<VM> creationDateColumn = new GeneralDateTimeColumn<VM>() {
            @Override
            protected Date getRawValue(VM object) {
                return object.getVmCreationDate();
            }
        };
        getTable().addColumn(creationDateColumn, constants.creationDateVm());

        TextColumnWithTooltip<VM> exportDateColumn = new GeneralDateTimeColumn<VM>() {
            @Override
            protected Date getRawValue(VM object) {
                return object.getExportDate();
            }
        };
        getTable().addColumn(exportDateColumn, constants.exportDateVm());

        getTable().addActionButton(new WebAdminButtonDefinition<VM>(constants.restoreVm()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getRestoreCommand();
            }
        });

        getTable().addActionButton(new WebAdminButtonDefinition<VM>(constants.removeVm()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getRemoveCommand();
            }
        });
    }

    private void initApplicationsTable(ApplicationConstants constants) {
        applicationsTable = new ActionCellTable<String>(new AbstractDataProvider<String>() {
            @Override
            protected void onRangeChanged(HasData<String> display) {
            }
        }, GWT.<Resources> create(SubTableResources.class));

        TextColumnWithTooltip<String> nameColumn = new TextColumnWithTooltip<String>() {
            @Override
            public String getValue(String object) {
                return object;
            }
        };

        applicationsTable.addColumn(nameColumn, constants.installedAppsVm());
        applicationsTable.setWidth("100%"); //$NON-NLS-1$
        applicationsTable.setRowData(new ArrayList<String>());

        getDetailModel().getPropertyChangedEvent().addListener(new IEventListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if (getDetailModel().getAppListModel().getItems() != null) {
                    applicationsTable.setRowData(Linq.ToList(getDetailModel().getAppListModel().getItems()));
                } else {
                    applicationsTable.setRowData(new ArrayList<String>());
                }
            }
        });
    }

}
