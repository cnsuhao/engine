package org.ovirt.engine.ui.userportal.uicommon.model.vm;

import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.ui.common.auth.CurrentUser;
import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.userportal.UserPortalListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.UserPortalVmSnapshotListModel;
import org.ovirt.engine.ui.userportal.gin.ClientGinjector;
import org.ovirt.engine.ui.userportal.section.main.presenter.popup.vm.VmClonePopupPresenterWidget;
import org.ovirt.engine.ui.userportal.section.main.presenter.popup.vm.VmSnapshotCreatePopupPresenterWidget;
import org.ovirt.engine.ui.userportal.uicommon.model.UserPortalModelResolver;
import org.ovirt.engine.ui.userportal.uicommon.model.UserPortalSearchableDetailModelProvider;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class VmSnapshotListModelProvider extends UserPortalSearchableDetailModelProvider<Snapshot, UserPortalListModel, UserPortalVmSnapshotListModel> {

    private final Provider<VmSnapshotCreatePopupPresenterWidget> createPopupProvider;
    private final Provider<VmClonePopupPresenterWidget> cloneVmPopupProvider;

    @Inject
    public VmSnapshotListModelProvider(ClientGinjector ginjector,
            UserPortalListProvider parentModelProvider,
            UserPortalModelResolver resolver,
            Provider<VmSnapshotCreatePopupPresenterWidget> createPopupProvider,
            Provider<VmClonePopupPresenterWidget> cloneVmPopupProvider,
            CurrentUser user) {
        super(ginjector, parentModelProvider, UserPortalVmSnapshotListModel.class, resolver, user);
        this.createPopupProvider = createPopupProvider;
        this.cloneVmPopupProvider = cloneVmPopupProvider;
    }

    @Override
    protected UserPortalVmSnapshotListModel createModel() {
        return new UserPortalVmSnapshotListModel();
    }

    @Override
    public AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(UserPortalVmSnapshotListModel source,
            UICommand lastExecutedCommand, Model windowModel) {
        if (lastExecutedCommand == getModel().getNewCommand()) {
            return createPopupProvider.get();
        } else if (lastExecutedCommand == getModel().getCloneVmCommand()) {
            return cloneVmPopupProvider.get();
        } else {
            return super.getModelPopup(source, lastExecutedCommand, windowModel);
        }
    }

}
