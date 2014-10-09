package org.ovirt.engine.core.vdsbroker.irsbroker;

import org.ovirt.engine.core.common.asynctasks.AsyncTaskCreationInfo;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskType;
import org.ovirt.engine.core.common.vdscommands.DestroyImageVDSCommandParameters;
import org.ovirt.engine.core.compat.Guid;

public class DestroyImageVDSCommand<P extends DestroyImageVDSCommandParameters> extends IrsCreateCommand<P> {

    public DestroyImageVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void ExecuteIrsBrokerCommand() {

        DestroyImageVDSCommandParameters params = getParameters();
        int imageListSize = params.getImageList().size();
        String[] volUUID = new String[imageListSize];
        int i = 0;
        for (Guid tempGuid : params.getImageList()) {
            volUUID[i++] = tempGuid.toString();
        }

        uuidReturn = getIrsProxy().deleteVolume(params.getStorageDomainId().toString(),
                                                params.getStoragePoolId().toString(),
                                                params.getImageGroupId().toString(),
                                                volUUID,
                                                String.valueOf(params.getPostZero()),
                                                String.valueOf(params.getForce()));

        ProceedProxyReturnValue();

        Guid taskID = new Guid(uuidReturn.mUuid);

        getVDSReturnValue()
                .setCreationInfo(
                        new AsyncTaskCreationInfo(taskID, AsyncTaskType.deleteVolume, getParameters()
                                .getStoragePoolId()));
    }
}
