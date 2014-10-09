package org.ovirt.engine.core.common.queries;

import org.ovirt.engine.core.compat.*;

public class GetTemplateMessagesParameters extends GetMessagesByIdParametersBase {
    private static final long serialVersionUID = 5138681016422927844L;

    public GetTemplateMessagesParameters(Guid vmId) {
        super(vmId);
    }

    public GetTemplateMessagesParameters() {
    }
}
