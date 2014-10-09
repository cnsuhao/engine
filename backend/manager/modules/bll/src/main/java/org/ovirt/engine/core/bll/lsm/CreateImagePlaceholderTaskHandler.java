package org.ovirt.engine.core.bll.lsm;

import org.ovirt.engine.core.bll.AbstractSPMAsyncTaskHandler;
import org.ovirt.engine.core.bll.ImagesHandler;
import org.ovirt.engine.core.bll.tasks.TaskHandlerCommand;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.LiveMigrateDiskParameters;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskType;
import org.ovirt.engine.core.common.businessentities.ImageStatus;
import org.ovirt.engine.core.common.vdscommands.DeleteImageGroupVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.TargetDomainImageGroupVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSParametersBase;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

public class CreateImagePlaceholderTaskHandler extends AbstractSPMAsyncTaskHandler<TaskHandlerCommand<? extends LiveMigrateDiskParameters>> {

    public CreateImagePlaceholderTaskHandler(TaskHandlerCommand<? extends LiveMigrateDiskParameters> cmd) {
        super(cmd);
    }

    @Override
    protected void beforeTask() {
        // No-op before the sync image command in the database side
    }

    @Override
    protected VDSCommandType getVDSCommandType() {
        return VDSCommandType.CloneImageGroupStructure;
    }

    @Override
    public AsyncTaskType getTaskType() {
        return AsyncTaskType.cloneImageStructure;
    }

    @Override
    protected VDSParametersBase getVDSParameters() {
        return new TargetDomainImageGroupVDSCommandParameters(
                getEnclosingCommand().getParameters().getStoragePoolId().getValue(),
                getEnclosingCommand().getParameters().getSourceStorageDomainId(),
                getEnclosingCommand().getParameters().getImageGroupID(),
                getEnclosingCommand().getParameters().getTargetStorageDomainId());
    }

    @Override
    protected VdcObjectType getTaskObjectType() {
        return VdcObjectType.VM;
    }

    @Override
    protected Guid[] getTaskObjects() {
        return new Guid[] { getEnclosingCommand().getParameters().getVmId() };
    }

    @Override
    protected void revertTask() {
        // Unlock the image we left locked
        ImagesHandler.updateImageStatus(getEnclosingCommand().getParameters().getImageId(), ImageStatus.OK);
        ImagesHandler.updateImageStatus(getEnclosingCommand().getParameters().getDestinationImageId(), ImageStatus.OK);
    }

    @Override
    protected VDSCommandType getRevertVDSCommandType() {
        return VDSCommandType.DeleteImageGroup;
    }

    @Override
    public AsyncTaskType getRevertTaskType() {
        return AsyncTaskType.deleteImage;
    }

    @Override
    protected VDSParametersBase getRevertVDSParameters() {
        return new DeleteImageGroupVDSCommandParameters(
                getEnclosingCommand().getParameters().getStoragePoolId().getValue(),
                getEnclosingCommand().getParameters().getTargetStorageDomainId(),
                getEnclosingCommand().getParameters().getImageGroupID(),
                DbFacade.getInstance()
                        .getDiskImageDao()
                        .get(getEnclosingCommand().getParameters().getDestinationImageId())
                        .isWipeAfterDelete(),
                getEnclosingCommand().getParameters().getForceDelete(),
                null);
    }
}
