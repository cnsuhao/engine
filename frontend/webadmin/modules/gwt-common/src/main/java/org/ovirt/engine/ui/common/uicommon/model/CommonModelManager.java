package org.ovirt.engine.ui.common.uicommon.model;

import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.ui.common.auth.CurrentUser;
import org.ovirt.engine.ui.common.uicommon.FrontendFailureEventListener;
import org.ovirt.engine.ui.uicommonweb.models.CommonModel;
import org.ovirt.engine.ui.uicommonweb.models.LoginModel;

import com.google.gwt.event.shared.EventBus;

/**
 * Manages the {@link CommonModel} instance shared by all model providers.
 */
public class CommonModelManager {

    private static CommonModel commonModel;

    /**
     * Creates new {@link CommonModel} instance and sets up necessary event listeners.
     * <p>
     * Should be called right after successful user authentication, before redirecting the user to the main section.
     */
    public static void init(final EventBus eventBus, final CurrentUser user, final LoginModel loginModel,
            final FrontendFailureEventListener frontendFailureEventListener) {
        commonModel = CommonModel.newInstance();

        commonModel.getSelectedItemChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                MainModelSelectionChangeEvent.fire(eventBus, commonModel.getSelectedItem());
            }
        });

        commonModel.getSignedOutEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                // Clear CommonModel reference after the user signs out
                commonModel = null;

                loginModel.resetAfterLogout();
                user.onUserLogout();
                frontendFailureEventListener.hide();
            }
        });
    }

    /**
     * Returns the {@link CommonModel} instance if the user is currently logged in, {@code null} otherwise.
     */
    public static CommonModel instance() {
        return commonModel;
    }

}
