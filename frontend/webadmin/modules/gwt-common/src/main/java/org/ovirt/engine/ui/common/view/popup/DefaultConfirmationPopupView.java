package org.ovirt.engine.ui.common.view.popup;

import java.util.ArrayList;

import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.presenter.popup.DefaultConfirmationPopupPresenterWidget;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class DefaultConfirmationPopupView extends AbstractConfirmationPopupView implements DefaultConfirmationPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<ConfirmationModel, DefaultConfirmationPopupView> {
        Driver driver = GWT.create(Driver.class);
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, DefaultConfirmationPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<DefaultConfirmationPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField
    VerticalPanel descriptionPanel;

    @Inject
    public DefaultConfirmationPopupView(EventBus eventBus, CommonApplicationResources resources) {
        super(eventBus, resources);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        Driver.driver.initialize(this);
    }

    @Override
    public void edit(final ConfirmationModel object) {
        Driver.driver.edit(object);

        object.getItemsChangedEvent().addListener(new IEventListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                ArrayList<String> items = (ArrayList<String>) object.getItems();

                for (String item : items) {
                    descriptionPanel.add(new Label(item));
                }
            }
        });
    }

    @Override
    public ConfirmationModel flush() {
        return Driver.driver.flush();
    }

}
