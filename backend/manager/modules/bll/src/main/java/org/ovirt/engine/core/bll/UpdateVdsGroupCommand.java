package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.utils.VersionSupport;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.VdsActionParameters;
import org.ovirt.engine.core.common.action.VdsGroupOperationParameters;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VdsSelectionAlgorithm;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkCluster;
import org.ovirt.engine.core.common.businessentities.network.NetworkStatus;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.SearchParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.validation.group.UpdateEntity;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dao.VdsStaticDAO;
import org.ovirt.engine.core.dao.network.NetworkDao;

@SuppressWarnings("serial")
public class UpdateVdsGroupCommand<T extends VdsGroupOperationParameters> extends
        VdsGroupOperationCommandBase<T> {

    private List<VDS> allForVdsGroup;
    private VDSGroup oldGroup;

    public UpdateVdsGroupCommand(T parameters) {
        super(parameters);
    }

    protected UpdateVdsGroupCommand(Guid commandId) {
        super(commandId);
    }

    @Override
    protected void executeCommand() {
        // TODO: This code should be revisited and proper compensation logic should be introduced here
        CheckMaxMemoryOverCommitValue();
        getVdsGroupDAO().update(getParameters().getVdsGroup());

        if ((oldGroup.getStoragePoolId() != null
                && !oldGroup.getStoragePoolId().equals(getVdsGroup().getStoragePoolId()))
                || (oldGroup.getStoragePoolId() == null
                && getVdsGroup().getStoragePoolId() != null)) {
            VdsActionParameters parameters = new VdsActionParameters();
            for (VDS vds : allForVdsGroup) {
                parameters.setVdsId(vds.getId());
                if (getVdsGroup().getStoragePoolId() != null) {
                    VdcReturnValueBase addVdsSpmIdReturn =
                            getBackend().runInternalAction(VdcActionType.AddVdsSpmId, parameters);
                    if (!addVdsSpmIdReturn.getSucceeded()) {
                        setSucceeded(false);
                        getReturnValue().setFault(addVdsSpmIdReturn.getFault());
                        return;
                    }
                }
            }

            if (oldGroup.getStoragePoolId() != null) {
                for (VDS vds : allForVdsGroup) {
                    getVdsSpmIdMapDAO().removeByVdsAndStoragePool(vds.getId(), oldGroup.getStoragePoolId().getValue());
                }
            }
        }

        // when changing data center we check that default networks exists in
        // cluster
        List<Network> networks = getNetworkDAO()
                .getAllForCluster(getVdsGroup().getId());
        boolean exists = false;
        String managementNetwork = Config.<String> GetValue(ConfigValues.ManagementNetwork);
        for (Network net : networks) {
            if (StringUtils.equals(net.getName(), managementNetwork)) {
                exists = true;
            }
        }
        if (!exists) {
            if (getVdsGroup().getStoragePoolId() != null) {
                List<Network> storagePoolNets =
                        getNetworkDAO()
                                .getAllForDataCenter(
                                        getVdsGroup().getStoragePoolId()
                                                .getValue());
                for (Network net : storagePoolNets) {
                    if (StringUtils.equals(net.getName(), managementNetwork)) {
                        getNetworkClusterDAO().save(new NetworkCluster(getVdsGroup().getId(), net.getId(),
                                NetworkStatus.OPERATIONAL, true, true));
                    }
                }
            }
        }

        setSucceeded(true);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (getParameters().getIsInternalCommand()) {
            return getSucceeded() ? AuditLogType.SYSTEM_UPDATE_VDS_GROUP
                    : AuditLogType.SYSTEM_UPDATE_VDS_GROUP_FAILED;
        }

        return getSucceeded() ? AuditLogType.USER_UPDATE_VDS_GROUP
                : AuditLogType.USER_UPDATE_VDS_GROUP_FAILED;
    }

    @Override
    protected boolean canDoAction() {
        boolean result = true;
        boolean hasVms = false;
        oldGroup = getVdsGroupDAO().get(getVdsGroup().getId());
        if (oldGroup == null) {
            addCanDoActionMessage(VdcBllMessages.VDS_CLUSTER_IS_NOT_VALID);
            result = false;
        }
        // check that if name was changed, it was done to the same cluster
        if (result && !StringUtils.equals(oldGroup.getname(), getVdsGroup().getname())) {
            VDSGroup groupWithName = getVdsGroupDAO().getByName(getVdsGroup().getname());
            if (groupWithName != null && !groupWithName.getId().equals(getVdsGroup().getId())) {
                addCanDoActionMessage(VdcBllMessages.VDS_GROUP_CANNOT_DO_ACTION_NAME_IN_USE);
                result = false;
            }
        }
        if (result && !VersionSupport.checkVersionSupported(getVdsGroup()
                .getcompatibility_version())) {
            addCanDoActionMessage(VersionSupport.getUnsupportedVersionMessage());
            result = false;
        }
        // decreasing of compatibility version is not allowed
        if (result && getVdsGroup().getcompatibility_version().compareTo(oldGroup.getcompatibility_version()) < 0) {
            result = false;
            getReturnValue()
                    .getCanDoActionMessages()
                    .add(VdcBllMessages.ACTION_TYPE_FAILED_CANNOT_DECREASE_COMPATIBILITY_VERSION
                            .toString());
        }
        if (result && oldGroup.getStoragePoolId() != null
                && !oldGroup.getStoragePoolId().equals(getVdsGroup().getStoragePoolId())) {
            addCanDoActionMessage(VdcBllMessages.VDS_GROUP_CANNOT_CHANGE_STORAGE_POOL);
            result = false;
        }
        // If both original Cpu and new Cpu are null, don't check Cpu validity
        if (result) {
            allForVdsGroup = getVdsDAO().getAllForVdsGroup(oldGroup.getId());
        }
        if (result && (oldGroup.getcpu_name() != null || getVdsGroup().getcpu_name() != null)) {
            // Check that cpu exist
            if (!checkIfCpusExist()) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_CPU_NOT_FOUND);
                addCanDoActionMessage(VdcBllMessages.VAR__TYPE__CLUSTER);
                result = false;
            } else {
                // if cpu changed from intel to amd (or backwards) and there are
                // vds in this cluster, cannot update
                if (!StringUtils.isEmpty(oldGroup.getcpu_name())
                        && !checkIfCpusSameManufacture(oldGroup) && !allForVdsGroup.isEmpty()) {
                    addCanDoActionMessage(VdcBllMessages.VDS_GROUP_CANNOT_UPDATE_CPU_ILLEGAL);
                    result = false;
                }
            }
        }
        if (result) {
            List<VDS> vdss = new ArrayList<VDS>();
            boolean isAddedToStoragePool = oldGroup.getStoragePoolId() == null
                    && getVdsGroup().getStoragePoolId() != null;
            for (VDS vds : allForVdsGroup) {
                if (vds.getstatus() == VDSStatus.Up) {
                    if (isAddedToStoragePool) {
                        addCanDoActionMessage(VdcBllMessages.VDS_GROUP_CANNOT_UPDATE_VDS_UP);
                        return false;
                    } else {
                        vdss.add(vds);
                    }
                }
            }
            for (VDS vds : vdss) {
                if (!VersionSupport.checkClusterVersionSupported(
                        getVdsGroup().getcompatibility_version(), vds)) {
                    result = false;
                    addCanDoActionMessage(VdcBllMessages.VDS_GROUP_CANNOT_UPDATE_COMPATIBILITY_VERSION_WITH_LOWER_HOSTS);
                    break;
                } else if (missingServerCpuFlags(vds) != null) {
                    addCanDoActionMessage(VdcBllMessages.VDS_GROUP_CANNOT_UPDATE_CPU_WITH_LOWER_HOSTS);
                    result = false;
                    break;
                }
            }
            if (result) {
                SearchParameters searchParams = new SearchParameters("vms: cluster = "
                        + oldGroup.getname(), SearchType.VM);
                searchParams.setMaxCount(Integer.MAX_VALUE);

                @SuppressWarnings("unchecked")
                List<VM> vmList =
                        (List<VM>) getBackend().runInternalQuery(VdcQueryType.Search, searchParams)
                                .getReturnValue();
                boolean notDownVms = false;
                boolean suspendedVms = false;
                for (VM vm : vmList) {
                    // the search can return vm from cluster with similar name
                    // so it's critical to check that
                    // the vm cluster id is the same as the cluster.id
                    if (!vm.getVdsGroupId().equals(oldGroup.getId())) {
                        continue;
                    } else {
                        hasVms = true;
                    }
                    if (vm.getStatus() == VMStatus.Suspended) {
                        suspendedVms = true;
                    } else if (vm.getStatus() != VMStatus.Down) {
                        notDownVms = true;
                    }
                }
                boolean sameCpuNames = StringUtils.equals(oldGroup.getcpu_name(), getVdsGroup().getcpu_name());
                if (!sameCpuNames) {
                    if (suspendedVms) {
                        addCanDoActionMessage(VdcBllMessages.VDS_GROUP_CANNOT_UPDATE_CPU_WITH_SUSPENDED_VMS);
                        result = false;
                    } else if (notDownVms) {
                        int compareResult = compareCpuLevels(oldGroup);
                        if (compareResult < 0) {
                            addCanDoActionMessage(VdcBllMessages.VDS_GROUP_CANNOT_LOWER_CPU_LEVEL);
                            result = false;
                        } else if (compareResult > 0) {// Upgrade of CPU in same compability level is allowed if
                                                       // there
                            // are running VMs - but we should warn they
                            // cannot not be hibernated
                            AuditLogableBase logable = new AuditLogableBase();
                            logable.AddCustomValue("VdsGroup", getParameters().getVdsGroup().getname());
                            AuditLogDirector.log(logable,
                                    AuditLogType.CANNOT_HIBERNATE_RUNNING_VMS_AFTER_CLUSTER_CPU_UPGRADE);
                        }
                    }
                }
            }
        }
        if (result && getVdsGroup().getStoragePoolId() != null) {
            storage_pool storagePool = getStoragePoolDAO().get(getVdsGroup().getStoragePoolId().getValue());
            if (oldGroup.getStoragePoolId() == null && storagePool.getstorage_pool_type() == StorageType.LOCALFS) {
                // we allow only one cluster in localfs data center
                if (!getVdsGroupDAO().getAllForStoragePool(getVdsGroup().getStoragePoolId().getValue()).isEmpty()) {
                    getReturnValue()
                            .getCanDoActionMessages()
                            .add(VdcBllMessages.VDS_GROUP_CANNOT_ADD_MORE_THEN_ONE_HOST_TO_LOCAL_STORAGE
                                    .toString());
                    result = false;
                }
                // selection algorithm must be set to none in localfs
                else if (getVdsGroup().getselection_algorithm() != VdsSelectionAlgorithm.None) {
                    getReturnValue()
                            .getCanDoActionMessages()
                            .add(VdcBllMessages.VDS_GROUP_SELECTION_ALGORITHM_MUST_BE_SET_TO_NONE_ON_LOCAL_STORAGE
                                    .toString());
                    result = false;
                }
                else if (VDSGroup.DEFAULT_VDS_GROUP_ID.equals(getVdsGroup().getId())) {
                    addCanDoActionMessage(VdcBllMessages.DEFAULT_CLUSTER_CANNOT_BE_ON_LOCALFS);
                    result = false;
                }
            }
        }
        if (result) {
            result = validateMetrics();
        }
        if (result) {
            if (!(getVdsGroup().supportsGlusterService() || getVdsGroup().supportsVirtService())) {
                addCanDoActionMessage(VdcBllMessages.VDS_GROUP_AT_LEAST_ONE_SERVICE_MUST_BE_ENABLED);
                result = false;
            }
            else if (getVdsGroup().supportsGlusterService() && getVdsGroup().supportsVirtService()
                    && !isAllowClusterWithVirtGluster()) {
                addCanDoActionMessage(VdcBllMessages.VDS_GROUP_ENABLING_BOTH_VIRT_AND_GLUSTER_SERVICES_NOT_ALLOWED);
                result = false;
            }
        }
        if (result && hasVms && !getVdsGroup().supportsVirtService()) {
            addCanDoActionMessage(VdcBllMessages.VDS_GROUP_CANNOT_DISABLE_VIRT_WHEN_CLUSTER_CONTAINS_VMS);
            result = false;
        }
        if (result && !getVdsGroup().supportsGlusterService()) {
            List<GlusterVolumeEntity> volumes = getGlusterVolumeDao().getByClusterId(getVdsGroup().getId());
            if (volumes != null && volumes.size() > 0) {
                addCanDoActionMessage(VdcBllMessages.VDS_GROUP_CANNOT_DISABLE_GLUSTER_WHEN_CLUSTER_CONTAINS_VOLUMES);
                result = false;
            }
        }
        return result;
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__CLUSTER);
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__UPDATE);
    }

    protected boolean checkIfCpusSameManufacture(VDSGroup group) {
        return CpuFlagsManagerHandler.CheckIfCpusSameManufacture(group.getcpu_name(),
                getVdsGroup().getcpu_name(),
                getVdsGroup().getcompatibility_version());
    }

    protected boolean checkIfCpusExist() {
        return CpuFlagsManagerHandler.CheckIfCpusExist(getVdsGroup().getcpu_name(),
                getVdsGroup().getcompatibility_version());
    }

    protected List<String> missingServerCpuFlags(VDS vds) {
        return CpuFlagsManagerHandler.missingServerCpuFlags(
                getVdsGroup().getcpu_name(),
                vds.getcpu_flags(),
                getVdsGroup().getcompatibility_version());
    }

    protected int compareCpuLevels(VDSGroup otherGroup) {
        return CpuFlagsManagerHandler.compareCpuLevels(getVdsGroup().getcpu_name(),
                otherGroup.getcpu_name(),
                otherGroup.getcompatibility_version());
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(UpdateEntity.class);
        return super.getValidationGroups();
    }

    protected VdsStaticDAO getVdsStaticDAO() {
        return getDbFacade().getVdsStaticDao();
    }

    @Override
    protected NetworkDao getNetworkDAO() {
        return getDbFacade().getNetworkDao();
    }
}
