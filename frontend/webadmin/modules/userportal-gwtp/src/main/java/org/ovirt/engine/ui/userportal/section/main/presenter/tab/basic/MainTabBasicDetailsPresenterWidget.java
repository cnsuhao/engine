package org.ovirt.engine.ui.userportal.section.main.presenter.tab.basic;

import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.ui.common.widget.HasEditorDriver;
import org.ovirt.engine.ui.uicommonweb.models.userportal.ConsoleProtocol;
import org.ovirt.engine.ui.uicommonweb.models.userportal.UserPortalBasicListModel;
import org.ovirt.engine.ui.uicommonweb.models.userportal.UserPortalItemModel;
import org.ovirt.engine.ui.userportal.ApplicationMessages;
import org.ovirt.engine.ui.userportal.section.main.presenter.popup.console.ConsoleModelChangedEvent;
import org.ovirt.engine.ui.userportal.section.main.presenter.popup.console.ConsoleModelChangedEvent.ConsoleModelChangedHandler;
import org.ovirt.engine.ui.userportal.uicommon.model.UserPortalModelInitEvent;
import org.ovirt.engine.ui.userportal.uicommon.model.UserPortalModelInitEvent.UserPortalModelInitHandler;
import org.ovirt.engine.ui.userportal.uicommon.model.basic.UserPortalBasicListProvider;
import org.ovirt.engine.ui.userportal.widget.basic.ConsoleUtils;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.PresenterWidget;
import com.gwtplatform.mvp.client.View;

public class MainTabBasicDetailsPresenterWidget extends PresenterWidget<MainTabBasicDetailsPresenterWidget.ViewDef> {

    public interface ViewDef extends View, HasEditorDriver<UserPortalBasicListModel> {

        void editDistItems(Iterable<DiskImage> diskImages);

        void setConsoleWarningMessage(String message);

        void setConsoleProtocol(String protocol);

        void setEditConsoleEnabled(boolean enabled);

        HasClickHandlers getEditButton();

        void clear();

        void displayVmOsImages(boolean dispaly);
    }

    private final ConsoleUtils consoleUtils;
    private final ApplicationMessages messages;

    @Inject
    public MainTabBasicDetailsPresenterWidget(EventBus eventBus,
            ViewDef view,
            final UserPortalBasicListProvider modelProvider,
            final ConsoleUtils consoleUtils,
            final ApplicationMessages messages) {
        super(eventBus, view);
        this.consoleUtils = consoleUtils;
        this.messages = messages;

        listenOnSelectedItemEvent(modelProvider);

        listenOnDiskModelChangeEvent(modelProvider);

        listenOnEditButton(modelProvider);

        listenOnConsoleModelChangeEvent(eventBus, modelProvider);

        getEventBus().addHandler(UserPortalModelInitEvent.getType(), new UserPortalModelInitHandler() {

            @Override
            public void onUserPortalModelInit(UserPortalModelInitEvent event) {
                listenOnSelectedItemEvent(modelProvider);
                listenOnDiskModelChangeEvent(modelProvider);
            }

        });
    }

    protected void listenOnConsoleModelChangeEvent(EventBus eventBus, final UserPortalBasicListProvider modelProvider) {
        eventBus.addHandler(ConsoleModelChangedEvent.getType(), new ConsoleModelChangedHandler() {

            @Override
            public void onConsoleModelChanged(ConsoleModelChangedEvent event) {
                if (modelProvider.getModel().getSelectedItem() == null) {
                    return;
                }

                setupConsole(modelProvider);
            }

        });
    }

    private void listenOnDiskModelChangeEvent(final UserPortalBasicListProvider modelProvider) {
        modelProvider.getModel().getvmBasicDiskListModel().getItemsChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if (modelProvider.getModel().getSelectedItem() == null) {
                    return;
                }
                setupDisks(modelProvider);
            }
        });
    }

    private void listenOnSelectedItemEvent(final UserPortalBasicListProvider modelProvider) {
        modelProvider.getModel().getSelectedItemChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if (modelProvider.getModel().getSelectedItem() == null) {
                    getView().clear();
                    return;
                }
                getView().edit(modelProvider.getModel());
                getView().displayVmOsImages(true);
                setupDisks(modelProvider);
                setupConsole(modelProvider);
            }

        });
    }

    private void listenOnEditButton(final UserPortalBasicListProvider modelProvider) {
        registerHandler(getView().getEditButton().addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (!isEditConsoleEnabled(modelProvider.getModel().getSelectedItem())) {
                    return;
                }
                modelProvider.getModel().getEditConsoleCommand().Execute();
            }
        }));
    }

    private void setupDisks(final UserPortalBasicListProvider modelProvider) {
        @SuppressWarnings("unchecked")
        Iterable<DiskImage> diskImages = modelProvider.getModel().getvmBasicDiskListModel().getItems();
        if (diskImages != null) {
            getView().editDistItems(diskImages);
        }
    }

    private void setupConsole(final UserPortalBasicListProvider modelProvider) {
        UserPortalItemModel item = modelProvider.getModel().getSelectedItem();

        getView().setEditConsoleEnabled(isEditConsoleEnabled(item));

        if (!item.getIsPool()) {
            ConsoleProtocol protocol = consoleUtils.determineConnectionProtocol(item);
            if (protocol == null) {
                getView().setConsoleWarningMessage(consoleUtils.determineProtocolMessage(item));
            } else {
                getView().setConsoleProtocol(protocol == null ? "" : determineProtocolMessage(protocol, item)); //$NON-NLS-1$
            }
        } else {
            getView().setConsoleProtocol(""); //$NON-NLS-1$
        }
    }

    private String determineProtocolMessage(ConsoleProtocol protocol, UserPortalItemModel item) {
        if (consoleUtils.isSmartcardGloballyEnabled(item) && !consoleUtils.isSmartcardEnabledOverriden(item)) {
            return messages.consoleWithSmartcard(protocol.displayName);
        }

        return protocol.displayName;
    }

    private boolean isEditConsoleEnabled(UserPortalItemModel item) {
        if (!item.getIsPool() && consoleUtils.determineConnectionProtocol(item) != null
                && item.getStatus().equals(VMStatus.Up)) {
            return true;
        }

        return false;

    }

}

