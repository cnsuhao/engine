package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.ovirt.engine.core.common.businessentities.Image;
import org.ovirt.engine.core.common.businessentities.ImageStatus;
import org.ovirt.engine.core.common.businessentities.VolumeFormat;
import org.ovirt.engine.core.common.businessentities.VolumeType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.NGuid;
import org.ovirt.engine.core.dal.dbbroker.DbFacadeUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

public class ImageDaoDbFacadeImpl extends DefaultGenericDaoDbFacade<Image, Guid> implements ImageDao {

    public ImageDaoDbFacadeImpl() {
        super("Image");
    }

    @Override
    public void updateStatus(Guid id, ImageStatus status) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("image_id", id)
                .addValue("status", status);
        getCallsHandler().executeModification("UpdateImageStatus", parameterSource);
    }

    @Override
    public void updateImageVmSnapshotId(Guid id, Guid vmSnapshotId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("image_id", id)
                .addValue("vm_snapshot_id", vmSnapshotId);
        getCallsHandler().executeModification("UpdateImageVmSnapshotId", parameterSource);
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(Image entity) {
        return createIdParameterMapper(entity.getId())
                .addValue("creation_date", entity.getCreationDate())
                .addValue("it_guid", entity.getTemplateImageId())
                .addValue("size", entity.getSize())
                .addValue("ParentId", entity.getParentId())
                .addValue("imageStatus", entity.getStatus())
                .addValue("lastModified", entity.getLastModified())
                .addValue("vm_snapshot_id", entity.getSnapshotId())
                .addValue("volume_type", entity.getVolumeType())
                .addValue("volume_format", entity.getVolumeFormat())
                .addValue("image_group_id", entity.getDiskId())
                .addValue("active", entity.isActive())
                .addValue("quota_id", entity.getQuotaId());
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(Guid id) {
        return getCustomMapSqlParameterSource().addValue("image_guid", id);
    }

    @Override
    protected ParameterizedRowMapper<Image> createEntityRowMapper() {
        return ImageRowMapper.instance;
    }

    private static class ImageRowMapper implements ParameterizedRowMapper<Image> {

        public static ImageRowMapper instance = new ImageRowMapper();

        private ImageRowMapper() {
        }

        @Override
        public Image mapRow(ResultSet rs, int rowNum) throws SQLException {
            Image entity = new Image();
            entity.setId(Guid.createGuidFromString(rs.getString("image_guid")));
            entity.setCreationDate(DbFacadeUtils.fromDate(rs.getTimestamp("creation_date")));
            entity.setTemplateImageId(Guid.createGuidFromString(rs.getString("it_guid")));
            entity.setSize(rs.getLong("size"));
            entity.setParentId(Guid.createGuidFromString(rs.getString("ParentId")));
            entity.setStatus(ImageStatus.forValue(rs.getInt("imageStatus")));
            entity.setLastModified(DbFacadeUtils.fromDate(rs.getTimestamp("lastModified")));
            entity.setSnapshotId(Guid.createGuidFromString(rs.getString("vm_snapshot_id")));
            entity.setVolumeType(VolumeType.forValue(rs.getInt("volume_type")));
            entity.setVolumeFormat(VolumeFormat.forValue(rs.getInt("volume_format")));
            entity.setDiskId(Guid.createGuidFromString(rs.getString("image_group_id")));
            entity.setQuotaId(Guid.createGuidFromString(rs.getString("quota_id")));
            entity.setActive((Boolean) rs.getObject("active"));
            return entity;
        }
    }

    @Override
    public void updateQuotaForImageAndSnapshots(Guid imageGroupId, NGuid quotaId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("image_group_id", imageGroupId)
                .addValue("quota_id", quotaId);
        getCallsHandler().executeModification("updateQuotaForImageAndSnapshots", parameterSource);
    }
}
