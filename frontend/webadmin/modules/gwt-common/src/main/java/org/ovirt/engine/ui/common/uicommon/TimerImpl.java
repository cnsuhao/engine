package org.ovirt.engine.ui.common.uicommon;

import java.util.logging.Logger;

import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.ui.uicommonweb.ITimer;
import org.ovirt.engine.ui.uicommonweb.ProvideTickEvent;

import com.google.gwt.user.client.Timer;

public class TimerImpl implements ITimer {

    private final Event tickEvent;
    private static Logger logger = Logger.getLogger(TimerImpl.class.getName());
    private int interval;

    private final Timer timer = TimerFactory.factoryTimer("UICommon Timer", new Timer() { //$NON-NLS-1$
        @Override
        public void run() {
            logger.info("Timer execution"); //$NON-NLS-1$
            tickEvent.raise(this, EventArgs.Empty);
        }
    });

    public TimerImpl() {
        tickEvent = new Event(ProvideTickEvent.Definition);
    }

    @Override
    public int getInterval() {
        return 0;
    }

    @Override
    public void setInterval(int value) {
        logger.info("Timer interval set to " + value + " by UICommon"); //$NON-NLS-1$ //$NON-NLS-2$
        interval = value;
    }

    @Override
    public void start() {
        logger.info("Timer started by UICommon"); //$NON-NLS-1$
        timer.scheduleRepeating(interval);
    }

    @Override
    public void stop() {
        logger.info("Timer stopped by UICommon"); //$NON-NLS-1$
        timer.cancel();
    }

    @Override
    public Event getTickEvent() {
        return tickEvent;
    }

}
