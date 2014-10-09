package org.ovirt.engine.ui.userportal.uicommon;

import java.util.List;

import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.EventDefinition;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.common.uicommon.ClientAgentType;
import org.ovirt.engine.ui.common.uicommon.DocumentationPathTranslator;
import org.ovirt.engine.ui.common.uicommon.model.UiCommonInitEvent;
import org.ovirt.engine.ui.common.uicommon.model.UiCommonInitEvent.UiCommonInitHandler;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Configurator;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.vms.ISpice;
import org.ovirt.engine.ui.uicommonweb.models.vms.WANDisableEffects;
import org.ovirt.engine.ui.uicommonweb.models.vms.WanColorDepth;
import org.ovirt.engine.ui.userportal.section.main.presenter.tab.MainTabBasicPresenter;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class UserPortalConfigurator extends Configurator implements IEventListener, UiCommonInitHandler {

    public static final String DOCUMENTATION_GUIDE_PATH = "User_Portal_Guide/index.html"; //$NON-NLS-1$

    public EventDefinition spiceVersionFileFetchedEvent_Definition =
            new EventDefinition("spiceVersionFileFetched", UserPortalConfigurator.class); //$NON-NLS-1$
    public Event spiceVersionFileFetchedEvent = new Event(spiceVersionFileFetchedEvent_Definition);

    public EventDefinition documentationFileFetchedEvent_Definition =
        new EventDefinition("documentationFileFetched", UserPortalConfigurator.class); //$NON-NLS-1$
    public Event documentationFileFetchedEvent = new Event(documentationFileFetchedEvent_Definition);

    public EventDefinition usbFilterFileFetchedEvent_Definition =
            new EventDefinition("usbFilterFileFetched", UserPortalConfigurator.class); //$NON-NLS-1$
    public Event usbFilterFileFetchedEvent = new Event(usbFilterFileFetchedEvent_Definition);

    private final Provider<MainTabBasicPresenter> basicPresenter;

    private static final ClientAgentType clientAgentType = new ClientAgentType();

    @Inject
    public UserPortalConfigurator(Provider<MainTabBasicPresenter> basicPresenter, EventBus eventBus) {
        super();
        this.basicPresenter = basicPresenter;
        eventBus.addHandler(UiCommonInitEvent.getType(), this);

        // This means that it is UserPortal application.
        setIsAdmin(false);

        // Add event listeners
        spiceVersionFileFetchedEvent.addListener(this);
        documentationFileFetchedEvent.addListener(this);
        usbFilterFileFetchedEvent.addListener(this);

        // Update USB filters
        updateUsbFilter();

        // Update Spice version if needed
        updateSpiceVersion();
    }

    public void updateUsbFilter() {
        fetchFile(getSpiceBaseURL() + "usbfilter.txt", usbFilterFileFetchedEvent); //$NON-NLS-1$
    }

    @Override
    public void Configure(ISpice spice) {
        super.Configure(spice);

        updateWanColorDepthOptions(spice);
        updateWANDisableEffects(spice);
    }

    private void updateWANDisableEffects(final ISpice spice) {
        AsyncDataProvider.GetWANDisableEffects(new AsyncQuery(this, new INewAsyncCallback() {
            @SuppressWarnings("unchecked")
            @Override
            public void OnSuccess(Object target, Object returnValue) {
                spice.setWANDisableEffects((List<WANDisableEffects>) returnValue);
            }
        }));
    }

    private void updateWanColorDepthOptions(final ISpice spice) {
        AsyncDataProvider.GetWANColorDepth(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void OnSuccess(Object target, Object returnValue) {
                spice.setWANColorDepth((WanColorDepth) returnValue);
            }
        }));
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args) {
        if (ev.equals(spiceVersionFileFetchedEvent_Definition)) {
            Version spiceVersion = parseVersion(((FileFetchEventArgs) args).getFileContent());
            setSpiceVersion(spiceVersion);
        } else if (ev.equals(documentationFileFetchedEvent_Definition)) {
            String documentationPathFileContent = ((FileFetchEventArgs) args).getFileContent();
            DocumentationPathTranslator.init(documentationPathFileContent);
        } else if (ev.equals(usbFilterFileFetchedEvent_Definition)) {
            String usbFilter = ((FileFetchEventArgs) args).getFileContent();
            setUsbFilter(usbFilter);
        }
    }

    /**
     * Returns true if the basic view is shown, else returns false
     */
    @Override
    public boolean getSpiceFullScreen() {
        return basicPresenter.get().isVisible();
    }

    @Override
    protected Event getSpiceVersionFileFetchedEvent() {
        return spiceVersionFileFetchedEvent;
    }

    @Override
    public void onUiCommonInit(UiCommonInitEvent event) {
        updateDocumentationBaseURL();
    }

    @Override
    protected String clientBrowserType() {
        return clientAgentType.browser;
    }

    @Override
    protected String clientOsType() {
        return clientAgentType.os;
    }

    @Override
    protected String clientPlatformType() {
        return clientAgentType.getPlatform();
    }

    @Override
    protected void onUpdateDocumentationBaseURL() {
        fetchFile(getDocumentationBaseURL() + "UserPortalDocumentationPath.csv", documentationFileFetchedEvent); //$NON-NLS-1$
    }

}
