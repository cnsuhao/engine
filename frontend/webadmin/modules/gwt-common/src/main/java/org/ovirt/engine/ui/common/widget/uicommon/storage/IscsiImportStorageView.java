package org.ovirt.engine.ui.common.widget.uicommon.storage;

import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.SanStorageModelBase;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;

public class IscsiImportStorageView extends SanImportStorageView {

    @UiField
    @Path(value = "error")
    Label errorMessage;

    EntityModelCellTable<ListModel> table;
    IscsiTargetToLunView iscsiTargetToLunView;

    @Override
    protected void initLists(SanStorageModelBase object) {
        super.initLists(object);

        iscsiTargetToLunView = new IscsiTargetToLunView(74, 173, true, true);
        extraContentPanel.add(iscsiTargetToLunView);
    }

    @Override
    void localize(CommonApplicationConstants constants) {
        listLabel.setText(constants.storageIscsiSelectStorageLabel());
    }

    @Override
    void addStyles() {
        contentPanel.getElement().getStyle().setHeight(395, Unit.PX);
        listPanel.getElement().getStyle().setHeight(134, Unit.PX);
        addBorder(listPanel.getElement().getStyle());
        addBorder(extraContentPanel.getElement().getStyle());
    }

    private void addBorder(Style style) {
        style.setBorderColor("lightGrey"); //$NON-NLS-1$
        style.setBorderWidth(1, Unit.PX);
        style.setBorderStyle(BorderStyle.SOLID);
    }

    @Override
    public void edit(final SanStorageModelBase object) {
        super.edit(object);
        iscsiTargetToLunView.edit(object);
        iscsiTargetToLunView.activateItemsUpdate();
    }

    @Override
    public boolean isSubViewFocused() {
        return iscsiTargetToLunView.isDiscoverPanelFocused();
    }
}
