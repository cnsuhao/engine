package org.ovirt.engine.ui.common.widget.table.column;

import org.ovirt.engine.ui.common.widget.renderer.DiskSizeRenderer;
import org.ovirt.engine.ui.common.widget.renderer.DiskSizeRenderer.DiskSizeUnit;

public abstract class DiskSizeColumn<T> extends RenderedTextColumn<T, Long> {

    public DiskSizeColumn() {
        super(new DiskSizeRenderer<Long>(DiskSizeUnit.BYTE));
    }

    public DiskSizeColumn(DiskSizeUnit diskSizeUnit) {
        super(new DiskSizeRenderer<Long>(diskSizeUnit));
    }

}
