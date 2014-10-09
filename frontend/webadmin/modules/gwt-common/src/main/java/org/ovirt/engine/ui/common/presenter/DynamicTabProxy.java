package org.ovirt.engine.ui.common.presenter;

import org.ovirt.engine.ui.common.gin.BaseClientGinjector;
import org.ovirt.engine.ui.common.widget.tab.DynamicTabData;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.inject.Provider;
import com.gwtplatform.common.client.StandardProvider;
import com.gwtplatform.mvp.client.RequestTabsHandler;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceWithGatekeeper;
import com.gwtplatform.mvp.client.proxy.TabContentProxyImpl;
import com.gwtplatform.mvp.client.proxy.TabContentProxyPlaceImpl;

/**
 * Presenter proxy implementation for {@link DynamicTabPresenter} subclasses.
 * <p>
 * Code for wrapped proxy implementation should match GWTP-generated {@code TabContentProxyPlaceImpl} subclasses for
 * presenters acting as tab content (presenters revealed within a {@code TabContainerPresenter}).
 *
 * @param <T>
 *            Presenter type.
 */
public abstract class DynamicTabProxy<T extends DynamicTabPresenter<?, ?>> extends TabContentProxyPlaceImpl<T> implements Provider<T> {

    public static class WrappedProxy<T extends DynamicTabPresenter<?, ?>> extends TabContentProxyImpl<T> {

        public WrappedProxy(PlaceManager placeManager, EventBus eventBus,
                Provider<T> presenterProvider, Type<RequestTabsHandler> requestTabsEventType,
                String label, float priority, String historyToken) {
            bind(placeManager, eventBus);
            this.requestTabsEventType = requestTabsEventType;
            this.tabData = new DynamicTabData(label, priority, historyToken);
            this.targetHistoryToken = historyToken;
            addRequestTabsHandler();
            this.presenter = new StandardProvider<T>(presenterProvider);
        }

    }

    private T presenter;

    public DynamicTabProxy(BaseClientGinjector ginjector,
            Type<RequestTabsHandler> requestTabsEventType,
            String label, float priority, String historyToken) {
        bind(ginjector.getPlaceManager(), ginjector.getEventBus());
        this.proxy = new WrappedProxy<T>(ginjector.getPlaceManager(), ginjector.getEventBus(),
                this, requestTabsEventType, label, priority, historyToken);
        this.place = new PlaceWithGatekeeper(historyToken, ginjector.getDefaultGatekeeper());

        // Create and bind presenter eagerly
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
                get();
            }
        });
    }

    @Override
    public void manualRevealFailed() {
        super.manualRevealFailed();
        placeManager.revealDefaultPlace();
    }

    @Override
    public final T get() {
        if (presenter == null) {
            presenter = createPresenter();
            presenter.bind();
        }

        return presenter;
    }

    /**
     * Instantiates the associated presenter.
     * <p>
     * This method is called when the presenter is requested for the first time (subsequent requests reuse the existing
     * presenter instance).
     */
    protected abstract T createPresenter();

}
