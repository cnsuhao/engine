package org.ovirt.engine.ui.uicommonweb.models.templates;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.queries.GetAllFromExportDomainQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.ListWithDetailsModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ExportVmModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public abstract class VmBaseListModel<T> extends ListWithDetailsModel {

    protected void Export(String title)
    {
        T selectedEntity = (T) getSelectedItem();
        if (selectedEntity == null)
        {
            return;
        }

        if (getWindow() != null)
        {
            return;
        }

        ExportVmModel model = new ExportVmModel();
        setWindow(model);
        model.StartProgress(null);
        model.setTitle(title);
        model.setHashName("export_virtual_machine"); //$NON-NLS-1$
        setupExportModel(model);

        AsyncDataProvider.GetStorageDomainList(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void OnSuccess(Object target, Object returnValue) {
                        VmBaseListModel vmListModel = (VmBaseListModel) target;
                        List<storage_domains> storageDomains =
                                (List<storage_domains>) returnValue;

                        List<storage_domains> filteredStorageDomains =
                                new ArrayList<storage_domains>();
                        for (storage_domains a : storageDomains)
                        {
                            if (a.getstorage_domain_type() == StorageDomainType.ImportExport)
                            {
                                filteredStorageDomains.add(a);
                            }
                        }

                        vmListModel.PostExportGetStorageDomainList(filteredStorageDomains);
                    }
                }), extractStoragePoolIdNullSafe(selectedEntity));
    }

    private void PostExportGetStorageDomainList(List<storage_domains> storageDomains)
    {
        ExportVmModel model = (ExportVmModel) getWindow();
        model.getStorage().setItems(storageDomains);
        model.getStorage().setSelectedItem(Linq.FirstOrDefault(storageDomains));

        boolean noActiveStorage = true;
        for (storage_domains a : storageDomains) {
            if (a.getstatus() == StorageDomainStatus.Active) {
                noActiveStorage = false;
                break;
            }
        }

        if (entitiesSelectedOnDifferentDataCenters()) {
            model.getCollapseSnapshots().setIsChangable(false);
            model.getForceOverride().setIsChangable(false);

            model.setMessage(entityResideOnSeveralDCsMakeSureTheExportedVMResideOnSameDcMsg());

            UICommand tempVar = new UICommand("Cancel", this); //$NON-NLS-1$
            tempVar.setTitle(ConstantsManager.getInstance().getConstants().close());
            tempVar.setIsDefault(true);
            tempVar.setIsCancel(true);
            model.getCommands().add(tempVar);
            model.StopProgress();
        }
        else if (storageDomains.isEmpty()) {
            model.getCollapseSnapshots().setIsChangable(false);
            model.getForceOverride().setIsChangable(false);

            model.setMessage(thereIsNoExportDomainBackupEntityAttachExportDomainToVmsDcMsg());

            UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
            tempVar2.setTitle(ConstantsManager.getInstance().getConstants().close());
            tempVar2.setIsDefault(true);
            tempVar2.setIsCancel(true);
            model.getCommands().add(tempVar2);
            model.StopProgress();
        }
        else if (noActiveStorage) {
            model.getCollapseSnapshots().setIsChangable(false);
            model.getForceOverride().setIsChangable(false);

            model.setMessage(ConstantsManager.getInstance()
                    .getConstants()
                    .theRelevantExportDomainIsNotActivePleaseActivateItMsg());

            UICommand tempVar3 = new UICommand("Cancel", this); //$NON-NLS-1$
            tempVar3.setTitle(ConstantsManager.getInstance().getConstants().close());
            tempVar3.setIsDefault(true);
            tempVar3.setIsCancel(true);
            model.getCommands().add(tempVar3);
            model.StopProgress();
        }
        else {
            showWarningOnExistingEntities(model, getEntityExportDomain());

            UICommand tempVar4 = new UICommand("OnExport", this); //$NON-NLS-1$
            tempVar4.setTitle(ConstantsManager.getInstance().getConstants().ok());
            tempVar4.setIsDefault(true);
            model.getCommands().add(tempVar4);
            UICommand tempVar5 = new UICommand("Cancel", this); //$NON-NLS-1$
            tempVar5.setTitle(ConstantsManager.getInstance().getConstants().cancel());
            tempVar5.setIsCancel(true);
            model.getCommands().add(tempVar5);
        }
    }

    protected void showWarningOnExistingEntities(ExportVmModel model, final VdcQueryType getVmOrTemplateQuery) {
        Guid storageDomainId = ((storage_domains) model.getStorage().getSelectedItem()).getId();
        AsyncDataProvider.GetDataCentersByStorageDomain(new AsyncQuery(new Object[] { this, model },
                new INewAsyncCallback() {
                    @Override
                    public void OnSuccess(Object target, Object returnValue) {
                        Object[] array = (Object[]) target;
                        VmBaseListModel vmListModel = (VmBaseListModel) array[0];
                        ExportVmModel exportVmModel = (ExportVmModel) array[1];
                        List<storage_pool> storagePools = (List<storage_pool>) returnValue;
                        vmListModel.PostShowWarningOnExistingVms(exportVmModel, storagePools, getVmOrTemplateQuery);
                    }
                }), storageDomainId);
    }

    private void PostShowWarningOnExistingVms(final ExportVmModel exportModel,
            List<storage_pool> storagePools,
            VdcQueryType getVmOrTemplateQuery) {
        storage_pool storagePool = storagePools.size() > 0 ? storagePools.get(0) : null;

        if (storagePool != null) {
            AsyncQuery _asyncQuery = new AsyncQuery();
            _asyncQuery.setModel(this);
            _asyncQuery.asyncCallback = new INewAsyncCallback() {
                @Override
                public void OnSuccess(Object model, Object result) {
                    VmBaseListModel listModel = (VmBaseListModel) model;
                    ExportVmModel windowModel = (ExportVmModel) listModel.getWindow();
                    List<T> foundVms = new ArrayList<T>();

                    if (result != null) {
                        VdcQueryReturnValue returnValue = (VdcQueryReturnValue) result;
                        Iterable<T> iterableReturnValue = asIterableReturnValue(returnValue.getReturnValue());

                        for (Object rawSelectedItem : listModel.getSelectedItems()) {
                            T selectedItem = (T) rawSelectedItem;
                            for (T returnValueItem : iterableReturnValue) {
                                if (entititesEqualsNullSafe(returnValueItem, selectedItem)) {
                                    foundVms.add(selectedItem);
                                    break;
                                }
                            }
                        }
                    }

                    if (foundVms.size() != 0) {
                        windowModel.setMessage(composeEntityOnStorage(composeExistingVmsWarningMessage(foundVms)));
                    }

                    exportModel.StopProgress();
                }
            };

            Guid storageDomainId = ((storage_domains) exportModel.getStorage().getSelectedItem()).getId();
            GetAllFromExportDomainQueryParameters tempVar =
                    new GetAllFromExportDomainQueryParameters(storagePool.getId(), storageDomainId);
            Frontend.RunQuery(getVmOrTemplateQuery, tempVar, _asyncQuery);
        } else {
            exportModel.StopProgress();
        }
    }

    private String composeExistingVmsWarningMessage(List<T> existingVms) {
        String res = ""; //$NON-NLS-1$
        for (T t : existingVms) {
            String name = extractNameFromEntity(t);
            res += "\u2022  " + name + " "; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return res;
    }

    protected void setupExportModel(ExportVmModel model) {
        // no-op by default. Override if needed.
    }

    protected abstract String composeEntityOnStorage(String entities);

    protected abstract Iterable<T> asIterableReturnValue(Object returnValue);

    protected abstract boolean entititesEqualsNullSafe(T e1, T e2);

    protected abstract String extractNameFromEntity(T entity);

    protected abstract Guid extractStoragePoolIdNullSafe(T entity);

    protected abstract boolean entitiesSelectedOnDifferentDataCenters();

    protected abstract String entityResideOnSeveralDCsMakeSureTheExportedVMResideOnSameDcMsg();

    protected abstract String thereIsNoExportDomainBackupEntityAttachExportDomainToVmsDcMsg();

    protected abstract VdcQueryType getEntityExportDomain();

}
