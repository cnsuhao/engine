package org.ovirt.engine.ui.userportal.section.main.presenter;

import org.ovirt.engine.ui.common.uicommon.model.SearchableModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.userportal.section.main.presenter.SearchableModelActivationEvent.SearchableModelActivationHandler;

import com.google.gwt.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.proxy.Proxy;

/**
 * Base class for presenters bound to UiCommon list models whose activation needs to be handled by the application.
 * <p>
 * When {@linkplain #onReveal revealed}, the associated UiCommon model will be activated, with any other models being
 * stopped. This ensures that only the model of the currently visible (revealed) presenter is active at the given time.
 *
 * @param <T>
 *            List model item type.
 * @param <M>
 *            List model type.
 * @param <V>
 *            View type.
 * @param <P>
 *            Proxy type.
 */
public abstract class AbstractModelActivationPresenter<T, M extends SearchableListModel, V extends View, P extends Proxy<?>> extends Presenter<V, P> {

    protected final SearchableModelProvider<T, M> modelProvider;

    public AbstractModelActivationPresenter(EventBus eventBus, V view, P proxy,
            final SearchableModelProvider<T, M> modelProvider) {
        super(eventBus, view, proxy);
        this.modelProvider = modelProvider;

        // Add handler for list model activation requests
        eventBus.addHandler(SearchableModelActivationEvent.getType(), new SearchableModelActivationHandler() {
            @Override
            public void onSearchableModelActivation(SearchableModelActivationEvent event) {
                SearchableListModel currentModel = modelProvider.getModel();

                if (event.getListModel() == currentModel) {
                    // Activate model
                    currentModel.getSearchCommand().Execute();
                } else {
                    // Stop model
                    currentModel.EnsureAsyncSearchStopped();
                }
            }
        });
    }

    @Override
    protected void onReveal() {
        super.onReveal();

        // Request activation of the associated list model
        SearchableModelActivationEvent.fire(this, modelProvider.getModel());
    }

}
