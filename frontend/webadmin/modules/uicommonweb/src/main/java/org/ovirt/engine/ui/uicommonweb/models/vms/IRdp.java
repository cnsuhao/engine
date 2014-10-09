package org.ovirt.engine.ui.uicommonweb.models.vms;

import org.ovirt.engine.core.compat.Event;

public interface IRdp
{
    Event getDisconnectedEvent();

    String getAddress();

    void setAddress(String value);

    String getGuestID();

    void setGuestID(String value);

    boolean getUseLocalDrives();

    void setUseLocalDrives(boolean value);

    void Connect();
}
