package org.ovirt.engine.ui.userportal.section.main.view;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.AbstractView;
import org.ovirt.engine.ui.userportal.ApplicationConstants;
import org.ovirt.engine.ui.userportal.section.main.presenter.HeaderPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class HeaderView extends AbstractView implements HeaderPresenterWidget.ViewDef {

    interface ViewUiBinder extends UiBinder<Widget, HeaderView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<HeaderView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField
    @WithElementId("userName")
    InlineLabel userNameLabel;

    @UiField(provided = true)
    @WithElementId
    final Anchor logoutLink;

    @UiField(provided = true)
    @WithElementId
    final Anchor guideLink;

    @UiField(provided = true)
    @WithElementId
    final Anchor aboutLink;

    @UiField
    HTMLPanel mainTabBarPanel;

    @UiField
    FlowPanel mainTabContainer;

    @Inject
    public HeaderView(ApplicationConstants constants) {
        this.logoutLink = new Anchor(constants.logoutLinkLabel());
        this.guideLink = new Anchor(constants.guideLinkLabel());
        this.aboutLink = new Anchor(constants.aboutLinkLabel());
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        mainTabBarPanel.getElement().getStyle().setZIndex(1);
    }

    @Override
    public void addTabWidget(Widget tabWidget, int index) {
        mainTabContainer.insert(tabWidget, index);
    }

    @Override
    public void removeTabWidget(Widget tabWidget) {
        mainTabContainer.getElement().removeChild(tabWidget.getElement());
    }

    @Override
    public void setUserName(String userName) {
        userNameLabel.setText(userName);
    }

    @Override
    public HasClickHandlers getLogoutLink() {
        return logoutLink;
    }

    @Override
    public HasClickHandlers getAboutLink() {
        return aboutLink;
    }

    @Override
    public void setMainTabPanelVisible(boolean visible) {
        mainTabBarPanel.setVisible(visible);
    }

    @Override
    public HasHandlers getGuideLink() {
        return guideLink;
    }

    @Override
    public void setGuideLinkEnabled(boolean enabled) {
        guideLink.getElement().getStyle().setCursor(enabled ? Cursor.POINTER : Cursor.DEFAULT);
    }

}
