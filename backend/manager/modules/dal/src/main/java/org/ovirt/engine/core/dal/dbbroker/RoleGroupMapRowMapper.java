package org.ovirt.engine.core.dal.dbbroker;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.RoleGroupMap;
import org.ovirt.engine.core.compat.Guid;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

public class RoleGroupMapRowMapper implements ParameterizedRowMapper<RoleGroupMap> {
    public static final RoleGroupMapRowMapper instance = new RoleGroupMapRowMapper();

    @Override
    public RoleGroupMap mapRow(ResultSet rs, int rowNum) throws SQLException {
        RoleGroupMap entity = new RoleGroupMap(ActionGroup.forValue(rs.getInt("action_group_id")),
                Guid.createGuidFromString(rs.getString("role_id")));
        return entity;
    }
}
