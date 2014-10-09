package org.ovirt.engine.core.dao.gluster;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.businessentities.gluster.AccessProtocol;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeOptionEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeType;
import org.ovirt.engine.core.common.businessentities.gluster.TransportType;
import org.ovirt.engine.core.common.utils.EnumUtils;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.MassOperationsGenericDaoDbFacade;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Implementation of the DB Facade for Gluster Volumes.
 */
public class GlusterVolumeDaoDbFacadeImpl extends MassOperationsGenericDaoDbFacade<GlusterVolumeEntity, Guid> implements
        GlusterVolumeDao {

    private static final ParameterizedRowMapper<GlusterVolumeEntity> volumeRowMapper = new GlusterVolumeRowMapper();
    private static final ParameterizedRowMapper<AccessProtocol> accessProtocolRowMapper = new AccessProtocolRowMapper();
    private static final ParameterizedRowMapper<TransportType> transportTypeRowMapper = new TransportTypeRowMapper();

    public GlusterVolumeDaoDbFacadeImpl() {
        super("GlusterVolume");
        setProcedureNameForGet("GetGlusterVolumeById");
    }

    @Override
    public void save(GlusterVolumeEntity volume) {
        insertVolumeEntity(volume);
        insertVolumeBricks(volume);
        insertVolumeOptions(volume);
        insertVolumeAccessProtocols(volume);
        insertVolumeTransportTypes(volume);
    }

    @Override
    public GlusterVolumeEntity getById(Guid id) {
        GlusterVolumeEntity volume = getCallsHandler().executeRead(
                "GetGlusterVolumeById", volumeRowMapper,
                createVolumeIdParams(id));
        fetchRelatedEntities(volume);
        return volume;
    }

    @Override
    public GlusterVolumeEntity getByName(Guid clusterId, String volName) {
        GlusterVolumeEntity volume = getCallsHandler().executeRead(
                "GetGlusterVolumeByName", volumeRowMapper,
                getCustomMapSqlParameterSource()
                        .addValue("cluster_id", clusterId)
                        .addValue("vol_name", volName));

        fetchRelatedEntities(volume);
        return volume;
    }

    @Override
    public List<GlusterVolumeEntity> getByClusterId(Guid clusterId) {
        List<GlusterVolumeEntity> volumes =
                getCallsHandler().executeReadList("GetGlusterVolumesByClusterGuid",
                        volumeRowMapper,
                        getCustomMapSqlParameterSource().addValue("cluster_id", clusterId));
        fetchRelatedEntities(volumes);
        return volumes;
    }

    @Override
    public List<GlusterVolumeEntity> getVolumesByOption(Guid clusterId,
            GlusterStatus status,
            String optionKey,
            String optionValue) {
        List<GlusterVolumeEntity> volumes =
                getCallsHandler().executeReadList("GetGlusterVolumesByOption",
                        volumeRowMapper,
                        getCustomMapSqlParameterSource()
                                .addValue("cluster_id", clusterId)
                                .addValue("status", EnumUtils.nameOrNull(status))
                                .addValue("option_key", optionKey)
                                .addValue("option_val", optionValue));
        fetchRelatedEntities(volumes);
        return volumes;
    }

    @Override
    public List<GlusterVolumeEntity> getAllWithQuery(String query) {
        List<GlusterVolumeEntity> volumes = new SimpleJdbcTemplate(jdbcTemplate).query(query, volumeRowMapper);
        fetchRelatedEntities(volumes);
        return volumes;
    }

    @Override
    public void remove(Guid id) {
        getCallsHandler().executeModification("DeleteGlusterVolumeByGuid",
                createVolumeIdParams(id));
    }

    @Override
    public void removeByName(Guid clusterId, String volName) {
        getCallsHandler().executeModification("DeleteGlusterVolumeByName",
                getCustomMapSqlParameterSource()
                        .addValue("cluster_id", clusterId)
                        .addValue("vol_name", volName));
    }

    @Override
    public void removeAll(Collection<Guid> ids) {
        getCallsHandler().executeModification("DeleteGlusterVolumesByGuids",
                getCustomMapSqlParameterSource().addValue("volume_ids", StringUtils.join(ids, ',')));
    }

    @Override
    public void removeByClusterId(Guid clusterId) {
        getCallsHandler().executeModification("DeleteGlusterVolumesByClusterId",
                getCustomMapSqlParameterSource()
                        .addValue("cluster_id", clusterId));
    }

    @Override
    public void updateVolumeStatus(Guid volumeId, GlusterStatus status) {
        getCallsHandler().executeModification("UpdateGlusterVolumeStatus",
                createVolumeIdParams(volumeId).addValue("status", EnumUtils.nameOrNull(status)));
    }

    @Override
    public void updateVolumeStatusByName(Guid clusterId, String volumeName, GlusterStatus status) {
        getCallsHandler().executeModification("UpdateGlusterVolumeStatusByName",
                getCustomMapSqlParameterSource()
                        .addValue("cluster_id", clusterId)
                        .addValue("vol_name", volumeName)
                        .addValue("status", EnumUtils.nameOrNull(status)));
    }

    @Override
    public void addAccessProtocol(Guid volumeId, AccessProtocol protocol) {
        getCallsHandler().executeModification("InsertGlusterVolumeAccessProtocol",
                createAccessProtocolParams(volumeId, protocol));
    }

    @Override
    public void removeAccessProtocol(Guid volumeId, AccessProtocol protocol) {
        getCallsHandler().executeModification("DeleteGlusterVolumeAccessProtocol",
                createAccessProtocolParams(volumeId, protocol));
    }

    @Override
    public void addTransportType(Guid volumeId, TransportType transportType) {
        getCallsHandler().executeModification("InsertGlusterVolumeTransportType",
                createTransportTypeParams(volumeId, transportType));
    }

    @Override
    public void removeTransportType(Guid volumeId, TransportType transportType) {
        getCallsHandler().executeModification("DeleteGlusterVolumeTransportType",
                createTransportTypeParams(volumeId, transportType));
    }

    private List<AccessProtocol> getAccessProtocolsOfVolume(Guid volumeId) {
        return getCallsHandler().executeReadList(
                "GetAccessProtocolsByGlusterVolumeGuid",
                accessProtocolRowMapper,
                createVolumeIdParams(volumeId));
    }

    private List<TransportType> getTransportTypesOfVolume(Guid volumeId) {
        return getCallsHandler().executeReadList(
                "GetTransportTypesByGlusterVolumeGuid",
                transportTypeRowMapper,
                createVolumeIdParams(volumeId));
    }

    private MapSqlParameterSource createVolumeIdParams(Guid id) {
        return getCustomMapSqlParameterSource().addValue("volume_id", id);
    }

    private MapSqlParameterSource createAccessProtocolParams(Guid volumeId, AccessProtocol protocol) {
        return createVolumeIdParams(volumeId).addValue("access_protocol", EnumUtils.nameOrNull(protocol));
    }

    private MapSqlParameterSource createTransportTypeParams(Guid volumeId, TransportType transportType) {
        return createVolumeIdParams(volumeId).addValue("transport_type", EnumUtils.nameOrNull(transportType));
    }

    private void insertVolumeEntity(GlusterVolumeEntity volume) {
        getCallsHandler().executeModification("InsertGlusterVolume", createFullParametersMapper(volume));
    }

    private void insertVolumeBricks(GlusterVolumeEntity volume) {
        List<GlusterBrickEntity> bricks = volume.getBricks();
        for (GlusterBrickEntity brick : bricks) {
            if (brick.getVolumeId() == null) {
                brick.setVolumeId(volume.getId());
            }
            dbFacade.getGlusterBrickDao().save(brick);
        }
    }

    private void insertVolumeOptions(GlusterVolumeEntity volume) {
        Collection<GlusterVolumeOptionEntity> options = volume.getOptions();
        for (GlusterVolumeOptionEntity option : options) {
            if (option.getVolumeId() == null) {
                option.setVolumeId(volume.getId());
            }
            dbFacade.getGlusterOptionDao().save(option);
        }
    }

    private void insertVolumeAccessProtocols(GlusterVolumeEntity volume) {
        for (AccessProtocol protocol : volume.getAccessProtocols()) {
            addAccessProtocol(volume.getId(), protocol);
        }
    }

    private void insertVolumeTransportTypes(GlusterVolumeEntity volume) {
        for (TransportType transportType : volume.getTransportTypes()) {
            addTransportType(volume.getId(), transportType);
        }
    }

    /**
     * Fetches and populates related entities like bricks, options, access protocols for the given volumes
     *
     * @param volumes
     */
    private void fetchRelatedEntities(List<GlusterVolumeEntity> volumes) {
        for (GlusterVolumeEntity volume : volumes) {
            fetchRelatedEntities(volume);
        }
    }

    /**
     * Fetches and populates related entities like bricks, options, access protocols for the given volume
     *
     * @param volume
     */
    private void fetchRelatedEntities(GlusterVolumeEntity volume) {
        if (volume != null) {
            volume.setBricks(dbFacade.getGlusterBrickDao().getBricksOfVolume(volume.getId()));
            volume.setOptions(dbFacade.getGlusterOptionDao().getOptionsOfVolume(volume.getId()));
            volume.setAccessProtocols(new HashSet<AccessProtocol>(getAccessProtocolsOfVolume(volume.getId())));
            volume.setTransportTypes(new HashSet<TransportType>(getTransportTypesOfVolume(volume.getId())));
        }
    }

    private static final class GlusterVolumeRowMapper implements ParameterizedRowMapper<GlusterVolumeEntity> {
        @Override
        public GlusterVolumeEntity mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            GlusterVolumeEntity entity = new GlusterVolumeEntity();
            entity.setId(Guid.createGuidFromString(rs.getString("id")));
            entity.setClusterId(Guid.createGuidFromString(rs
                    .getString("cluster_id")));
            entity.setName(rs.getString("vol_name"));
            entity.setVolumeType(GlusterVolumeType.valueOf(rs.getString("vol_type")));
            entity.setStatus(GlusterStatus.valueOf(rs.getString("status")));
            entity.setReplicaCount(rs.getInt("replica_count"));
            entity.setStripeCount(rs.getInt("stripe_count"));
            return entity;
        }
    }

    private static final class AccessProtocolRowMapper implements ParameterizedRowMapper<AccessProtocol> {
        @Override
        public AccessProtocol mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            return AccessProtocol.valueOf(rs.getString("access_protocol"));
        }
    }

    private static final class TransportTypeRowMapper implements ParameterizedRowMapper<TransportType> {
        @Override
        public TransportType mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            return TransportType.valueOf(rs.getString("transport_type"));
        }
    }

    private MapSqlParameterSource createReplicaCountParams(Guid volumeId, int replicaCount) {
        return createVolumeIdParams(volumeId).addValue("replica_count", replicaCount);
    }

    private MapSqlParameterSource createStripeCountParams(Guid volumeId, int stripeCount) {
        return createVolumeIdParams(volumeId).addValue("stripe_count", stripeCount);
    }

    @Override
    public void updateReplicaCount(Guid volumeId, int replicaCount) {
        getCallsHandler().executeModification("UpdateReplicaCount", createReplicaCountParams(volumeId, replicaCount));
    }

    @Override
    public void updateStripeCount(Guid volumeId, int stripeCount) {
        getCallsHandler().executeModification("UpdateStripeCount", createStripeCountParams(volumeId, stripeCount));
    }

    @Override
    public void updateGlusterVolume(GlusterVolumeEntity volume) {
        getCallsHandler().executeModification("UpdateGlusterVolume",
                getCustomMapSqlParameterSource()
                        .addValue("id", volume.getId())
                        .addValue("cluster_id", volume.getClusterId())
                        .addValue("vol_name", volume.getName())
                        .addValue("vol_type", EnumUtils.nameOrNull(volume.getVolumeType()))
                        .addValue("status", EnumUtils.nameOrNull(volume.getStatus()))
                        .addValue("replica_count", volume.getReplicaCount())
                        .addValue("stripe_count", volume.getStripeCount()));
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(GlusterVolumeEntity volume) {
        return getCustomMapSqlParameterSource()
                .addValue("id", volume.getId())
                .addValue("cluster_id", volume.getClusterId())
                .addValue("vol_name", volume.getName())
                .addValue("vol_type", EnumUtils.nameOrNull(volume.getVolumeType()))
                .addValue("status", EnumUtils.nameOrNull(volume.getStatus()))
                .addValue("replica_count", volume.getReplicaCount())
                .addValue("stripe_count", volume.getStripeCount());
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(Guid id) {
        return createVolumeIdParams(id);
    }

    @Override
    protected ParameterizedRowMapper<GlusterVolumeEntity> createEntityRowMapper() {
        return volumeRowMapper;
    }

    @Override
    public void addTransportTypes(Guid volumeId, Collection<TransportType> transportTypes) {
        for (TransportType transportType : transportTypes) {
            addTransportType(volumeId, transportType);
        }
    }

    @Override
    public void removeTransportTypes(Guid volumeId, Collection<TransportType> transportTypes) {
        for (TransportType transportType : transportTypes) {
            removeTransportType(volumeId, transportType);
        }
    }
}
