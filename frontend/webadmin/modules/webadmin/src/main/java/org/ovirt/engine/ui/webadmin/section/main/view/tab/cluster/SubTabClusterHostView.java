package org.ovirt.engine.ui.webadmin.section.main.view.tab.cluster;

import javax.inject.Inject;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.EnumColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.models.ApplicationModeHelper;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterHostListModel;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.cluster.SubTabClusterHostPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractSubTabTableView;
import org.ovirt.engine.ui.webadmin.widget.table.column.HostStatusColumn;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public class SubTabClusterHostView extends AbstractSubTabTableView<VDSGroup, VDS, ClusterListModel, ClusterHostListModel>
        implements SubTabClusterHostPresenter.ViewDef {

    @Inject
    public SubTabClusterHostView(SearchableDetailModelProvider<VDS, ClusterListModel, ClusterHostListModel> modelProvider, ApplicationConstants constants) {
        super(modelProvider);
        initTable(constants);
        initWidget(getTable());
    }

    void initTable(final ApplicationConstants constants) {
        getTable().addColumn(new HostStatusColumn(), constants.empty(), "30px"); //$NON-NLS-1$

        TextColumnWithTooltip<VDS> nameColumn = new TextColumnWithTooltip<VDS>() {
            @Override
            public String getValue(VDS object) {
                return object.getvds_name();
            }
        };
        getTable().addColumn(nameColumn, constants.nameClusterHost());

        TextColumnWithTooltip<VDS> hostColumn = new TextColumnWithTooltip<VDS>() {
            @Override
            public String getValue(VDS object) {
                return object.gethost_name();
            }
        };
        getTable().addColumn(hostColumn, constants.hostIpClusterHost());

        TextColumnWithTooltip<VDS> statusColumn = new EnumColumn<VDS, VDSStatus>() {
            @Override
            public VDSStatus getRawValue(VDS object) {
                return object.getstatus();
            }
        };
        getTable().addColumn(statusColumn, constants.statusClusterHost());

        if (ApplicationModeHelper.isModeSupported(ApplicationMode.VirtOnly)) {
            TextColumnWithTooltip<VDS> loadColumn = new TextColumnWithTooltip<VDS>() {
                @Override
                public String getValue(VDS object) {
                    int numOfActiveVMs = object.getvm_active() != null ? object.getvm_active() : 0;
                    return ConstantsManager.getInstance().getMessages().numberOfVmsForHostsLoad(numOfActiveVMs);
                }
            };
            getTable().addColumn(loadColumn, constants.loadClusterHost());
        }
    }

}
