package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.ovirt.engine.core.common.businessentities.DiskLunMap;
import org.ovirt.engine.core.common.businessentities.DiskLunMapId;
import org.ovirt.engine.core.compat.Guid;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

public class DiskLunMapDaoDbFacadeImpl extends DefaultGenericDaoDbFacade<DiskLunMap, DiskLunMapId>
        implements DiskLunMapDao {

    public DiskLunMapDaoDbFacadeImpl() {
        super("DiskLunMap");
    }

    @Override
    public void update(DiskLunMap entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(DiskLunMapId id) {
        return getCustomMapSqlParameterSource().addValue("disk_id", id.getDiskId()).addValue("lun_id", id.getLunId());
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(DiskLunMap entity) {
        return createIdParameterMapper(entity.getId());
    }

    @Override
    protected ParameterizedRowMapper<DiskLunMap> createEntityRowMapper() {
        return new ParameterizedRowMapper<DiskLunMap>() {

            @Override
            public DiskLunMap mapRow(ResultSet rs, int rowNum) throws SQLException {
                DiskLunMap diskLunMap = new DiskLunMap();

                diskLunMap.setDiskId(Guid.createGuidFromString(rs.getString("disk_id")));
                diskLunMap.setLunId(rs.getString("lun_id"));

                return diskLunMap;
            }
        };
    }

    @Override
    public DiskLunMap getDiskIdByLunId(String lunId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("lun_id", lunId);

        return getCallsHandler().executeRead("GetDiskLunMapByLunId", createEntityRowMapper(), parameterSource);
    }
}
