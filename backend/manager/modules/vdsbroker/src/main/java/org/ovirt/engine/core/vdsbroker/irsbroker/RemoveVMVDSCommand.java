package org.ovirt.engine.core.vdsbroker.irsbroker;

import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.vdscommands.RemoveVMVDSCommandParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VDSExceptionBase;

public class RemoveVMVDSCommand<P extends RemoveVMVDSCommandParameters> extends IrsBrokerCommand<P> {
    public RemoveVMVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void ExecuteIrsBrokerCommand() {
        if (getParameters().getStorageDomainId().equals(Guid.Empty)) {
            status = getIrsProxy().removeVM(getParameters().getStoragePoolId().toString(),
                    getParameters().getVmGuid().toString());
        } else {
            status = getIrsProxy()
                    .removeVM(getParameters().getStoragePoolId().toString(),
                            getParameters().getVmGuid().toString(),
                            getParameters().getStorageDomainId().toString());
        }
        ProceedProxyReturnValue();
    }

    @Override
    protected void ProceedProxyReturnValue() {
        VdcBllErrors returnStatus = GetReturnValueFromStatus(getReturnStatus());
        switch (returnStatus) {
        case Done:
            return;
        default:
            VDSExceptionBase outEx = createDefaultConcreteException(getReturnStatus().mMessage);
            InitializeVdsError(returnStatus);
            outEx.setVdsError(getVDSReturnValue().getVdsError());
            throw outEx;
        }
    }

    @Override
    protected VDSExceptionBase createDefaultConcreteException(String errorMessage) {
        return new IrsOperationFailedNoFailoverException(errorMessage);
    }
}
