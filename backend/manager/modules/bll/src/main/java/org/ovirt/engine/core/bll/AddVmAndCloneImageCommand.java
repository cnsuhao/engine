package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.validator.StorageDomainValidator;
import org.ovirt.engine.core.common.action.MoveOrCopyImageGroupParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.VmManagementParametersBase;
import org.ovirt.engine.core.common.businessentities.CopyVolumeType;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.ImageOperation;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

/**
 * This abstract class holds helper methods for concrete command classes that require to add a VM and clone an image in
 * the process
 *
 * @param <T>
 */
public abstract class AddVmAndCloneImageCommand<T extends VmManagementParametersBase> extends AddVmCommand<T> {

    protected AddVmAndCloneImageCommand(Guid commandId) {
        super(commandId);
    }

    public AddVmAndCloneImageCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected boolean validateIsImagesOnDomains() {
        return true;
    }

    protected void copyDiskImage(
            DiskImage diskImage,
            Guid srcStorageDomainId,
            Guid destStorageDomainId,
            VdcActionType parentCommandType) {
        DiskImage newDiskImage = cloneDiskImage(getVmId(),
                destStorageDomainId,
                Guid.NewGuid(),
                Guid.NewGuid(),
                diskImage);
        ImagesHandler.setDiskAlias(newDiskImage, getVm());
        MoveOrCopyImageGroupParameters parameters = createCopyParameters(newDiskImage,
                srcStorageDomainId,
                diskImage.getId(),
                diskImage.getImageId(), parentCommandType);
        VdcReturnValueBase result = executeChildCopyingCommand(parameters);
        handleCopyResult(newDiskImage, parameters, result);
    }

    @Override
    protected void removeVmRelatedEntitiesFromDb() {
        removeVmImages();
        super.removeVmRelatedEntitiesFromDb();
    }

    private void removeVmImages() {
        // Remove vm images, in case they were not already removed by child commands
        ArrayList<VdcActionParametersBase> imageParams = getParameters().getImagesParameters();
        if (imageParams != null) {
            for (VdcActionParametersBase param : imageParams) {
                DiskImage diskImage = getDiskImageToRemoveByParam((MoveOrCopyImageGroupParameters) param);
                if (diskImage != null) {
                    ImagesHandler.removeDiskImage(diskImage, getVmId());
                }
            }
        }
    }

    @Override
    protected Collection<DiskImage> getImagesToCheckDestinationStorageDomains() {
        return getDiskImagesToBeCloned();
    }

    protected MoveOrCopyImageGroupParameters createCopyParameters(DiskImage diskImage,
            Guid srcStorageDomainId,
            Guid srcImageGroupId,
            Guid srcImageId, VdcActionType parentCommandType) {
        MoveOrCopyImageGroupParameters params =
                new MoveOrCopyImageGroupParameters(getVmId(),
                        srcImageGroupId,
                        srcImageId,
                        diskImage.getId(),
                        diskImage.getImageId(),
                        diskImage.getstorage_ids().get(0),
                        ImageOperation.Copy);
        params.setAddImageDomainMapping(false);
        params.setCopyVolumeType(CopyVolumeType.LeafVol);
        params.setVolumeFormat(diskImage.getvolume_format());
        params.setVolumeType(diskImage.getvolume_type());
        params.setUseCopyCollapse(true);
        params.setSourceDomainId(srcStorageDomainId);
        params.setWipeAfterDelete(diskImage.isWipeAfterDelete());
        params.setParentParameters(getParameters());
        params.setParentCommand(parentCommandType);
        return params;
    }

    @Override
    protected boolean canDoAction() {
        boolean retValue = false;
        if (super.canDoAction()) {
            for (DiskImage diskImage : getDiskImagesToBeCloned()) {
                retValue = checkImageConfiguration(diskImage);
                if (!retValue) {
                    break;
                }
            }
            retValue = true;
        }
        return retValue;
    }

    protected abstract boolean checkImageConfiguration(DiskImage diskImage);

    protected DiskImage cloneDiskImage(Guid newVmId,
            Guid storageDomainId,
            Guid newImageGroupId,
            Guid newImageGuid,
            DiskImage srcDiskImage) {
        DiskImage retDiskImage = DiskImage.copyOf(srcDiskImage);
        retDiskImage.setImageId(newImageGuid);
        retDiskImage.setParentId(Guid.Empty);
        retDiskImage.setit_guid(Guid.Empty);
        retDiskImage.setvm_snapshot_id(getVmSnapshotId());
        retDiskImage.setId(newImageGroupId);
        retDiskImage.setlast_modified_date(new Date());
        retDiskImage.setvolume_format(srcDiskImage.getvolume_format());
        retDiskImage.setvolume_type(srcDiskImage.getvolume_type());
        ArrayList<Guid> storageIds = new ArrayList<Guid>();
        storageIds.add(storageDomainId);
        retDiskImage.setstorage_ids(storageIds);
        return retDiskImage;
    }

    /**
     * Handle the result of copying the image
     *
     * @param copiedDiskImage
     *            disk image that represents the copied image at VDSM
     * @param parameters
     *            parameters for the child command that executes the copy at VDSM
     * @param result
     *            result of execution of child command
     */
    protected void handleCopyResult(DiskImage copiedDiskImage,
            VdcActionParametersBase parameters,
            VdcReturnValueBase result) {
        getParameters().getImagesParameters().add(parameters);
        // If a copy cannot be made, abort
        if (!result.getSucceeded()) {
            throw new VdcBLLException(VdcBllErrors.VolumeCreationError);
        } else {
            ImagesHandler.addDiskImageWithNoVmDevice(copiedDiskImage);
            getTaskIdList().addAll(result.getInternalTaskIdList());
            newDiskImages.add(copiedDiskImage);
        }
    }

    /**
     * Executes the child command responsible for the image copying
     * @param parameters
     *            parameters for copy
     * @return
     */
    protected VdcReturnValueBase executeChildCopyingCommand(VdcActionParametersBase parameters) {
        VdcReturnValueBase result = Backend.getInstance().runInternalAction(
                getChildActionType(),
                        parameters,
                        ExecutionHandler.createDefaultContexForTasks(getExecutionContext()));
        return result;
    }

    @Override
    protected boolean buildAndCheckDestStorageDomains() {
        if (diskInfoDestinationMap.isEmpty()) {
            List<storage_domains> domains =
                    DbFacade.getInstance()
                            .getStorageDomainDao()
                            .getAllForStoragePool(getStoragePoolId().getValue());
            Map<Guid, storage_domains> storageDomainsMap = new HashMap<Guid, storage_domains>();
            for (storage_domains storageDomain : domains) {
                StorageDomainValidator validator = new StorageDomainValidator(storageDomain);
                ArrayList<String> messages = new ArrayList<String>();
                if (validator.isDomainExistAndActive(messages) && validator.domainIsValidDestination(messages)) {
                    storageDomainsMap.put(storageDomain.getId(), storageDomain);
                }
            }
            for (Disk disk : getDiskImagesToBeCloned()) {
                DiskImage image = (DiskImage) disk;
                for (Guid storageId : image.getstorage_ids()) {
                    if (storageDomainsMap.containsKey(storageId)) {
                        diskInfoDestinationMap.put(image.getId(), image);
                        break;
                    }
                }
            }
            if (getDiskImagesToBeCloned().size() != diskInfoDestinationMap.size()) {
                logErrorOneOrMoreActiveDomainsAreMissing();
                return false;
            }
           List<Guid> storageDomainDest = new ArrayList<Guid>();
            for (DiskImage diskImage : diskInfoDestinationMap.values()) {
                Guid storageDomainId = diskImage.getstorage_ids().get(0);
                if (storageDomainDest.contains(storageDomainId)) {
                    destStorages.put(storageDomainId, storageDomainsMap.get(storageDomainId));
                }
                storageDomainDest.add(storageDomainId);
            }
            return true;
        }
        return super.buildAndCheckDestStorageDomains();
    }

    /**
     * Logs error if one or more active domains are missing for disk images
     */
    protected abstract void logErrorOneOrMoreActiveDomainsAreMissing();

    /**
     * Returns collection of DiskImage objects to use for construction of the imageTODestionationDomainMap
     *
     * @return
     */
    protected abstract Collection<DiskImage> getDiskImagesToBeCloned();

    protected DiskImage getDiskImageToRemoveByParam(MoveOrCopyImageGroupParameters param) {
        Guid imageGroupId = param.getDestImageGroupId();
        Guid imageId = param.getDestinationImageId();
        DiskImage diskImage = new DiskImage();
        diskImage.setId(imageGroupId);
        diskImage.setImageId(imageId);
        return diskImage;
    }

    @Override
    protected void executeVmCommand() {
        super.executeVmCommand();
        setVm(null);
        getVm().setVmtGuid(VmTemplateHandler.BlankVmTemplateId);
        getVmStaticDao().update(getVm().getStaticData());
    }

}
