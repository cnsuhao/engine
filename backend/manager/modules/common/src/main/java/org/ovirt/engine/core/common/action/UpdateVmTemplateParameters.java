package org.ovirt.engine.core.common.action;

import javax.validation.Valid;

import org.ovirt.engine.core.common.businessentities.VmTemplate;

public class UpdateVmTemplateParameters extends VmTemplateParametersBase {
    private static final long serialVersionUID = 7250355162926369307L;
    @Valid
    private VmTemplate _vmTemplate;

    public UpdateVmTemplateParameters(VmTemplate vmTemplate) {
        _vmTemplate = vmTemplate;
    }

    public VmTemplate getVmTemplateData() {
        return _vmTemplate;
    }

    public UpdateVmTemplateParameters() {
    }
}
