package org.ovirt.engine.core.bll;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.command.utils.StorageDomainSpaceChecker;
import org.ovirt.engine.core.bll.snapshots.SnapshotsValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.MoveVmParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.ImageOperation;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StoragePoolIsoMapId;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.vdscommands.IsVmDuringInitiatingVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

@Deprecated
@NonTransactiveCommandAttribute(forceCompensation = true)
public class MoveVmCommand<T extends MoveVmParameters> extends MoveOrCopyTemplateCommand<T> {

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected MoveVmCommand(Guid commandId) {
        super(commandId);
    }

    public MoveVmCommand(T parameters) {
        super(parameters);
        setVmId(parameters.getContainerId());
        parameters.setEntityId(getVmId());
        setStoragePoolId(getVm().getStoragePoolId());
    }

    @Override
    protected ImageOperation getMoveOrCopyImageOperation() {
        return ImageOperation.Move;
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__MOVE);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__VM);
    }

    @Override
    protected boolean canDoAction() {
        boolean retValue = true;
        if (getVm() == null) {
            retValue = false;
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_VM_NOT_FOUND);
        } else {
            setDescription(getVmName());
        }

        retValue = retValue && validate(new SnapshotsValidator().vmNotDuringSnapshot(getVmId()));

        // check that vm is down and images are ok
        // not checking storage domain, there is a check in
        // CheckTemplateInStorageDomain later
        VmHandler.updateDisksFromDb(getVm());
        List<DiskImage> diskImages = ImagesHandler.filterImageDisks(getVm().getDiskMap().values(), false, false);
        retValue = retValue && ImagesHandler.PerformImagesChecks(getVm(),
                                getReturnValue().getCanDoActionMessages(),
                                getVm().getStoragePoolId(),
                                Guid.Empty,
                                false,
                                true,
                                true,
                                true,
                                true,
                                true,
                                false,
                                true,
                                diskImages);
        setStoragePoolId(getVm().getStoragePoolId());

        ensureDomainMap(diskImages, getParameters().getStorageDomainId());
        for(DiskImage disk : diskImages) {
            imageFromSourceDomainMap.put(disk.getId(), disk);
        }

        retValue = retValue && checkTemplateInStorageDomain(diskImages);

        if (retValue
                && DbFacade.getInstance()
                        .getStoragePoolIsoMapDao()
                        .get(new StoragePoolIsoMapId(getStorageDomain().getId(),
                                getVm().getStoragePoolId())) == null) {
            retValue = false;
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_POOL_NOT_MATCH);
        }

        if (retValue && getVm().getDiskMap().size() == 0) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_VM_HAS_NO_DISKS);
            retValue = false;
        }

        // update vm snapshots for storage free space check
        ImagesHandler.fillImagesBySnapshots(getVm());
        return retValue && destinationHasSpace();
    }

    private boolean destinationHasSpace() {
        if (!StorageDomainSpaceChecker.hasSpaceForRequest(getStorageDomain(),
                (int) getVm().getActualDiskWithSnapshotsSize())) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW);
            return false;
        }
        return true;
    }

    protected boolean checkTemplateInStorageDomain(List<DiskImage> diskImages) {
        boolean retValue = checkStorageDomain() && checkStorageDomainStatus(StorageDomainStatus.Active)
                && checkIfDisksExist(diskImages);
        if (retValue && !VmTemplateHandler.BlankVmTemplateId.equals(getVm().getVmtGuid())) {
            List<DiskImage> imageList =
                    ImagesHandler.filterImageDisks(DbFacade.getInstance().getDiskDao().getAllForVm(getVm().getVmtGuid()),
                            false,
                            false);
            Map<Guid, DiskImage> templateImagesMap = new HashMap<Guid, DiskImage>();
            for (DiskImage image : imageList) {
                templateImagesMap.put(image.getImageId(), image);
            }
            for (DiskImage image : diskImages) {
                if (templateImagesMap.containsKey(image.getit_guid())) {
                    if (!templateImagesMap.get(image.getit_guid())
                            .getstorage_ids()
                            .contains(getParameters().getStorageDomainId())) {
                        retValue = false;
                        addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_TEMPLATE_NOT_FOUND_ON_DESTINATION_DOMAIN);
                        break;
                    }
                }
            }
        }
        return retValue;
    }

    @Override
    protected void executeCommand() {
        VM vm = getVm();
        if (vm.getStatus() != VMStatus.Down) {
            throw new VdcBLLException(VdcBllErrors.IRS_IMAGE_STATUS_ILLEGAL);
        }
        // Check if vm is initializing to run or already running - if it is in
        // such state,
        // we cannot move the vm
        boolean isVmDuringInit = ((Boolean) Backend
                .getInstance()
                .getResourceManager()
                .RunVdsCommand(VDSCommandType.IsVmDuringInitiating,
                        new IsVmDuringInitiatingVDSCommandParameters(vm.getId())).getReturnValue()).booleanValue();

        if (isVmDuringInit) {
            log.errorFormat("VM {0} must be down for Move VM to be successfuly executed", vm.getVmName());
            setActionReturnValue(vm.getStatus());
            setSucceeded(false);
            return;
        }

        VmHandler.LockVm(vm.getDynamicData(), getCompensationContext());
        moveOrCopyAllImageGroups();

        setSucceeded(true);

    }

    @Override
    protected void incrementDbGeneration() {
        getVmStaticDAO().incrementDbGeneration(getVm().getId());
    }

    @Override
    protected void moveOrCopyAllImageGroups() {
        moveOrCopyAllImageGroups(getVmId(), getVm().getDiskList());
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        switch (getActionState()) {
        case EXECUTE:
            return getSucceeded() ? AuditLogType.USER_MOVED_VM : AuditLogType.USER_FAILED_MOVE_VM;
        case END_SUCCESS:
            return getSucceeded() ? AuditLogType.USER_MOVED_VM_FINISHED_SUCCESS
                    : AuditLogType.USER_MOVED_VM_FINISHED_FAILURE;
        default:
            return AuditLogType.USER_MOVED_VM_FINISHED_FAILURE;
        }
    }

    protected void endMoveVmCommand() {
        boolean vmExists = (getVm() != null);
        if (vmExists) {
            incrementDbGeneration();
        }

        endActionOnAllImageGroups();

        if (vmExists) {
            VmHandler.UnLockVm(getVm());

            VmHandler.updateDisksFromDb(getVm());
        }

        else {
            setCommandShouldBeLogged(false);
            log.warn("MoveVmCommand::EndMoveVmCommand: Vm is null - not performing full EndAction");
        }

        setSucceeded(true);
    }

    @Override
    protected void endSuccessfully() {
        endMoveVmCommand();
    }

    @Override
    protected void endWithFailure() {
        endMoveVmCommand();
    }

    @Override
    protected VdcActionType getImagesActionType() {
        return VdcActionType.MoveOrCopyImageGroup;
    }
}
