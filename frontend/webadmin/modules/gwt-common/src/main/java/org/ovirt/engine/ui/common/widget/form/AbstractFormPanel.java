package org.ovirt.engine.ui.common.widget.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.ui.common.widget.label.TextBoxLabel;

import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ValueLabel;
import com.google.gwt.user.client.ui.Widget;

public abstract class AbstractFormPanel extends Composite {

    @UiField
    public HorizontalPanel contentPanel;

    // A list of detail viewers - every viewer represents a column of form items
    public List<Grid> detailViewers = new ArrayList<Grid>();

    private final Map<String, TextBoxLabel> valueLabelToItsTextBox = new HashMap<String, TextBoxLabel>();

    public void addFormDetailView(int numOfRows) {
        // Create a new column of form items
        Grid viewer = new Grid(numOfRows, 2);
        viewer.setStyleName("formPanel_viewer"); //$NON-NLS-1$
        viewer.getColumnFormatter().setStyleName(0, "formPanel_viewerNamesColumn"); //$NON-NLS-1$
        viewer.getColumnFormatter().setStyleName(1, "formPanel_viewerValuesColumn"); //$NON-NLS-1$

        // Add the column to the list and view
        detailViewers.add(viewer);
        contentPanel.add(viewer);
    }

    public void addFormItem(FormItem item) {
        // Create FormItem name label
        Label name = new Label(item.getName());
        name.setStyleName("formPanel_itemName"); //$NON-NLS-1$
        if (!name.getText().isEmpty()) {
            name.setText(name.getText() + ":"); //$NON-NLS-1$
        }

        // Create FormItem value label
        Widget widget = item.getValue();
        TextBoxLabel value = new TextBoxLabel();
        value.setStyleName("formPanel_itemValue"); //$NON-NLS-1$

        if (widget instanceof TextBoxLabel) {
            value = (TextBoxLabel) widget;
        } else if (widget instanceof ValueLabel<?>) {
            // this ensures, that the same item will use the same instance of it's TextBoxLabel
            String itemName = item.getName();
            if (valueLabelToItsTextBox.containsKey(itemName)) {
                value = valueLabelToItsTextBox.get(itemName);
            } else {
                valueLabelToItsTextBox.put(itemName, value);
            }

            value.setText(((ValueLabel<?>) widget).getElement().getInnerHTML());
        }

        // Add FormItem at the appropriate position (by the row/column specified in FormItem)
        detailViewers.get(item.getColumn()).setWidget(item.getRow(), 0, name);
        detailViewers.get(item.getColumn()).setWidget(item.getRow(), 1, value);
    }

    public void removeFormItem(FormItem item) {
        Grid grid = detailViewers.get(item.getColumn());
        if (grid != null) {
            grid.setWidget(item.getRow(), 0, new Label());
            grid.setWidget(item.getRow(), 1, new Label());
        }
    }

    public void clear() {
        for (Grid detailViewer : detailViewers) {
            detailViewer.clear(true);
        }
    }

    public void setColumnsWidth(String... columnsWidth) {
        for (int i = 0; i < detailViewers.size(); i++) {
            detailViewers.get(i).getColumnFormatter().setWidth(1, columnsWidth[i]);
        }
    }

}
