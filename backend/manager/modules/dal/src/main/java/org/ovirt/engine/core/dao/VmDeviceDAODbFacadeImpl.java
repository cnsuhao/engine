package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.SerializationFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

public class VmDeviceDAODbFacadeImpl extends
        MassOperationsGenericDaoDbFacade<VmDevice, VmDeviceId> implements VmDeviceDAO {

    private static VmDeviceRowMapper vmDeviceRowMapper = new VmDeviceRowMapper();

    public VmDeviceDAODbFacadeImpl() {
        super("VmDevice");
        setProcedureNameForGet("GetVmDeviceByDeviceId");
        setProcedureNameForGetAll("GetAllFromVmDevice");
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(VmDeviceId id) {
        return getCustomMapSqlParameterSource()
                .addValue("device_id", id.getDeviceId())
                .addValue("vm_id", id.getVmId());
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(VmDevice entity) {

        return createIdParameterMapper(entity.getId())
                .addValue("device", entity.getDevice())
                .addValue("type", entity.getType())
                .addValue("address", entity.getAddress())
                .addValue("boot_order", entity.getBootOrder())
                .addValue("spec_params",
                        entity.getSpecParams() == null ? null : SerializationFactory.getFactory()
                                .createSerializer()
                                .serialize(entity.getSpecParams()))
                .addValue("is_managed", entity.getIsManaged())
                .addValue("is_plugged", entity.getIsPlugged())
                .addValue("is_readonly", entity.getIsReadOnly())
                .addValue("alias", entity.getAlias());
    }

    @Override
    protected ParameterizedRowMapper<VmDevice> createEntityRowMapper() {
        return vmDeviceRowMapper;
    }

    @Override
    public boolean exists(VmDeviceId id) {
        return get(id) != null;
    }

    @Override
    public List<VmDevice> getVmDeviceByVmId(Guid vmId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_id", vmId);

        return getCallsHandler().executeReadList("GetVmDeviceByVmId",
                createEntityRowMapper(), parameterSource);
    }

    @Override
    public List<VmDevice> getVmDeviceByVmIdAndType(Guid vmId, String type) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_id", vmId)
                .addValue("type", type);
        return getCallsHandler().executeReadList("GetVmDeviceByVmIdAndType",
                createEntityRowMapper(), parameterSource);
    }

    @Override
    public List<VmDevice> getVmDeviceByVmIdTypeAndDevice(Guid vmId, String type, String device) {
        return getVmDeviceByVmIdTypeAndDevice(vmId, type, device, null, false);
    }

    @Override
    public List<VmDevice> getVmDeviceByVmIdTypeAndDevice
        (Guid vmId, String type, String device, Guid userID, boolean isFiltered) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_id", vmId)
                .addValue("type", type)
                .addValue("device", device)
                .addValue("user_id", userID)
                .addValue("is_filtered", isFiltered);

        return getCallsHandler().executeReadList("GetVmDeviceByVmIdTypeAndDevice",
                createEntityRowMapper(), parameterSource);
    }

    @Override
    public List<VmDevice> getUnmanagedDevicesByVmId(Guid vmId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_id", vmId);
        return getCallsHandler().executeReadList("GetVmUnmanagedDevicesByVmId",
                createEntityRowMapper(), parameterSource);
    }

    @Override
    public boolean isMemBalloonEnabled(Guid vmId) {

        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_id", vmId);

        Map<String, Object> dbResults =
                new SimpleJdbcCall(jdbcTemplate).withFunctionName("isMemBalloonEnabled").execute(
                        parameterSource);

        String resultKey = dialect.getFunctionReturnKey();
        return dbResults.get(resultKey) != null ? ((Boolean) dbResults.get(resultKey)).booleanValue() : false;

    }

    private static class VmDeviceRowMapper implements ParameterizedRowMapper<VmDevice> {

        @SuppressWarnings("unchecked")
        @Override
        public VmDevice mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            VmDevice vmDevice = new VmDevice();

            vmDevice.setId(new VmDeviceId(Guid.createGuidFromString(rs.getString("device_id")),
                    Guid.createGuidFromString(rs
                            .getString("vm_id"))));
            vmDevice.setDevice(rs.getString("device"));
            vmDevice.setType(rs.getString("type"));
            vmDevice.setAddress(rs.getString("address"));
            vmDevice.setBootOrder(rs.getInt("boot_order"));
            String specParams = rs.getString("spec_params");
            if (StringUtils.isEmpty(specParams)) {
                vmDevice.setSpecParams(new HashMap<String, Object>());
            } else {
                vmDevice.setSpecParams(SerializationFactory.getFactory()
                        .createDeserializer()
                        .deserialize(specParams, HashMap.class));
            }

            vmDevice.setIsManaged(rs.getBoolean("is_managed"));
            vmDevice.setIsPlugged(rs.getBoolean("is_plugged"));
            vmDevice.setIsReadOnly(rs.getBoolean("is_readonly"));
            vmDevice.setAlias(rs.getString("alias"));
            return vmDevice;
        }
    }

    @Override
    public void removeAll(List<VmDeviceId> removedDeviceIds) {
        for (VmDeviceId vmDeviceId : removedDeviceIds) {
            remove(vmDeviceId);
        }
    }

    @Override
    public void saveAll(List<VmDevice> newVmDevices) {
        for (VmDevice vmDevice : newVmDevices) {
            save(vmDevice);
        }
    }

    @Override
    public void clearDeviceAddress(Guid deviceId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("device_id", deviceId);

        getCallsHandler().executeModification("clearVmDeviceAddress", parameterSource);

    }

    @Override
    public void updateRuntimeInfo(VmDevice vmDevice) {
        MapSqlParameterSource paramsForUpdate = createParameterSourceForUpdate(vmDevice)
                .addValue("address", vmDevice.getAddress())
                .addValue("alias",vmDevice.getAlias());

        getCallsHandler().executeModification("UpdateVmDeviceRuntimeInfo", paramsForUpdate);
    }

    private MapSqlParameterSource createParameterSourceForUpdate(VmDevice vmDevice) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("vm_id", vmDevice.getVmId())
                .addValue("device_id",vmDevice.getDeviceId());
        return parameterSource;
    }


    @Override
    public void updateHotPlugDisk(VmDevice vmDevice) {
        MapSqlParameterSource paramsForUpdate = createParameterSourceForUpdate(vmDevice)
            .addValue("boot_order",vmDevice.getBootOrder())
            .addValue("is_plugged",vmDevice.getIsPlugged());
        getCallsHandler().executeModification("UpdateVmDeviceForHotPlugDisk" , paramsForUpdate);

    }

}
