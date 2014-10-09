package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.businessentities.BootSequence;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.MigrationSupport;
import org.ovirt.engine.core.common.businessentities.OriginType;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.SessionState;
import org.ovirt.engine.core.common.businessentities.UsbPolicy;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VdsSelectionAlgorithm;
import org.ovirt.engine.core.common.businessentities.VmExitStatus;
import org.ovirt.engine.core.common.businessentities.VmOsType;
import org.ovirt.engine.core.common.businessentities.VmPauseStatus;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.NGuid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.dbbroker.DbFacadeUtils;
import org.ovirt.engine.core.utils.MultiValueMapUtils;
import org.ovirt.engine.core.utils.vmproperties.VmPropertiesUtils;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * <code>VmDAODbFacadeImpl</code> provides a concrete implementation of {@link VmDAO}. The functionality is code
 * refactored out of {@link Dorg.ovirt.engine.core.dal.dbbroker.DbFacad}.
 */
public class VmDAODbFacadeImpl extends BaseDAODbFacade implements VmDAO {

    @Override
    public VM get(Guid id) {
        return get(id, null, false);
    }

    @Override
    public VM get(Guid id, Guid userID, boolean isFiltered) {
        return getCallsHandler().executeRead("GetVmByVmGuid", VMRowMapper.instance, getCustomMapSqlParameterSource()
                .addValue("vm_guid", id).addValue("user_id", userID).addValue("is_filtered", isFiltered));
    }

    @Override
    public VM getForHibernationImage(Guid id) {
        return getCallsHandler().executeRead("GetVmByHibernationImageId",
                VMRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("image_id", id));
    }

    @Override
    public Map<Boolean, List<VM>> getForDisk(Guid id) {
        Map<Boolean, List<VM>> result = new HashMap<Boolean, List<VM>>();
        List<VMWithPlugInfo> vms = getVmsWithPlugInfo(id);
        for (VMWithPlugInfo vm : vms) {
            MultiValueMapUtils.addToMap(vm.isPlugged(), vm.getVM(), result);
        }
        return result;
    }

    @Override
    public List<VM> getVmsListForDisk(Guid id) {
        List<VM> result = new ArrayList<VM>();
        List<VMWithPlugInfo> vms = getVmsWithPlugInfo(id);
        for (VMWithPlugInfo vm : vms) {
            result.add(vm.getVM());
        }
        return result;
    }

    private List<VMWithPlugInfo> getVmsWithPlugInfo(Guid id) {
        return getCallsHandler().executeReadList
                ("GetVmsByDiskId",
                        VMWithPlugInfoRowMapper.instance,
                        getCustomMapSqlParameterSource().addValue("disk_guid", id));
    }

    @Override
    public List<VM> getAllForUser(Guid id) {
        return getCallsHandler().executeReadList("GetVmsByUserId",
                VMRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("user_id", id));
    }

    @Override
    public List<VM> getAllForUserWithGroupsAndUserRoles(Guid id) {
        return getCallsHandler().executeReadList("GetVmsByUserIdWithGroupsAndUserRoles", VMRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("user_id", id));
    }

    @Override
    public List<VM> getAllForAdGroupByName(String name) {
        return getCallsHandler().executeReadList("GetVmsByAdGroupNames",
                VMRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("ad_group_names", name));
    }

    @Override
    public List<VM> getAllWithTemplate(Guid id) {
        return getCallsHandler().executeReadList("GetVmsByVmtGuid",
                VMRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("vmt_guid", id));
    }

    @Override
    public List<VM> getAllRunningForVds(Guid id) {
        return getCallsHandler().executeReadList("GetVmsRunningOnVds",
                VMRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("vds_id", id));
    }

    @Override
    public List<VM> getAllForDedicatedPowerClientByVds(Guid id) {
        return getCallsHandler().executeReadList("GetVmsDedicatedToPowerClientByVdsId",
                VMRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("dedicated_vm_for_vds", id));
    }

    @Override
    public Map<Guid, VM> getAllRunningByVds(Guid id) {
        HashMap<Guid, VM> map = new HashMap<Guid, VM>();

        for (VM vm : getAllRunningForVds(id)) {
            map.put(vm.getId(), vm);
        }

        return map;
    }

    @Override
    public List<VM> getAllUsingQuery(String query) {
        return new SimpleJdbcTemplate(jdbcTemplate).query(query, VMRowMapper.instance);
    }

    @Override
    public List<VM> getAllForStorageDomain(Guid id) {
        return getCallsHandler().executeReadList("GetVmsByStorageDomainId",
                VMRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("storage_domain_id", id));
    }

    @Override
    public List<VM> getAllVmsRelatedToQuotaId(Guid quotaId) {
        return getCallsHandler().executeReadList("getAllVmsRelatedToQuotaId",
                VMRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("quota_id", quotaId));
    }

    @Override
    public List<VM> getVmsByIds(List<Guid> vmsIds) {
        return getCallsHandler().executeReadList("GetVmsByIds",
                VMRowMapper.instance,
                getCustomMapSqlParameterSource().addValue("vms_ids", StringUtils.join(vmsIds, ',')));
    }

    @Override
    public List<VM> getAllActiveForStorageDomain(Guid id) {
        return getCallsHandler().executeReadList("GetActiveVmsByStorageDomainId",
                VMRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("storage_domain_id", id));
    }

    @Override
    public List<VM> getAll() {
        return getAll(null, false);
    }

    @Override
    public List<VM> getAll(Guid userID, boolean isFiltered) {
        return getCallsHandler().executeReadList("GetAllFromVms",
                VMRowMapper.instance,
                getCustomMapSqlParameterSource().addValue("user_id", userID).addValue("is_filtered", isFiltered));
    }

    @Override
    public void save(VM vm) {
        getCallsHandler().executeModification("InsertVm", getCustomMapSqlParameterSource()
                .addValue("description", vm.getDescription())
                .addValue("mem_size_mb", vm.getMemSizeMb())
                .addValue("os", vm.getOs())
                .addValue("vds_group_id", vm.getVdsGroupId())
                .addValue("vm_guid", vm.getId())
                .addValue("vm_name", vm.getVmName())
                .addValue("vmt_guid", vm.getVmtGuid())
                .addValue("num_of_monitors", vm.getNumOfMonitors())
                .addValue("allow_console_reconnect", vm.getAllowConsoleReconnect())
                .addValue("is_initialized", vm.isInitialized())
                .addValue("is_auto_suspend", vm.isAutoSuspend())
                .addValue("num_of_sockets", vm.getNumOfSockets())
                .addValue("cpu_per_socket", vm.getCpuPerSocket())
                .addValue("usb_policy", vm.getUsbPolicy())
                .addValue("time_zone", vm.getTimeZone())
                .addValue("auto_startup", vm.isAutoStartup())
                .addValue("is_stateless", vm.isStateless())
                .addValue("is_smartcard_enabled", vm.isSmartcardEnabled())
                .addValue("is_delete_protected", vm.isDeleteProtected())
                .addValue("dedicated_vm_for_vds", vm.getDedicatedVmForVds())
                .addValue("fail_back", vm.isFailBack())
                .addValue("vm_type", vm.getVmType())
                .addValue("nice_level", vm.getNiceLevel())
                .addValue("default_boot_sequence",
                        vm.getDefaultBootSequence())
                .addValue("default_display_type", vm.getDefaultDisplayType())
                .addValue("priority", vm.getPriority())
                .addValue("iso_path", vm.getIsoPath())
                .addValue("origin", vm.getOrigin())
                .addValue("initrd_url", vm.getInitrdUrl())
                .addValue("kernel_url", vm.getKernelUrl())
                .addValue("kernel_params", vm.getKernelParams())
                .addValue("migration_support",
                        vm.getMigrationSupport().getValue())
                .addValue("predefined_properties", vm.getPredefinedProperties())
                .addValue("userdefined_properties",
                        vm.getUserDefinedProperties())
                .addValue("min_allocated_mem", vm.getMinAllocatedMem())
                .addValue("cpu_pinning", vm.getCpuPinning())
                .addValue("host_cpu_flags", vm.isUseHostCpuFlags())
                .addValue("guest_agent_nics_hash", vm.getGuestAgentNicsHash()));
    }

    @Override
    public void remove(Guid id) {
        getCallsHandler().executeModification("DeleteVm", getCustomMapSqlParameterSource()
                .addValue("vm_guid", id));
    }

    @Override
    public List<VM> getAllForNetwork(Guid id) {
        return getCallsHandler().executeReadList("GetVmsByNetworkId",
                VMRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("network_id", id));
    }

    static final class VMRowMapper implements ParameterizedRowMapper<VM> {
        public static final VMRowMapper instance = new VMRowMapper();

        @Override
        public VM mapRow(ResultSet rs, int rowNum) throws SQLException {

            VM entity = new VM();
            entity.setId(Guid.createGuidFromString(rs.getString("vm_guid")));
            entity.setVmName(rs.getString("vm_name"));
            entity.setQuotaId(Guid.createGuidFromString(rs.getString("quota_id")));
            entity.setQuotaName(rs.getString("quota_name"));
            entity.setQuotaEnforcementType(QuotaEnforcementTypeEnum.forValue(rs.getInt("quota_enforcement_type")));
            entity.setVmMemSizeMb(rs.getInt("vm_mem_size_mb"));
            entity.setVmtGuid(Guid.createGuidFromString(rs.getString("vmt_guid")));
            entity.setVmOs(VmOsType.forValue(rs.getInt("vm_os")));
            entity.setVmDescription(rs.getString("vm_description"));
            entity.setVdsGroupId(Guid.createGuidFromString(rs.getString("vds_group_id")));
            entity.setVmDomain(rs.getString("vm_domain"));
            entity.setVmCreationDate(DbFacadeUtils.fromDate(rs.getTimestamp("vm_creation_date")));
            entity.setVdsGroupName(rs.getString("vds_group_name"));
            entity.setVdsGroupDescription(rs.getString("vds_group_description"));
            entity.setVmtName(rs.getString("vmt_name"));
            entity.setVmtMemSizeMb(rs.getInt("vmt_mem_size_mb"));
            entity.setVmtOs(VmOsType.forValue(rs.getInt("vmt_os")));
            entity.setVmtCreationDate(DbFacadeUtils.fromDate(rs.getTimestamp("vmt_creation_date")));
            entity.setVmtChildCount(rs.getInt("vmt_child_count"));
            entity.setVmtNumOfCpus(rs.getInt("vmt_num_of_cpus"));
            entity.setVmtNumOfSockets(rs.getInt("vmt_num_of_sockets"));
            entity.setVmtCpuPerSocket(rs.getInt("vmt_cpu_per_socket"));
            entity.setVmtDescription(rs.getString("vmt_description"));
            entity.setStatus(VMStatus.forValue(rs.getInt("status")));
            entity.setVmIp(rs.getString("vm_ip"));
            entity.setVmHost(rs.getString("vm_host"));
            entity.setVmPid((Integer) rs.getObject("vm_pid"));
            entity.setDbGeneration(rs.getLong("db_generation"));
            entity.setLastStartTime(DbFacadeUtils.fromDate(rs.getTimestamp("last_start_time")));
            entity.setGuestCurrentUserName(rs.getString("guest_cur_user_name"));
            entity.setConsoleCurrentUserName(rs.getString("console_cur_user_name"));
            entity.setGuestLastLoginTime(DbFacadeUtils.fromDate(rs.getTimestamp("guest_last_login_time")));
            entity.setGuestLastLogoutTime(DbFacadeUtils.fromDate(rs.getTimestamp("guest_last_logout_time")));
            entity.setConsoleUserId(NGuid.createGuidFromString(rs.getString("console_user_id")));
            entity.setGuestOs(rs.getString("guest_os"));
            entity.setCpuUser(rs.getDouble("cpu_user"));
            entity.setCpuSys(rs.getDouble("cpu_sys"));
            entity.setElapsedTime(rs.getDouble("elapsed_time"));
            entity.setUsageNetworkPercent((Integer) rs.getObject("usage_network_percent"));
            entity.setUsageMemPercent((Integer) rs.getObject("usage_mem_percent"));
            entity.setUsageCpuPercent((Integer) rs.getObject("usage_cpu_percent"));
            entity.setRunOnVds(NGuid.createGuidFromString(rs.getString("run_on_vds")));
            entity.setMigratingToVds(NGuid.createGuidFromString(rs.getString("migrating_to_vds")));
            entity.setAppList(rs.getString("app_list"));
            entity.setDisplay((Integer) rs.getObject("display"));
            entity.setVmPoolName(rs.getString("vm_pool_name"));
            entity.setVmPoolId(NGuid.createGuidFromString(rs.getString("vm_pool_id")));
            entity.setNumOfMonitors(rs.getInt("num_of_monitors"));
            entity.setAllowConsoleReconnect(rs.getBoolean("allow_console_reconnect"));
            entity.setInitialized(rs.getBoolean("is_initialized"));
            entity.setAutoSuspend(rs.getBoolean("is_auto_suspend"));
            entity.setNumOfSockets(rs.getInt("num_of_sockets"));
            entity.setCpuPerSocket(rs.getInt("cpu_per_socket"));
            entity.setUsbPolicy(UsbPolicy.forValue(rs.getInt("usb_policy")));
            entity.setAcpiEnable((Boolean) rs.getObject("acpi_enable"));
            entity.setSession(SessionState.forValue(rs.getInt("session")));
            entity.setDisplayIp(rs.getString("display_ip"));
            entity.setDisplayType(DisplayType.forValue(rs.getInt("display_type")));
            entity.setKvmEnable((Boolean) rs.getObject("kvm_enable"));
            entity.setBootSequence(BootSequence.forValue(rs.getInt("boot_sequence")));
            entity.setRunOnVdsName(rs.getString("run_on_vds_name"));
            entity.setTimeZone(rs.getString("time_zone"));
            entity.setDisplaySecurePort((Integer) rs.getObject("display_secure_port"));
            entity.setUtcDiff((Integer) rs.getObject("utc_diff"));
            entity.setAutoStartup(rs.getBoolean("auto_startup"));
            entity.setStateless(rs.getBoolean("is_stateless"));
            entity.setSmartcardEnabled(rs.getBoolean("is_smartcard_enabled"));
            entity.setDeleteProtected(rs.getBoolean("is_delete_protected"));
            entity.setDedicatedVmForVds(NGuid.createGuidFromString(rs.getString("dedicated_vm_for_vds")));
            entity.setFailBack(rs.getBoolean("fail_back"));
            entity.setLastVdsRunOn(NGuid.createGuidFromString(rs.getString("last_vds_run_on")));
            entity.setClientIp(rs.getString("client_ip"));
            entity.setGuestRequestedMemory((Integer) rs.getObject("guest_requested_memory"));
            entity.setVdsGroupCpuName(rs.getString("vds_group_cpu_name"));
            entity.setVmType(VmType.forValue(rs.getInt("vm_type")));
            entity.setStoragePoolId(Guid.createGuidFromString(rs.getString("storage_pool_id")));
            entity.setStoragePoolName(rs.getString("storage_pool_name"));
            entity.setSelectionAlgorithm(VdsSelectionAlgorithm.forValue(rs.getInt("selection_algorithm")));
            entity.setTransparentHugePages(rs.getBoolean("transparent_hugepages"));
            entity.setNiceLevel(rs.getInt("nice_level"));
            entity.setHibernationVolHandle(rs.getString("hibernation_vol_handle"));
            entity.setDefaultBootSequence(BootSequence.forValue(rs.getInt("default_boot_sequence")));
            entity.setDefaultDisplayType(DisplayType.forValue(rs.getInt("default_display_type")));
            entity.setPriority(rs.getInt("priority"));
            entity.setIsoPath(rs.getString("iso_path"));
            entity.setOrigin(OriginType.forValue(rs.getInt("origin")));
            entity.setInitrdUrl(rs.getString("initrd_url"));
            entity.setKernelUrl(rs.getString("kernel_url"));
            entity.setKernelParams(rs.getString("kernel_params"));
            entity.setVdsGroupCompatibilityVersion(new Version(rs.getString("vds_group_compatibility_version")));
            entity.setExitMessage(rs.getString("exit_message"));
            entity.setExitStatus(VmExitStatus.forValue(rs.getInt("exit_status")));
            entity.setVmPauseStatus(VmPauseStatus.forValue(rs.getInt("pause_status")));
            entity.setMigrationSupport(MigrationSupport.forValue(rs.getInt("migration_support")));
            String predefinedProperties = rs.getString("predefined_properties");
            String userDefinedProperties = rs.getString("userdefined_properties");
            entity.setPredefinedProperties(predefinedProperties);
            entity.setUserDefinedProperties(userDefinedProperties);
            entity.setCustomProperties(VmPropertiesUtils.getInstance().customProperties(predefinedProperties,
                    userDefinedProperties));
            entity.setMinAllocatedMem(rs.getInt("min_allocated_mem"));
            entity.setHash(rs.getString("hash"));
            entity.setCpuPinning(rs.getString("cpu_pinning"));
            entity.setUseHostCpuFlags(rs.getBoolean("host_cpu_flags"));
            entity.setGuestAgentNicsHash(rs.getInt("guest_agent_nics_hash"));
            return entity;
        }
    }

    private static class VMWithPlugInfo {

        public VM getVM() {
            return vm;
        }

        public void setVM(VM vm) {
            this.vm = vm;
        }

        public boolean isPlugged() {
            return isPlugged;
        }

        public void setPlugged(boolean isPlugged) {
            this.isPlugged = isPlugged;
        }

        private VM vm;
        private boolean isPlugged;
    }

    private static final class VMWithPlugInfoRowMapper implements ParameterizedRowMapper<VMWithPlugInfo> {
        public static final VMWithPlugInfoRowMapper instance = new VMWithPlugInfoRowMapper();

        @Override
        public VMWithPlugInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            @SuppressWarnings("synthetic-access")
            VMWithPlugInfo entity = new VMWithPlugInfo();

            entity.setPlugged(rs.getBoolean("is_plugged"));
            entity.setVM(VMRowMapper.instance.mapRow(rs, rowNum));
            return entity;
        }
    }
}
