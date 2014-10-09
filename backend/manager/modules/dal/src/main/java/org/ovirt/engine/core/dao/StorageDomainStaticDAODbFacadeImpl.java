package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StorageFormatType;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.compat.Guid;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

public class StorageDomainStaticDAODbFacadeImpl extends DefaultGenericDaoDbFacade<StorageDomainStatic, Guid> implements StorageDomainStaticDAO {

    public StorageDomainStaticDAODbFacadeImpl() {
        super("storage_domain_static");
        setProcedureNameForGet("Getstorage_domain_staticByid");
        setProcedureNameForGetAll("GetAllFromstorage_domain_static");
    }

    @Override
    public StorageDomainStatic getByName(String name) {
        return getCallsHandler().executeRead("Getstorage_domain_staticByName",
                StorageDomainStaticRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("name", name));
    }

    @Override
    public List<StorageDomainStatic> getAllOfStorageType(
            StorageType type) {
        return getCallsHandler().executeReadList("Getstorage_domain_staticBystorage_pool_type",
                StorageDomainStaticRowMapper.instance,
                getCustomMapSqlParameterSource()
                        .addValue("storage_pool_type", type));
    }

    @Override
    public List<StorageDomainStatic> getAllForStoragePoolOfStorageType(
            StorageType type, Guid pool) {
        return getCallsHandler().executeReadList("Getstorage_domain_staticBystorage_type_and_storage_pool_id",
                StorageDomainStaticRowMapper.instance,
                getStoragePoolIdParameterSource(pool)
                        .addValue("storage_type", type));
    }

    @Override
    public List<StorageDomainStatic> getAllForStoragePool(Guid id) {
        return getCallsHandler().executeReadList("Getstorage_domain_staticBystorage_pool_id",
                StorageDomainStaticRowMapper.instance,
                getStoragePoolIdParameterSource(id));
    }

    private MapSqlParameterSource getStoragePoolIdParameterSource(Guid id) {
        return getCustomMapSqlParameterSource()
                .addValue("storage_pool_id", id);
    }

    @Override
    public List<Guid> getAllIds(Guid pool, StorageDomainStatus status) {
        MapSqlParameterSource parameterSource = getStoragePoolIdParameterSource(pool)
                .addValue("status", status.getValue());

        ParameterizedRowMapper<Guid> mapper = new ParameterizedRowMapper<Guid>() {
            @Override
            public Guid mapRow(ResultSet rs, int rowNum) throws SQLException {
                return Guid.createGuidFromString(rs.getString("storage_id"));
            }
        };

        return getCallsHandler().executeReadList("GetStorageDomainIdsByStoragePoolIdAndStatus", mapper, parameterSource);
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(Guid id) {
        return getCustomMapSqlParameterSource()
                .addValue("id", id);
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(StorageDomainStatic domain) {
        return getCustomMapSqlParameterSource()
                .addValue("id", domain.getId())
                .addValue("storage", domain.getstorage())
                .addValue("storage_name", domain.getstorage_name())
                .addValue("storage_type", domain.getstorage_type())
                .addValue("storage_domain_type",
                        domain.getstorage_domain_type())
                .addValue("storage_domain_format_type", domain.getStorageFormat())
                .addValue("last_time_used_as_master", domain.getLastTimeUsedAsMaster());
    }

    @Override
    protected ParameterizedRowMapper<StorageDomainStatic> createEntityRowMapper() {
        return StorageDomainStaticRowMapper.instance;
    }

    private static final class StorageDomainStaticRowMapper implements ParameterizedRowMapper<StorageDomainStatic> {
        public static final StorageDomainStaticRowMapper instance = new StorageDomainStaticRowMapper();

        @Override
        public StorageDomainStatic mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            StorageDomainStatic entity = new StorageDomainStatic();
            entity.setId(Guid.createGuidFromString(rs.getString("id")));
            entity.setstorage(rs.getString("storage"));
            entity.setstorage_name(rs.getString("storage_name"));
            entity.setstorage_type(StorageType.forValue(rs
                    .getInt("storage_type")));
            entity.setstorage_domain_type(StorageDomainType.forValue(rs
                    .getInt("storage_domain_type")));
            entity.setStorageFormat(StorageFormatType.forValue(rs
                    .getString("storage_domain_format_type")));
            entity.setLastTimeUsedAsMaster(rs.getLong("last_time_used_as_master"));
            return entity;
        }
    }

}
