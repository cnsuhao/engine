package org.ovirt.engine.ui.userportal.widget.tab;

import org.ovirt.engine.ui.common.widget.tab.AbstractTab;
import org.ovirt.engine.ui.common.widget.tab.AbstractTabPanel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Widget;
import com.gwtplatform.mvp.client.TabData;

public class UserPortalMainTab extends AbstractTab {

    interface WidgetUiBinder extends UiBinder<Widget, UserPortalMainTab> {
        WidgetUiBinder uiBinder = GWT.create(WidgetUiBinder.class);
    }
    public UserPortalMainTab(TabData tabData, AbstractTabPanel tabPanel) {
        super(tabData, tabPanel);
        initWidget(WidgetUiBinder.uiBinder.createAndBindUi(this));
    }

}
