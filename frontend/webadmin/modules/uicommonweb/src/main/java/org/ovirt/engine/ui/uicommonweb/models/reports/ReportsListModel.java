package org.ovirt.engine.ui.uicommonweb.models.reports;

import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.EventDefinition;
import org.ovirt.engine.ui.uicommonweb.HtmlParameters;
import org.ovirt.engine.ui.uicommonweb.ReportInit;
import org.ovirt.engine.ui.uicommonweb.models.CommonModel;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemModel;
import org.ovirt.engine.ui.uicompat.ReportParser.Dashboard;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.Cookies;

public class ReportsListModel extends SearchableListModel {

    HtmlParameters htmlParams = new HtmlParameters();
    private String lastResourceId = ""; //$NON-NLS-1$
    private final String reportUrl;
    private final Event reportModelRefreshEvent = new Event(new EventDefinition("ReportModelRefreshed", //$NON-NLS-1$
            ReportsListModel.class));

    public Event getReportModelRefreshEvent() {
        return reportModelRefreshEvent;
    }

    public ReportsListModel(String baseUrl) {
        String sessionID = Cookies.getCookie("JSESSIONID"); //$NON-NLS-1$
        reportUrl = baseUrl + "/flow.html" + "?viewAsDashboardFrame=true"; //$NON-NLS-1$ //$NON-NLS-2$
        htmlParams.setParameter("sessionID", sessionID); //$NON-NLS-1$

        setFlowId();

        String currentLocale = LocaleInfo.getCurrentLocale().getLocaleName();
        htmlParams.setParameter("userLocale", (currentLocale.equals("default") ? "en_US" : currentLocale)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        setDefaultSearchString("Reports:"); //$NON-NLS-1$
        setSearchString(getDefaultSearchString());
        setAvailableInModes(ApplicationMode.VirtOnly);

        getSearchNextPageCommand().setIsAvailable(false);
        getSearchPreviousPageCommand().setIsAvailable(false);

        setIsTimerDisabled(true);
    }

    public Map<String, List<String>> getParams() {
        return htmlParams.getParameters();
    }

    public String getUrl() {
        return reportUrl;
    }

    private void setFlowId() {
        if (ReportInit.getInstance().isCommunityEdition()) {
            htmlParams.setParameter("_flowId", "viewReportFlow"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            htmlParams.setParameter("_flowId", "dashboardRuntimeFlow"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    public void setPassword(String password) {
        htmlParams.setParameter("j_password", password); //$NON-NLS-1$
    }

    public void setUser(String user) {
        htmlParams.setParameter("j_username", user); //$NON-NLS-1$
    }

    public void setDataCenterID(final String uuid) {
        htmlParams.setParameter(getHiddenParamPrefix() + "P_DataCenter_ID", uuid); //$NON-NLS-1$
    }

    public void setResourceId(String idParamName, String id) {
        htmlParams.setParameter(getHiddenParamPrefix() + idParamName, id);
        lastResourceId = idParamName;
    }

    public void setUri(String uri) {
        if (ReportInit.getInstance().isCommunityEdition()) {
            htmlParams.setParameter("reportUnit", uri); //$NON-NLS-1$
        } else {
            htmlParams.setParameter("dashboardResource", uri); //$NON-NLS-1$
        }
    }

    private void clearTreeSensitiveParams() {
        removeParam("P_DataCenter_ID"); //$NON-NLS-1$
        removeParam(lastResourceId);
    }

    public void removeParam(String paramName) {
        htmlParams.removeParameter(paramName);
    }

    public void refreshReportModel() {
        SystemTreeItemModel treeItemModel =
                ((SystemTreeItemModel) CommonModel.getInstance().getSystemTree().getSelectedItem());
        if (treeItemModel == null) {
            return;
        }

        clearTreeSensitiveParams();
        String title = treeItemModel.getTitle();

        Dashboard dashboard = ReportInit.getInstance().getDashboard(treeItemModel.getType().toString());
        if (dashboard != null) {
            setUri(dashboard.getUri());

            switch (treeItemModel.getType()) {
            case System: {
                break;
            }
            case DataCenter: {
                storage_pool dataCenter = (storage_pool) treeItemModel.getEntity();
                setDataCenterID(dataCenter.getId().toString());
                break;
            }
            case Clusters: {
                storage_pool dataCenter = (storage_pool) treeItemModel.getEntity();
                setDataCenterID(dataCenter.getId().toString());
                break;
            }
            case Cluster: {
                VDSGroup cluster = (VDSGroup) treeItemModel.getEntity();
                setDataCenterID(cluster.getStoragePoolId().toString());
                setResourceId("P_Cluster_ID", cluster.getQueryableId().toString()); //$NON-NLS-1$
                break;
            }
            case Hosts: {
                VDSGroup cluster = (VDSGroup) treeItemModel.getEntity();
                setDataCenterID(cluster.getStoragePoolId().toString());
                setResourceId("P_Cluster_ID", cluster.getQueryableId().toString()); //$NON-NLS-1$
                break;
            }
            case Host: {
                VDS host = (VDS) treeItemModel.getEntity();
                setDataCenterID(host.getStoragePoolId().toString());
                setResourceId("P_Host_ID", host.getQueryableId().toString()); //$NON-NLS-1$
                break;
            }
            case Storages: {
                storage_pool dataCenter = (storage_pool) treeItemModel.getEntity();
                setDataCenterID(dataCenter.getId().toString());
                break;
            }
            case Storage: {
                storage_domains storage = (storage_domains) treeItemModel.getEntity();
                setDataCenterID(storage.getstorage_pool_id().toString());
                setResourceId("P_StorageDomain_ID", storage.getQueryableId().toString()); //$NON-NLS-1$
                break;
            }
            case Templates: {
                storage_pool dataCenter = (storage_pool) treeItemModel.getEntity();
                setDataCenterID(dataCenter.getId().toString());
                break;
            }
            case VMs: {
                VM vm = (VM) treeItemModel.getEntity();
                setDataCenterID(vm.getStoragePoolId().toString());
                setResourceId("P_VM_ID", vm.getQueryableId().toString()); //$NON-NLS-1$
                break;
            }
            case Networks: {
                storage_pool dataCenter = (storage_pool) treeItemModel.getEntity();
                setDataCenterID(dataCenter.getId().toString());
                break;
            }
            case Network: {
                Network network = (Network) treeItemModel.getEntity();
                setDataCenterID(network.getDataCenterId().toString());
                setResourceId("P_Network_ID", network.getQueryableId().toString()); //$NON-NLS-1$
                break;
            }
            default:
                // webadmin: redirect to default tab in case no tab is selected.
            }
        }
        GWT.log("Tree Item changed: " + title); //$NON-NLS-1$

        reportModelRefreshEvent.raise(this, EventArgs.Empty);
    }

    @Override
    protected String getListName() {
        return "DeashboardReportsListModel"; //$NON-NLS-1$
    }

    private String getHiddenParamPrefix() {
        if (ReportInit.getInstance().isCommunityEdition()) {
            return ""; //$NON-NLS-1$
        } else {
            return "hidden_"; //$NON-NLS-1$
        }
    }

    @Override
    public boolean IsSearchStringMatch(String searchString)
    {
        return searchString.trim().toLowerCase().startsWith("reports"); //$NON-NLS-1$
    }

}
