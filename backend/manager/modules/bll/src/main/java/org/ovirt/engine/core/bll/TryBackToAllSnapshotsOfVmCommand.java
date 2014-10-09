package org.ovirt.engine.core.bll;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.snapshots.SnapshotsManager;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.ImagesContainterParametersBase;
import org.ovirt.engine.core.common.action.TryBackToAllSnapshotsOfVmParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.core.common.businessentities.Snapshot.SnapshotStatus;
import org.ovirt.engine.core.common.businessentities.Snapshot.SnapshotType;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.SnapshotDao;
import org.ovirt.engine.core.utils.linq.Function;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@LockIdNameAttribute
@NonTransactiveCommandAttribute(forceCompensation = true)
public class TryBackToAllSnapshotsOfVmCommand<T extends TryBackToAllSnapshotsOfVmParameters> extends VmCommand<T> {

    private static final long serialVersionUID = 2636628918352438919L;

    private final SnapshotsManager snapshotsManager = new SnapshotsManager();

    /**
     * Constructor for command creation when compensation is applied on startup
     * @param commandId
     */
    protected TryBackToAllSnapshotsOfVmCommand(Guid commandId) {
        super(commandId);
    }

    public TryBackToAllSnapshotsOfVmCommand(T parameters) {
        super(parameters);
        parameters.setEntityId(getVmId());
    }

    @Override
    public Map<String, String> getJobMessageProperties() {
        if (jobProperties == null) {
            jobProperties = super.getJobMessageProperties();
            Snapshot snapshot = getSnapshotDao().get(getParameters().getDstSnapshotId());
            if (snapshot != null) {
                jobProperties.put(VdcObjectType.Snapshot.name().toLowerCase(), snapshot.getDescription());
            }
        }
        return jobProperties;
    }

    @Override
    protected void endWithFailure() {
        Guid previouslyActiveSnapshotId =
                getSnapshotDao().getId(getVmId(), SnapshotType.PREVIEW, SnapshotStatus.LOCKED);
        getSnapshotDao().remove(previouslyActiveSnapshotId);
        getSnapshotDao().remove(getSnapshotDao().getId(getVmId(), SnapshotType.ACTIVE));

        snapshotsManager.addActiveSnapshot(previouslyActiveSnapshotId, getVm(), getCompensationContext());

        super.endWithFailure();
    }

    protected SnapshotDao getSnapshotDao() {
        return DbFacade.getInstance().getSnapshotDao();
    }

    @Override
    protected void endSuccessfully() {
        getVmStaticDAO().incrementDbGeneration(getVm().getId());
        endActionOnDisks();

        if (getVm() != null) {
            VmHandler.unlockVm(getVm(), getCompensationContext());
            restoreVmConfigFromSnapshot();
        } else {
            setCommandShouldBeLogged(false);
            log.warn("VmCommand::EndVmCommand: Vm is null - not performing EndAction on Vm");
        }

        setSucceeded(true);
    }

    private void restoreVmConfigFromSnapshot() {
        getSnapshotDao().updateStatus(getParameters().getDstSnapshotId(), SnapshotStatus.IN_PREVIEW);
        getSnapshotDao().updateStatus(getSnapshotDao().getId(getVm().getId(),
                SnapshotType.PREVIEW,
                SnapshotStatus.LOCKED),
                SnapshotStatus.OK);

        snapshotsManager.attempToRestoreVmConfigurationFromSnapshot(getVm(),
                getSnapshotDao().get(getParameters().getDstSnapshotId()),
                getSnapshotDao().getId(getVm().getId(), SnapshotType.ACTIVE),
                getCompensationContext());
    }

    @Override
    protected void executeVmCommand() {

        final Guid newActiveSnapshotId = Guid.NewGuid();
        final List<DiskImage> images = DbFacade
                .getInstance()
                .getDiskImageDao()
                .getAllSnapshotsForVmSnapshot(getParameters().getDstSnapshotId());
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
            @Override
            public Void runInTransaction() {
                Snapshot previousActiveSnapshot = getSnapshotDao().get(getVmId(), SnapshotType.ACTIVE);
                getCompensationContext().snapshotEntity(previousActiveSnapshot);
                Guid previousActiveSnapshotId = previousActiveSnapshot.getId();
                getSnapshotDao().remove(previousActiveSnapshotId);
                snapshotsManager.addSnapshot(previousActiveSnapshotId,
                        "Active VM before the preview",
                        SnapshotType.PREVIEW,
                        getVm(),
                        getCompensationContext());
                snapshotsManager.addActiveSnapshot(newActiveSnapshotId, getVm(), getCompensationContext());
                snapshotsManager.removeAllIllegalDisks(previousActiveSnapshotId, getVm().getId());
                //if there are no images there's no reason the save the compensation data to DB as
                //the update is being executed in the same transaction so we can restore the vm config and end the command.
                if (!images.isEmpty()) {
                    getCompensationContext().stateChanged();
                } else {
                    getVmStaticDAO().incrementDbGeneration(getVm().getId());
                    restoreVmConfigFromSnapshot();
                }
                return null;
            }
        });

        if (images.size() > 0) {
            VmHandler.LockVm(getVm().getDynamicData(), getCompensationContext());
            freeLock();
            TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
                @Override
                public Void runInTransaction() {
                    for (DiskImage image : images) {
                        ImagesContainterParametersBase tempVar = new ImagesContainterParametersBase(image.getImageId());
                        tempVar.setParentCommand(VdcActionType.TryBackToAllSnapshotsOfVm);
                        tempVar.setVmSnapshotId(newActiveSnapshotId);
                        tempVar.setEntityId(getParameters().getEntityId());
                        tempVar.setParentParameters(getParameters());
                        tempVar.setQuotaId(image.getQuotaId());
                        ImagesContainterParametersBase p = tempVar;
                        VdcReturnValueBase vdcReturnValue =
                                Backend.getInstance().runInternalAction(VdcActionType.TryBackToSnapshot,
                                        p,
                                        ExecutionHandler.createDefaultContexForTasks(getExecutionContext()));
                        getParameters().getImagesParameters().add(p);

                        if (vdcReturnValue.getSucceeded()) {
                            getTaskIdList().addAll(vdcReturnValue.getInternalTaskIdList());
                        } else if (vdcReturnValue.getFault() != null) {
                            // if we have a fault, forward it to the user
                            throw new VdcBLLException(vdcReturnValue.getFault().getError(), vdcReturnValue.getFault()
                                    .getMessage());
                        } else {
                            log.error("Cannot create snapshot");
                            throw new VdcBLLException(VdcBllErrors.IRS_IMAGE_STATUS_ILLEGAL);
                        }
                    }
                    return null;
                }
            });
        }
        setSucceeded(true);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        switch (getActionState()) {
        case EXECUTE:
            return getSucceeded() ? AuditLogType.USER_TRY_BACK_TO_SNAPSHOT
                    : AuditLogType.USER_FAILED_TRY_BACK_TO_SNAPSHOT;

        case END_SUCCESS:
            return getSucceeded() ? AuditLogType.USER_TRY_BACK_TO_SNAPSHOT_FINISH_SUCCESS
                    : AuditLogType.USER_TRY_BACK_TO_SNAPSHOT_FINISH_FAILURE;

        default:
            return AuditLogType.USER_TRY_BACK_TO_SNAPSHOT_FINISH_FAILURE;
        }
    }

    @Override
    protected boolean canDoAction() {
        VmHandler.updateDisksFromDb(getVm());
        Collection<DiskImage> diskImages =
                ImagesHandler.filterImageDisks(getVm().getDiskMap().values(), false, true);
        DiskImage vmDisk = LinqUtils.first(diskImages);
        boolean result = true;

        Snapshot snapshot = getSnapshotDao().get(getParameters().getDstSnapshotId());
        SnapshotsValidator snapshotsValidator = new SnapshotsValidator();
        result = result && validate(snapshotsValidator.vmNotDuringSnapshot(getVmId()))
                && validate(snapshotsValidator.snapshotExists(snapshot))
                && validate(snapshotsValidator.snapshotNotBroken(snapshot));

        if (vmDisk != null) {
            result =
                    result
                            && ImagesHandler.PerformImagesChecks(getVm(),
                                    getReturnValue().getCanDoActionMessages(),
                                    getVm().getStoragePoolId(),
                                    Guid.Empty,
                                    true,
                                    true,
                                    false,
                                    false,
                                    true,
                                    true,
                                    true, true, getVm().getDiskMap().values());
        }
        if (result && LinqUtils.foreach(diskImages, new Function<DiskImage, Guid>() {
            @Override
            public Guid eval(DiskImage disk) {
                return disk.getvm_snapshot_id().getValue();
            }
        }).contains(getParameters().getDstSnapshotId())) {
            result = false;
            addCanDoActionMessage(VdcBllMessages.CANNOT_PREIEW_CURRENT_IMAGE);
        }

        if (!result) {
            addCanDoActionMessage(VdcBllMessages.VAR__ACTION__PREVIEW);
            addCanDoActionMessage(VdcBllMessages.VAR__TYPE__SNAPSHOT);
        }
        return result;
    }

    @Override
    protected VdcActionType getChildActionType() {
        return VdcActionType.TryBackToSnapshot;
    }

    @Override
    protected Map<String, String> getExclusiveLocks() {
        return Collections.singletonMap(getVmId().toString(), LockingGroup.VM.name());
    }
}
