package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.queries.GetVdsByVdsIdParameters;
import org.ovirt.engine.core.common.queries.GetVmByVmIdParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.NGuid;

public class GetVdsCertificateSubjectByVmIdQuery <P extends GetVmByVmIdParameters> extends QueriesCommandBase<P> {
    public GetVdsCertificateSubjectByVmIdQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        getQueryReturnValue().setSucceeded(false);
        VdcQueryReturnValue returnValue = null;
        Guid vmId = getParameters().getId();
        if (vmId != null) {
            VM vm = getDbFacade().getVmDao().get(vmId);
            if (vm != null) {
                NGuid vdsId = vm.getRunOnVds();
                if (vdsId != null) {
                    returnValue = Backend.getInstance().runInternalQuery(VdcQueryType.GetVdsCertificateSubjectByVdsId, new GetVdsByVdsIdParameters(vdsId.getValue()));
                }
            }
        }
        if (returnValue != null) {
            getQueryReturnValue().setSucceeded(returnValue.getSucceeded());
            getQueryReturnValue().setReturnValue(returnValue.getReturnValue());
        }
    }
}
