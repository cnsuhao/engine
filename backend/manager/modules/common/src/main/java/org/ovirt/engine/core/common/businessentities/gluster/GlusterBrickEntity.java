package org.ovirt.engine.core.common.businessentities.gluster;

import javax.validation.constraints.NotNull;

import org.ovirt.engine.core.common.businessentities.BusinessEntity;
import org.ovirt.engine.core.common.businessentities.IVdcQueryable;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.common.validation.group.gluster.AddBrick;
import org.ovirt.engine.core.common.validation.group.gluster.RemoveBrick;
import org.ovirt.engine.core.compat.Guid;

/**
 * Brick is the building block of a Gluster Volume. It represents a directory on one of the servers of the cluster, and
 * is typically represented in the form <b>serverName:brickDirectory</b><br>
 * It also has a status (ONLINE / OFFLINE) which represents the status of the brick process that runs on the server to
 * which the brick belongs.
 *
 * @see GlusterVolumeEntity
 * @see GlusterBrickStatus
 */
public class GlusterBrickEntity extends IVdcQueryable implements BusinessEntity<Guid> {
    private static final long serialVersionUID = 7119439284741452278L;

    @NotNull(message = "VALIDATION.GLUSTER.BRICK.ID.NOT_NULL", groups = { RemoveBrick.class })
    private Guid id;

    @NotNull(message = "VALIDATION.GLUSTER.VOLUME.ID.NOT_NULL", groups = { AddBrick.class })
    private Guid volumeId;

    @NotNull(message = "VALIDATION.GLUSTER.VOLUME.BRICK.SERVER_ID.NOT_NULL", groups = { CreateEntity.class })
    private Guid serverId;

    private String serverName;

    @NotNull(message = "VALIDATION.GLUSTER.VOLUME.BRICK.BRICK_DIR.NOT_NULL", groups = { CreateEntity.class })
    private String brickDirectory;

    private GlusterStatus status = GlusterStatus.DOWN;

    private Integer brickOrder;

    public GlusterBrickEntity() {
    }

    public GlusterBrickEntity(Guid volumeId, VdsStatic server, String brickDirectory, GlusterStatus brickStatus) {
        setVolumeId(volumeId);
        setServerId(server.getId());
        setServerName(server.gethost_name());
        setBrickDirectory(brickDirectory);
        setStatus(brickStatus);
    }

    public Guid getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(Guid volumeId) {
        this.volumeId = volumeId;
    }

    public Guid getServerId() {
        return serverId;
    }

    public void setServerId(Guid serverId) {
        this.serverId = serverId;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setBrickDirectory(String brickDirectory) {
        this.brickDirectory = brickDirectory;
    }

    public String getBrickDirectory() {
        return brickDirectory;
    }

    public GlusterStatus getStatus() {
        return status;
    }

    public void setStatus(GlusterStatus status) {
        this.status = status;
    }

    public boolean isOnline() {
        return status == GlusterStatus.UP;
    }

    public String getQualifiedName() {
        return serverName + ":" + brickDirectory;
    }

    @Override
    public String toString() {
        return getQualifiedName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getId().hashCode();
        result = prime * result + ((volumeId == null) ? 0 : volumeId.hashCode());
        result = prime * result + ((serverId == null) ? 0 : serverId.hashCode());
        result = prime * result + ((brickDirectory == null) ? 0 : brickDirectory.hashCode());
        result = prime * result + ((brickOrder == null) ? 0 : brickOrder.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GlusterBrickEntity)) {
            return false;
        }

        GlusterBrickEntity brick = (GlusterBrickEntity) obj;
        return (getId().equals(brick.getId())
                && (ObjectUtils.objectsEqual(volumeId, brick.getVolumeId()))
                && (ObjectUtils.objectsEqual(serverId, brick.getServerId()))
                && (ObjectUtils.objectsEqual(brickDirectory, brick.getBrickDirectory()))
                && (ObjectUtils.objectsEqual(brickOrder, brick.getBrickOrder()))
                && status == brick.getStatus());
    }

    public void copyFrom(GlusterBrickEntity brick) {
        setId(brick.getId());
        setVolumeId(brick.getVolumeId());
        setServerId(brick.getServerId());
        setServerName(brick.getServerName());
        setBrickDirectory(brick.getBrickDirectory());
        setBrickOrder(brick.getBrickOrder());
        setStatus(brick.getStatus());
    }

    /**
     * Generates the id if not present. Volume brick doesn't have an id in
     * GlusterFS, and hence is generated on the backend side.
     * @return id of the brick
     */
    @Override
    public Guid getId() {
        return getId(true);
    }

    public Guid getId(boolean generateIfNull) {
        if(id == null && generateIfNull) {
            id = Guid.NewGuid();
        }
        return id;
    }

    @Override
    public void setId(Guid id) {
        this.id = id;
    }

    @Override
    public Object getQueryableId() {
        return getId();
    }

    public Integer getBrickOrder() {
        return brickOrder;
    }

    public void setBrickOrder(Integer brickOrder) {
        this.brickOrder = brickOrder;
    }
}
