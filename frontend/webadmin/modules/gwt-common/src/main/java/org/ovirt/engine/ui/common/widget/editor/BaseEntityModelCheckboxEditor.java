package org.ovirt.engine.ui.common.widget.editor;

import java.util.List;

import org.ovirt.engine.ui.common.widget.AbstractValidatedWidgetWithLabel;
import org.ovirt.engine.ui.common.widget.Align;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.user.client.ui.CheckBox;

public abstract class BaseEntityModelCheckboxEditor<T> extends AbstractValidatedWidgetWithLabel<T, BaseEntityModelCheckbox<T>>
        implements IsEditor<WidgetWithLabelEditor<T, BaseEntityModelCheckboxEditor<T>>> {

    private final WidgetWithLabelEditor<T, BaseEntityModelCheckboxEditor<T>> editor;

    private final boolean useCheckBoxWidgetLabel;

    public BaseEntityModelCheckboxEditor(BaseEntityModelCheckbox<T> contentWidget) {
        this(Align.LEFT, contentWidget);
    }

    public BaseEntityModelCheckboxEditor(Align labelAlign, BaseEntityModelCheckbox<T> contentWidget) {
        super(contentWidget);

        this.editor = WidgetWithLabelEditor.of(getContentWidget().asEditor(), this);
        this.useCheckBoxWidgetLabel = labelAlign == Align.RIGHT;

        // In case we use CheckBox widget label instead of declared LabelElement,
        // align content widget container to the left and hide the LabelElement
        if (useCheckBoxWidgetLabel) {
            getContentWidgetContainer().getElement().getStyle().setFloat(Float.LEFT);
            getLabelElement().getStyle().setDisplay(Display.NONE);
        }
    }

    public CheckBox asCheckBox() {
        return getContentWidget().asCheckBox();
    }

    @Override
    public WidgetWithLabelEditor<T, BaseEntityModelCheckboxEditor<T>> asEditor() {
        return editor;
    }

    @Override
    protected void applyCommonValidationStyles() {
        super.applyCommonValidationStyles();
        getValidatedWidgetStyle().setPadding(5, Unit.PX);
    }

    @Override
    public void markAsValid() {
        super.markAsValid();
        getValidatedWidgetStyle().setBorderStyle(BorderStyle.NONE);
    }

    @Override
    public void markAsInvalid(List<String> validationHints) {
        super.markAsInvalid(validationHints);
        getValidatedWidgetStyle().setBorderStyle(BorderStyle.SOLID);
    }

    @Override
    protected Element getContentWidgetElement() {
        // Actual check box input element is the first child of CheckBox element
        Node input = asCheckBox().getElement().getChild(0);
        return Element.as(input);
    }

    @Override
    protected void updateLabelElementId(String elementId) {
        if (useCheckBoxWidgetLabel) {
            LabelElement.as(Element.as(asCheckBox().getElement().getChild(1))).setHtmlFor(elementId);
        } else {
            super.updateLabelElementId(elementId);
        }
    }

    @Override
    public String getLabel() {
        if (useCheckBoxWidgetLabel) {
            return asCheckBox().getText();
        } else {
            return super.getLabel();
        }
    }

    @Override
    public void setLabel(String label) {
        if (useCheckBoxWidgetLabel) {
            asCheckBox().setText(label);
        } else {
            super.setLabel(label);
        }
    }
}
