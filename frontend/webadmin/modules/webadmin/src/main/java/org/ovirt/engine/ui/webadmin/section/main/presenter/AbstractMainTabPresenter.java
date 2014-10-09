package org.ovirt.engine.ui.webadmin.section.main.presenter;

import org.ovirt.engine.ui.common.uicommon.model.CommonModelManager;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.uicommon.model.MainModelSelectionChangeEvent;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;

import com.google.gwt.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ProxyEvent;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;

/**
 * Base class for main tab presenters.
 *
 * @param <T>
 *            Table row data type.
 * @param <M>
 *            Main model type.
 * @param <V>
 *            View type.
 * @param <P>
 *            Proxy type.
 */
public abstract class AbstractMainTabPresenter<T, M extends SearchableListModel, V extends View, P extends ProxyPlace<?>> extends Presenter<V, P> {

    protected final PlaceManager placeManager;
    protected final MainModelProvider<T, M> modelProvider;

    public AbstractMainTabPresenter(EventBus eventBus, V view, P proxy,
            PlaceManager placeManager, MainModelProvider<T, M> modelProvider) {
        super(eventBus, view, proxy);
        this.placeManager = placeManager;
        this.modelProvider = modelProvider;
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainTabPanelPresenter.TYPE_SetTabContent, this);
    }

    /**
     * We use manual reveal since we want to prevent users from accessing this presenter when the corresponding main
     * model is not available.
     */
    @Override
    public boolean useManualReveal() {
        return true;
    }

    @Override
    protected void onReveal() {
        super.onReveal();

        // Notify model provider that the tab has been revealed
        modelProvider.onMainTabSelected();
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        super.prepareFromRequest(request);

        // Reveal presenter only when the main model is available
        if (getModel().getIsAvailable()) {
            getProxy().manualReveal(this);
        } else {
            getProxy().manualRevealFailed();
            revealActiveMainModelPresenter();
        }
    }

    protected M getModel() {
        return modelProvider.getModel();
    }

    @ProxyEvent
    public void onMainModelSelectionChange(MainModelSelectionChangeEvent event) {
        if (event.getMainModel() == getModel()) {
            if (event.getMainModel().getIsAvailable()) {
                // Reveal main tab place when the corresponding model is selected
                placeManager.revealPlace(getMainTabRequest());
            } else {
                revealActiveMainModelPresenter();
            }
        }
    }

    void revealActiveMainModelPresenter() {
        MainModelSelectionChangeEvent.fire(getEventBus(), CommonModelManager.instance().getSelectedItem());
    }

    /**
     * Returns the place request associated with this main tab presenter.
     */
    protected abstract PlaceRequest getMainTabRequest();

    /**
     * Controls the sub tab panel visibility.
     */
    protected void setSubTabPanelVisible(boolean subTabPanelVisible) {
        UpdateMainContentLayoutEvent.fire(this, subTabPanelVisible);
    }

}
