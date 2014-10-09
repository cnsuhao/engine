package org.ovirt.engine.core.common;

import org.ovirt.engine.core.common.mode.ApplicationMode;


public enum EventNotificationEntity {
    UNKNOWN(ApplicationMode.AllModes),
    Host(ApplicationMode.AllModes),
    Vm(ApplicationMode.VirtOnly),
    Storage(ApplicationMode.VirtOnly),
    Engine(ApplicationMode.AllModes),
    GlusterVolume(ApplicationMode.GlusterOnly),
    DWH(ApplicationMode.VirtOnly);

    private int availableInModes;

    private EventNotificationEntity(ApplicationMode applicationMode) {
        this.availableInModes = applicationMode.getValue();
    }

    public int getValue() {
        return this.ordinal();
    }

    public static EventNotificationEntity forValue(int value) {
        return values()[value];
    }

    public int getAvailableInModes() {
        return availableInModes;
    }
}
