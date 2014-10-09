package org.ovirt.engine.ui.common.widget.uicommon.storage;

import org.ovirt.engine.ui.common.widget.HasEditorDriver;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextBoxEditor;
import org.ovirt.engine.ui.uicommonweb.models.storage.IStorageModel;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ValueBox;

public abstract class AbstractStorageView<M extends IStorageModel> extends Composite implements HasEditorDriver<M> {

    @Ignore
    protected EntityModelTextBoxEditor pathEditor;

    protected boolean multiSelection;

    public abstract void focus();

    protected void createPathEditor() {
        pathEditor = new EntityModelTextBoxEditor() {
            boolean accessible;

            @Override
            public void setAccessible(boolean accessible) {
                this.accessible = accessible;

                if (!accessible) {
                    ValueBox<Object> localPathValueBox = super.asValueBox();
                    localPathValueBox.setReadOnly(true);
                    localPathValueBox.getElement().getStyle().setBorderWidth(0, Unit.PX);
                }
            }

            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(accessible ? enabled : true);
            }
        };
    }

    public boolean isSubViewFocused() {
        return false;
    }

}
