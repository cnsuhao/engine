package org.ovirt.engine.ui.webadmin.section.main.view.tab;

import com.google.gwt.i18n.client.NumberFormat;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.QuotaStorage;
import org.ovirt.engine.core.common.businessentities.QuotaVdsGroup;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.widget.renderer.DiskSizeRenderer;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.quota.QuotaListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.MainTabQuotaPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractMainTabWithDetailsTableView;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.table.column.QuotaDcStatusColumn;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import org.ovirt.engine.ui.webadmin.widget.table.column.QuotaPercentColumn;


public class MainTabQuotaView extends AbstractMainTabWithDetailsTableView<Quota, QuotaListModel> implements MainTabQuotaPresenter.ViewDef {

    private static final NumberFormat decimalFormat = NumberFormat.getDecimalFormat();
    private static final DiskSizeRenderer<Number> diskSizeRenderer =
            new DiskSizeRenderer<Number>(DiskSizeRenderer.DiskSizeUnit.GIGABYTE);

    interface ViewIdHandler extends ElementIdHandler<MainTabQuotaView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public MainTabQuotaView(MainModelProvider<Quota, QuotaListModel> modelProvider, ApplicationConstants constants) {
        super(modelProvider);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable(constants);
        initWidget(getTable());
    }

    void initTable(final ApplicationConstants constants) {
        getTable().enableColumnResizing();

        getTable().addColumn(new QuotaDcStatusColumn(), constants.empty(), "30px"); //$NON-NLS-1$

        getTable().addColumn(new TextColumnWithTooltip<Quota>() {
            @Override
            public String getValue(Quota object) {
                return object.getQuotaName() == null ? "" : object.getQuotaName(); //$NON-NLS-1$
            }
        }, constants.nameQuota(), "120px"); //$NON-NLS-1$

        getTable().addColumn(new TextColumnWithTooltip<Quota>() {
            @Override
            public String getValue(Quota object) {
                return object.getDescription() == null ? "" : object.getDescription(); //$NON-NLS-1$
            }
        }, constants.descriptionQuota(), "120px"); //$NON-NLS-1$

        getTable().addColumn(new QuotaPercentColumn<Quota>() {
            @Override
            protected Integer getProgressValue(Quota object) {
                int value;
                long allocated = 0;
                long used = 0;
                if (object.getGlobalQuotaVdsGroup() != null) {
                    allocated = object.getGlobalQuotaVdsGroup().getMemSizeMB();
                    used = object.getGlobalQuotaVdsGroup().getMemSizeMBUsage();
                } else {
                    for (QuotaVdsGroup quotaVdsGroup : object.getQuotaVdsGroups()) {
                        allocated += quotaVdsGroup.getMemSizeMB();
                        used += quotaVdsGroup.getMemSizeMBUsage();
                    }
                }
                if (allocated == 0) {
                    return 0;
                }
                value = (int)(((double)used/allocated) * 100);
                return allocated < 0 ? -1 : value > 100 ? 100 : value;
            }

            @Override
            public ApplicationConstants getaApplicationConstants() {
                return constants;
            }
        },
        constants.usedMemoryQuota(), "100px"); //$NON-NLS-1$

        getTable().addColumn(new TextColumnWithTooltip<Quota>() {
            @Override
            public String getValue(Quota object) {
                int value;
                long allocated = 0;
                long used = 0;
                if (object.getGlobalQuotaVdsGroup() != null) {
                    allocated = object.getGlobalQuotaVdsGroup().getMemSizeMB();
                    used = object.getGlobalQuotaVdsGroup().getMemSizeMBUsage();
                } else {
                    for (QuotaVdsGroup quotaVdsGroup : object.getQuotaVdsGroups()) {
                        allocated += quotaVdsGroup.getMemSizeMB();
                        used += quotaVdsGroup.getMemSizeMBUsage();
                    }
                }
                value = (int)(allocated-used);
                String returnVal;
                if (allocated < 0) {
                    returnVal = constants.unlimited();
                } else if (value <= 0){
                    returnVal = "0 MB"; //$NON-NLS-1$
                } else if (value <= 5*1024) {
                    returnVal = value + "MB"; //$NON-NLS-1$
                } else {
                    returnVal = decimalFormat.format((double)value/1024) + "GB"; //$NON-NLS-1$
                }
                return returnVal;
            }
        },constants.freeMemory(), "80px"); //$NON-NLS-1$

        getTable().addColumn(new QuotaPercentColumn<Quota>() {
            @Override
            protected Integer getProgressValue(Quota object) {
                int value;
                int allocated = 0;
                int used = 0;
                if (object.getGlobalQuotaVdsGroup() != null) {
                    allocated = object.getGlobalQuotaVdsGroup().getVirtualCpu();
                    used = object.getGlobalQuotaVdsGroup().getVirtualCpuUsage();
                } else {
                    for (QuotaVdsGroup quotaVdsGroup : object.getQuotaVdsGroups()) {
                        allocated += quotaVdsGroup.getVirtualCpu();
                        used += quotaVdsGroup.getVirtualCpuUsage();
                    }
                }
                if (allocated == 0) {
                    return 0;
                }
                value = (int)(((double)used/allocated) * 100);
                return allocated < 0 ? -1 : value > 100 ? 100 : value;
            }

            @Override
            public ApplicationConstants getaApplicationConstants() {
                return constants;
            }
        },
        constants.runningCpuQuota(), "100px"); //$NON-NLS-1$

        getTable().addColumn(new TextColumnWithTooltip<Quota>() {
            @Override
            public String getValue(Quota object) {
                int value;
                int allocated = 0;
                int used = 0;
                if (object.getGlobalQuotaVdsGroup() != null) {
                    allocated = object.getGlobalQuotaVdsGroup().getVirtualCpu();
                    used = object.getGlobalQuotaVdsGroup().getVirtualCpuUsage();
                } else {
                    for (QuotaVdsGroup quotaVdsGroup : object.getQuotaVdsGroups()) {
                        allocated += quotaVdsGroup.getVirtualCpu();
                        used += quotaVdsGroup.getVirtualCpuUsage();
                    }
                }
                value = allocated - used;

                String returnVal;
                if (allocated < 0) {
                    returnVal = constants.unlimited();
                } else if (value <= 0) {
                    returnVal = "0"; //$NON-NLS-1$
                } else {
                    returnVal = value + ""; //$NON-NLS-1$
                }
                return returnVal;
            }
        },constants.freeVcpu(), "80px"); //$NON-NLS-1$

        getTable().addColumn(new QuotaPercentColumn<Quota>() {
            @Override
            protected Integer getProgressValue(Quota object) {
                int value;
                double allocated = 0;
                double used = 0;
                if (object.getGlobalQuotaStorage() != null) {
                    allocated = object.getGlobalQuotaStorage().getStorageSizeGB();
                    used = object.getGlobalQuotaStorage().getStorageSizeGBUsage();
                } else {
                    for (QuotaStorage quotaStorage : object.getQuotaStorages()) {
                        allocated += quotaStorage.getStorageSizeGB();
                        used += quotaStorage.getStorageSizeGBUsage();
                    }
                }
                if (allocated == 0) {
                    return 0;
                }
                value = (int)((used/allocated) * 100);
                return allocated < 0 ? -1 : value > 100 ? 100 : value;
            }

            @Override
            public ApplicationConstants getaApplicationConstants() {
                return constants;
            }
        },
        constants.usedStorageQuota(), "100px"); //$NON-NLS-1$

        getTable().addColumn(new TextColumnWithTooltip<Quota>() {
            @Override
            public String getValue(Quota object) {
                double value;
                double allocated = 0;
                double used = 0;
                if (object.getGlobalQuotaStorage() != null) {
                    allocated = object.getGlobalQuotaStorage().getStorageSizeGB();
                    used = object.getGlobalQuotaStorage().getStorageSizeGBUsage();
                } else {
                    for (QuotaStorage quotaStorage : object.getQuotaStorages()) {
                        allocated += quotaStorage.getStorageSizeGB();
                        used += quotaStorage.getStorageSizeGBUsage();
                    }
                }
                value = allocated - used;

                String returnVal;
                if (allocated < 0) {
                    returnVal = constants.unlimited();
                } else if (value <= 0) {
                    returnVal = "0 GB"; //$NON-NLS-1$
                } else {
                    returnVal = diskSizeRenderer.render(value);
                }
                return returnVal;
            }
        },constants.freeStorage(),"80px"); //$NON-NLS-1$

        getTable().addActionButton(new WebAdminButtonDefinition<Quota>(constants.addQuota()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getCreateQuotaCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<Quota>(constants.editQuota()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getEditQuotaCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<Quota>(constants.copyQuota()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getCloneQuotaCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<Quota>(constants.removeQuota()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getRemoveQuotaCommand();
            }
        });

    }
}
