package org.ovirt.engine.ui.webadmin.section.main.view.tab.disk;

import javax.inject.Inject;

import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.ui.common.uicommon.model.DetailModelProvider;
import org.ovirt.engine.ui.common.view.AbstractSubTabFormView;
import org.ovirt.engine.ui.common.widget.form.FormBuilder;
import org.ovirt.engine.ui.common.widget.form.FormItem;
import org.ovirt.engine.ui.common.widget.form.GeneralFormPanel;
import org.ovirt.engine.ui.common.widget.label.TextBoxLabel;
import org.ovirt.engine.ui.uicommonweb.models.disks.DiskGeneralModel;
import org.ovirt.engine.ui.uicommonweb.models.disks.DiskListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.ClientGinjectorProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.disk.SubTabDiskGeneralPresenter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;

public class SubTabDiskGeneralView extends AbstractSubTabFormView<Disk, DiskListModel, DiskGeneralModel> implements SubTabDiskGeneralPresenter.ViewDef, Editor<DiskGeneralModel> {

    interface ViewUiBinder extends UiBinder<Widget, SubTabDiskGeneralView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface Driver extends SimpleBeanEditorDriver<DiskGeneralModel, SubTabDiskGeneralView> {
        Driver driver = GWT.create(Driver.class);
    }

    TextBoxLabel alias = new TextBoxLabel();
    TextBoxLabel description = new TextBoxLabel();
    TextBoxLabel diskId = new TextBoxLabel();
    TextBoxLabel lunId = new TextBoxLabel();
    TextBoxLabel quotaName = new TextBoxLabel();

    @UiField(provided = true)
    GeneralFormPanel formPanel;

    FormBuilder formBuilder;

    private ApplicationConstants constants = ClientGinjectorProvider.instance().getApplicationConstants();

    @Inject
    public SubTabDiskGeneralView(DetailModelProvider<DiskListModel, DiskGeneralModel> modelProvider) {
        super(modelProvider);

        // Init formPanel
        formPanel = new GeneralFormPanel();

        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        Driver.driver.initialize(this);

        // Build a form using the FormBuilder
        formBuilder = new FormBuilder(formPanel, 1, 6);

        formBuilder.setColumnsWidth("300px"); //$NON-NLS-1$
        formBuilder.addFormItem(new FormItem(constants.aliasDisk(), alias, 0, 0));
        formBuilder.addFormItem(new FormItem(constants.descriptionDisk(), description, 1, 0));
        formBuilder.addFormItem(new FormItem(constants.idDisk(), diskId, 2, 0));
        formBuilder.addFormItem(new FormItem(constants.lunIdSanStorage(), lunId, 3, 0) {
            @Override
            public boolean isVisible() {
                return getDetailModel().isLun();
            }
        });
        formBuilder.addFormItem(new FormItem(constants.quota(), quotaName, 4, 0) {
            @Override
            public boolean isVisible() {
                return getDetailModel().isQuotaAvailable();
            }
        });
    }

    @Override
    public void setMainTabSelectedItem(Disk selectedItem) {
        Driver.driver.edit(getDetailModel());
        formBuilder.showForm(getDetailModel());
    }

}
