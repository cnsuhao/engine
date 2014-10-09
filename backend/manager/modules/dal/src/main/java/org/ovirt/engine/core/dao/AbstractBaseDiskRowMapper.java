package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.businessentities.BaseDisk;
import org.ovirt.engine.core.common.businessentities.DiskInterface;
import org.ovirt.engine.core.common.businessentities.PropagateErrors;
import org.ovirt.engine.core.compat.Guid;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

public abstract class AbstractBaseDiskRowMapper<T extends BaseDisk> implements ParameterizedRowMapper<T> {

    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        T disk = createDiskEntity();

        disk.setId(Guid.createGuidFromString(rs.getString("disk_id")));
        disk.setDiskAlias(rs.getString("disk_alias"));
        disk.setDiskDescription(rs.getString("disk_description"));
        String diskInterface = rs.getString("disk_interface");
        if (!StringUtils.isEmpty(diskInterface)) {
            disk.setDiskInterface(DiskInterface.valueOf(diskInterface));
        }

        disk.setWipeAfterDelete(rs.getBoolean("wipe_after_delete"));
        String propagateErrors = rs.getString("propagate_errors");
        if (!StringUtils.isEmpty(propagateErrors)) {
            disk.setPropagateErrors(PropagateErrors.valueOf(propagateErrors));
        }

        disk.setShareable(rs.getBoolean("shareable"));
        disk.setBoot(rs.getBoolean("boot"));
        return disk;
    }

    /**
     * @return The disk entity that is being initialized.
     */
    protected abstract T createDiskEntity();
}
