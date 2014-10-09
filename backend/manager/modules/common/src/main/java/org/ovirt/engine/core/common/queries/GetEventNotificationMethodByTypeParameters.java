package org.ovirt.engine.core.common.queries;

import org.ovirt.engine.core.common.*;

public class GetEventNotificationMethodByTypeParameters extends VdcQueryParametersBase {
    private static final long serialVersionUID = -6576190126259512799L;

    public GetEventNotificationMethodByTypeParameters(EventNotificationMethods mathodType) {
        setMethodType(mathodType);
    }

    private EventNotificationMethods privateMethodType = EventNotificationMethods.forValue(0);

    public EventNotificationMethods getMethodType() {
        return privateMethodType;
    }

    private void setMethodType(EventNotificationMethods value) {
        privateMethodType = value;
    }

    public GetEventNotificationMethodByTypeParameters() {
    }
}
