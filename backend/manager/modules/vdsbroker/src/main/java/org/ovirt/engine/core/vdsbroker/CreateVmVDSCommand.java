package org.ovirt.engine.core.vdsbroker;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.core.common.businessentities.Snapshot.SnapshotStatus;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.vdscommands.CreateVmVDSCommandParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;
import org.ovirt.engine.core.vdsbroker.vdsbroker.CreateVDSCommand;
import org.ovirt.engine.core.vdsbroker.vdsbroker.CreateVmFromSysPrepVDSCommand;
import org.ovirt.engine.core.vdsbroker.vdsbroker.CreateVmFromSysPrepVDSCommandParameters;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VDSGenericException;

public class CreateVmVDSCommand<P extends CreateVmVDSCommandParameters> extends VdsIdVDSCommandBase<P> {
    public CreateVmVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void ExecuteVdsIdCommand() {

        CreateVDSCommand<?> command = null;
        try {
            if (_vdsManager != null) {
                final VM vm = getParameters().getVm();
                if (CanExecute()) {
                    boolean canExecute = ResourceManager.getInstance().AddAsyncRunningVm(
                                vm.getId());
                    if (canExecute) {
                        if (isSysprepUsed(vm)) {
                            // use answer file to run after sysprep.
                            CreateVmFromSysPrepVDSCommandParameters createVmFromSysPrepParam =
                                    new CreateVmFromSysPrepVDSCommandParameters(
                                            getVdsId(),
                                            vm,
                                            vm.getVmName(),
                                            vm.getVmDomain());
                            createVmFromSysPrepParam.setSysPrepParams(getParameters().getSysPrepParams());
                            command =
                                    new CreateVmFromSysPrepVDSCommand<CreateVmFromSysPrepVDSCommandParameters>(createVmFromSysPrepParam);
                            command.Execute();
                            if (command.getVDSReturnValue().getSucceeded()) {
                                vm.setInitialized(true);
                                saveSetInitializedToDb(vm.getId());
                            } else {
                                HandleCommandResult(command);
                            }
                        } else {
                            // normal run.
                            command = new CreateVDSCommand<CreateVmVDSCommandParameters>(getParameters());
                            command.Execute();
                            HandleCommandResult(command);
                            vm.setInitialized(true);
                            saveSetInitializedToDb(vm.getId());
                        }

                        if (command.getVDSReturnValue().getSucceeded()) {
                            TransactionSupport.executeInScope(TransactionScopeOption.Required,
                                    new TransactionMethod<Object>() {
                                        @Override
                                        public Object runInTransaction() {
                                            HandleVdsInformation();
                                            vm.setRunOnVds(getVdsId());
                                            DbFacade.getInstance().getVmDynamicDao().update(vm.getDynamicData());
                                            return null;
                                        }
                                    });
                        } else {
                            ResourceManager.getInstance().RemoveAsyncRunningVm(getParameters().getVmId());
                        }
                    }
                }
                getVDSReturnValue().setReturnValue(vm.getStatus());
            } else {
                getVDSReturnValue().setSucceeded(false);
            }
        } catch (java.lang.Exception e) {
            log.error("Error in excuting CreateVmVDSCommand", e);
            if (command == null || !command.getVDSReturnValue().getSucceeded()) {
                ResourceManager.getInstance().RemoveAsyncRunningVm(getParameters().getVmId());
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * @param vm
     * @return
     */
    private boolean isSysprepUsed(final VM vm) {
        return vm.useSysPrep() && vm.getVmOs().isWindows()
                && StringUtils.isEmpty(vm.getFloppyPath());
    }

    private void HandleVdsInformation() {
        getVds().setmem_commited(getVds().getmem_commited() + getParameters().getVm().getVmMemSizeMb());
        getVds().setmem_commited(getVds().getmem_commited() + getVds().getguest_overhead());
        getVds().setvm_count(getVds().getvm_count() + 1);
        getVds().setvms_cores_count(getVds().getvms_cores_count() + getParameters().getVm().getNumOfCpus());
        getVds().setpending_vcpus_count(
                getVds().getpending_vcpus_count() + getParameters().getVm().getNumOfCpus());
        getVds().setpending_vmem_size(
                getVds().getpending_vmem_size() + getParameters().getVm().getMinAllocatedMem());
        log.infoFormat("IncreasePendingVms::CreateVmIncreasing vds {0} pending vcpu count, now {1}. Vm: {2}", getVds()
                .getvds_name(), getVds().getpending_vcpus_count(), getParameters().getVm().getVmName());
        _vdsManager.UpdateDynamicData(getVds().getDynamicData());
    }

    private boolean CanExecute() {

        Guid guid = getParameters().getVm().getId();
        String vmName = getParameters().getVm().getVmName();
        VmDynamic vmDynamicFromDb = DbFacade.getInstance().getVmDynamicDao().get(guid);
        if (ResourceManager.getInstance().IsVmDuringInitiating(getParameters().getVm().getId())) {
            log.infoFormat("Vm Running failed - vm {0}:{1} already running", guid, vmName);
            getVDSReturnValue().setReturnValue(vmDynamicFromDb.getstatus());
            return false;
        } else {
            VMStatus vmStatus = vmDynamicFromDb.getstatus();

            if (vmStatus == VMStatus.ImageLocked) {
                log.infoFormat("Vm Running failed - vm {0}:{1} - cannot run vm when image is locked", guid, vmName);
                return false;
            }
            if (vmDynamicFromDb.getstatus() != VMStatus.Down && vmDynamicFromDb.getstatus() != VMStatus.Suspended) {
                log.infoFormat("Vm Running failed - vm {0}:{1} already running, status {2}", guid, vmName, vmStatus);
                getVDSReturnValue().setReturnValue(vmDynamicFromDb.getstatus());
                return false;
            }

            List<Snapshot> snapshots = DbFacade.getInstance().getSnapshotDao().getAll(guid);

            if (!snapshots.isEmpty() && SnapshotStatus.LOCKED == snapshots.get(snapshots.size() - 1).getStatus()) {
                log.infoFormat("VM Running failed - VM {0}:{1} - cannot run VM when VM during Snapshot", guid, vmName);
                return false;
            }
        }
        return true;
    }

    private void HandleCommandResult(CreateVDSCommand<?> command) {
        if (!command.getVDSReturnValue().getSucceeded() && command.getVDSReturnValue().getExceptionObject() != null) {
            if (command.getVDSReturnValue().getExceptionObject() instanceof VDSGenericException) {
                log.errorFormat("VDS::create Failed creating vm '{0}' in vds = {1} : {2} error = {3}",
                        getParameters().getVm().getVmName(), getVds().getId(), getVds().getvds_name(),
                        command.getVDSReturnValue().getExceptionString());
                getVDSReturnValue().setReturnValue(VMStatus.Down);
                getVDSReturnValue().setSucceeded(false);
                getVDSReturnValue().setExceptionString(command.getVDSReturnValue().getExceptionString());
                getVDSReturnValue().setVdsError(command.getVDSReturnValue().getVdsError());
            } else {
                throw command.getVDSReturnValue().getExceptionObject();
            }
        }
    }

    private void saveSetInitializedToDb(final Guid vmId) {
        TransactionSupport.executeInScope(TransactionScopeOption.RequiresNew,
                new TransactionMethod<Void>() {
                    @Override
                    public Void runInTransaction() {
                        DbFacade.getInstance().SaveIsInitialized(vmId, true);
                        return null;
                    }
                });
    }

    private static Log log = LogFactory.getLog(CreateVmVDSCommand.class);
}
