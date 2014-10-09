package org.ovirt.engine.core.bll;

import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.common.action.ImagesContainterParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.VmTemplateParametersBase;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.ImageStorageDomainMapId;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

/**
 * This command responsible to removing all Image Templates, of a VmTemplate
 * on all domains specified in the parameters
 */

@InternalCommandAttribute
public class RemoveAllVmTemplateImageTemplatesCommand<T extends VmTemplateParametersBase> extends VmTemplateCommand<T> {
    public RemoveAllVmTemplateImageTemplatesCommand(T parameters) {
        super(parameters);
        super.setVmTemplateId(parameters.getVmTemplateId());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void executeCommand() {
        List<DiskImage> imageTemplates = ImagesHandler.filterImageDisks(DbFacade.getInstance().getDiskDao().getAllForVm(
                getVmTemplateId()), false, false);
        boolean noImagesRemovedYet = true;
        for (DiskImage template : imageTemplates) {
            // get disk
            // remove this disk in all domain that were sent
            for (Guid domain : (Collection<Guid>)CollectionUtils.intersection(getParameters().getStorageDomainsList(), template.getstorage_ids())) {
                ImagesContainterParametersBase tempVar = new ImagesContainterParametersBase(template.getImageId(),
                        getVmTemplateId());
                tempVar.setStorageDomainId(domain);
                tempVar.setStoragePoolId(template.getstorage_pool_id().getValue());
                tempVar.setImageGroupID(template.getId());
                tempVar.setEntityId(getParameters().getEntityId());
                tempVar.setWipeAfterDelete(template.isWipeAfterDelete());
                tempVar.setTransactionScopeOption(TransactionScopeOption.RequiresNew);
                tempVar.setParentCommand(getActionType());
                tempVar.setParentParameters(getParameters());
                VdcReturnValueBase vdcReturnValue = Backend.getInstance().runInternalAction(
                                VdcActionType.RemoveTemplateSnapshot,
                                tempVar,
                                ExecutionHandler.createDefaultContexForTasks(getExecutionContext()));

                if (vdcReturnValue.getSucceeded()) {
                    getReturnValue().getInternalTaskIdList().addAll(vdcReturnValue.getInternalTaskIdList());
                } else {
                    if (noImagesRemovedYet) {
                        setSucceeded(false);
                        getReturnValue().setFault(vdcReturnValue.getFault());
                        return;
                    }

                    log.errorFormat("Can't remove image id: {0} for template id: {1} from domain id: {2} due to: {3}.",
                            template.getImageId(), getVmTemplateId(), domain,
                            vdcReturnValue.getFault().getMessage());
                }

                DbFacade.getInstance().getImageStorageDomainMapDao().remove(
                        new ImageStorageDomainMapId(template.getImageId(), domain));
                noImagesRemovedYet = false;
            }

            // remove images from db only if removing template completely
            if (getParameters().isRemoveTemplateFromDb()) {
                DiskImage diskImage = DbFacade.getInstance().getDiskImageDao().get(template.getImageId());
                if (diskImage != null) {
                    DbFacade.getInstance().getBaseDiskDao().remove(template.getImageId());
                    DbFacade.getInstance()
                            .getVmDeviceDao()
                            .remove(new VmDeviceId(diskImage.getImageId(), getVmTemplateId()));
                    DbFacade.getInstance().getImageStorageDomainMapDao().remove(diskImage.getImageId());
                    DbFacade.getInstance().getImageDao().remove(template.getImageId());
                }
            }
        }
        setSucceeded(true);
    }
}
