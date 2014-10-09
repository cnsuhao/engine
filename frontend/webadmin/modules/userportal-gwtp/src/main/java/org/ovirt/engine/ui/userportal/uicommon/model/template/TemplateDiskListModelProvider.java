package org.ovirt.engine.ui.userportal.uicommon.model.template;

import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.ui.common.auth.CurrentUser;
import org.ovirt.engine.ui.uicommonweb.models.templates.UserPortalTemplateDiskListModel;
import org.ovirt.engine.ui.uicommonweb.models.userportal.UserPortalTemplateListModel;
import org.ovirt.engine.ui.userportal.gin.ClientGinjector;
import org.ovirt.engine.ui.userportal.uicommon.model.UserPortalModelResolver;
import org.ovirt.engine.ui.userportal.uicommon.model.UserPortalSearchableDetailModelProvider;

import com.google.inject.Inject;

public class TemplateDiskListModelProvider
        extends UserPortalSearchableDetailModelProvider<DiskImage, UserPortalTemplateListModel, UserPortalTemplateDiskListModel> {

    @Inject
    public TemplateDiskListModelProvider(ClientGinjector ginjector,
            UserPortalTemplateListProvider parentProvider,
            UserPortalModelResolver resolver,
            CurrentUser user) {
        super(ginjector, parentProvider, UserPortalTemplateDiskListModel.class, resolver, user);
    }

    @Override
    protected UserPortalTemplateDiskListModel createModel() {
        return new UserPortalTemplateDiskListModel();
    }

}
