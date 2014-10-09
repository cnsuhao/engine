package org.ovirt.engine.ui.common.widget.table.column;

import com.google.gwt.user.cellview.client.Column;

/**
 * Column for displaying text using {@link TextCellWithTooltip}.
 *
 * @param <T>
 *            Table row data type.
 */
public abstract class TextColumnWithTooltip<T> extends Column<T, String> implements ColumnWithElementId {

    public TextColumnWithTooltip() {
        this(TextCellWithTooltip.UNLIMITED_LENGTH);
    }

    public TextColumnWithTooltip(int maxTextLength) {
        super(new TextCellWithTooltip(maxTextLength));
    }

    @Override
    public void configureElementId(String elementIdPrefix, String columnId) {
        getCell().setElementIdPrefix(elementIdPrefix);
        getCell().setColumnId(columnId);
    }

    @Override
    public TextCellWithTooltip getCell() {
        return (TextCellWithTooltip) super.getCell();
    }

}
