package org.ovirt.engine.core.bll.lsm;

import org.ovirt.engine.core.bll.AbstractSPMAsyncTaskHandler;
import org.ovirt.engine.core.bll.tasks.TaskHandlerCommand;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.LiveMigrateDiskParameters;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskType;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.vdscommands.SyncImageGroupDataVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSParametersBase;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.VmReplicateDiskParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.vdsbroker.ResourceManager;

public class VmReplicateDiskStartTaskHandler extends AbstractSPMAsyncTaskHandler<TaskHandlerCommand<? extends LiveMigrateDiskParameters>> {

    public VmReplicateDiskStartTaskHandler(TaskHandlerCommand<? extends LiveMigrateDiskParameters> cmd) {
        super(cmd);
    }

    @Override
    protected void beforeTask() {
        // Start disk migration
        VmReplicateDiskParameters migrationStartParams = new VmReplicateDiskParameters
                (getEnclosingCommand().getParameters().getVdsId(),
                        getEnclosingCommand().getParameters().getVmId(),
                        getEnclosingCommand().getParameters().getStoragePoolId().getValue(),
                        getEnclosingCommand().getParameters().getSourceStorageDomainId(),
                        getEnclosingCommand().getParameters().getTargetStorageDomainId(),
                        getEnclosingCommand().getParameters().getImageGroupID(),
                        getEnclosingCommand().getParameters().getDestinationImageId()
                );
        VDSReturnValue ret =
                ResourceManager.getInstance().runVdsCommand(VDSCommandType.VmReplicateDiskStart, migrationStartParams);

        if (!ret.getSucceeded()) {
            log.errorFormat("Failed VmReplicateDiskStart (Disk {0} , VM {1})",
                    getEnclosingCommand().getParameters().getImageGroupID(),
                    getEnclosingCommand().getParameters().getVmId());
            throw new VdcBLLException(ret.getVdsError().getCode(), ret.getVdsError().getMessage());
        }
    }

    @Override
    protected VDSCommandType getVDSCommandType() {
        return VDSCommandType.SyncImageGroupData;
    }

    @Override
    public AsyncTaskType getTaskType() {
        return AsyncTaskType.syncImageData;
    }

    @Override
    protected VDSParametersBase getVDSParameters() {
        return new SyncImageGroupDataVDSCommandParameters(
                getEnclosingCommand().getParameters().getStoragePoolId().getValue(),
                getEnclosingCommand().getParameters().getSourceStorageDomainId(),
                getEnclosingCommand().getParameters().getImageGroupID(),
                getEnclosingCommand().getParameters().getTargetStorageDomainId(),
                SyncImageGroupDataVDSCommandParameters.SYNC_TYPE_INTERNAL);
    }

    @Override
    protected void revertTask() {
        // Undo the replicateStart - use replicateFinish back to the source
        VmReplicateDiskParameters migrationStartParams = new VmReplicateDiskParameters
                (getEnclosingCommand().getParameters().getVdsId(),
                        getEnclosingCommand().getParameters().getVmId(),
                        getEnclosingCommand().getParameters().getStoragePoolId().getValue(),
                        getEnclosingCommand().getParameters().getSourceStorageDomainId(),
                        getEnclosingCommand().getParameters().getSourceStorageDomainId(),
                        getEnclosingCommand().getParameters().getImageGroupID(),
                        getEnclosingCommand().getParameters().getDestinationImageId()
                );
        ResourceManager.getInstance().runVdsCommand(VDSCommandType.VmReplicateDiskFinish, migrationStartParams);
    }

    @Override
    protected VDSCommandType getRevertVDSCommandType() {
        // VDSM handles the failure, so no action required here.
        return null;
    }

    @Override
    public AsyncTaskType getRevertTaskType() {
        // VDSM handles the failure, so no action required here.
        return null;
    }

    @Override
    protected VDSParametersBase getRevertVDSParameters() {
        // VDSM handles the failure, so no action required here.
        return null;
    }

    @Override
    protected VdcObjectType getTaskObjectType() {
        return VdcObjectType.VM;
    }

    @Override
    protected Guid[] getTaskObjects() {
        return new Guid[] { getEnclosingCommand().getParameters().getVmId() };
    }
}
