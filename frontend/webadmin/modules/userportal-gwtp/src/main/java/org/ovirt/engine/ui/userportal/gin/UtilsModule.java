package org.ovirt.engine.ui.userportal.gin;

import org.ovirt.engine.ui.userportal.section.login.presenter.ConnectAutomaticallyProvider;
import org.ovirt.engine.ui.userportal.utils.ConnectAutomaticallyManager;
import org.ovirt.engine.ui.userportal.utils.ConsoleManager;
import org.ovirt.engine.ui.userportal.widget.basic.ConsoleUtils;
import org.ovirt.engine.ui.userportal.widget.basic.MainTabBasicListItemMessagesTranslator;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Singleton;

public class UtilsModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(ConsoleUtils.class).in(Singleton.class);
        bind(MainTabBasicListItemMessagesTranslator.class).in(Singleton.class);
        bind(ConnectAutomaticallyProvider.class).in(Singleton.class);
        bind(ConnectAutomaticallyManager.class).in(Singleton.class);
        bind(ConsoleManager.class).in(Singleton.class);
    }

}
