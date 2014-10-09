package org.ovirt.engine.core.bll.gluster;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.bll.LockIdNameAttribute;
import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.gluster.CreateGlusterVolumeParameters;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeOptionParameters;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.gluster.AccessProtocol;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeOptionEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeType;
import org.ovirt.engine.core.common.businessentities.gluster.TransportType;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.common.validation.group.gluster.CreateReplicatedVolume;
import org.ovirt.engine.core.common.validation.group.gluster.CreateStripedVolume;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.gluster.CreateGlusterVolumeVDSParameters;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

/**
 * BLL command to create a new Gluster Volume
 */
@NonTransactiveCommandAttribute
@LockIdNameAttribute(isWait = true)
public class CreateGlusterVolumeCommand extends GlusterCommandBase<CreateGlusterVolumeParameters> {

    private static final long serialVersionUID = 7432566785114684972L;
    private GlusterVolumeEntity volume;

    public CreateGlusterVolumeCommand(CreateGlusterVolumeParameters params) {
        super(params);
        volume = getParameters().getVolume();
        setVdsGroupId(volume.getClusterId());
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__CREATE);
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__GLUSTER_VOLUME);
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        switch (volume.getVolumeType()) {
        case REPLICATE:
        case DISTRIBUTED_REPLICATE:
            addValidationGroup(CreateReplicatedVolume.class);
            break;
        case STRIPE:
        case DISTRIBUTED_STRIPE:
            addValidationGroup(CreateStripedVolume.class);
            break;
        default:
            addValidationGroup(CreateEntity.class);
        }
        return super.getValidationGroups();
    }

    @Override
    protected boolean canDoAction() {
        if (!super.canDoAction()) {
            return false;
        }

        VDSGroup cluster = getVdsGroup();
        if (cluster == null) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_CLUSTER_IS_NOT_VALID);
            return false;
        }

        if (!cluster.supportsGlusterService()) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_CLUSTER_DOES_NOT_SUPPORT_GLUSTER);
            return false;
        }

        if (volumeNameExists(volume.getName())) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_GLUSTER_VOLUME_NAME_ALREADY_EXISTS);
            addCanDoActionMessage(String.format("$volumeName %1$s", volume.getName()));
            return false;
        }

        return validateBricks(volume);
    }

    private boolean volumeNameExists(String volumeName) {
        GlusterVolumeEntity volumeEntity = getGlusterVolumeDao().getByName(getVdsGroupId(), volumeName);
        return (volumeEntity == null) ? false : true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ovirt.engine.core.bll.CommandBase#executeCommand()
     */
    @Override
    protected void executeCommand() {
        // set the gluster volume name for audit purpose
        setGlusterVolumeName(volume.getName());

        if(volume.getTransportTypes() == null || volume.getTransportTypes().isEmpty()) {
            volume.addTransportType(TransportType.TCP);
        }

        // GLUSTER access protocol is enabled by default
        volume.addAccessProtocol(AccessProtocol.GLUSTER);
        if (!volume.getAccessProtocols().contains(AccessProtocol.NFS)) {
            volume.disableNFS();
        }

        if (volume.getAccessProtocols().contains(AccessProtocol.CIFS)) {
            volume.enableCifs();
        }

        VDSReturnValue returnValue = runVdsCommand(
                VDSCommandType.CreateGlusterVolume,
                new CreateGlusterVolumeVDSParameters(upServer.getId(), volume));
        setSucceeded(returnValue.getSucceeded());

        if(!getSucceeded()) {
            handleVdsError(AuditLogType.GLUSTER_VOLUME_CREATE_FAILED, returnValue.getVdsError().getMessage());
            return;
        }

        // Volume created successfully. Insert it to database.
        GlusterVolumeEntity createdVolume = (GlusterVolumeEntity) returnValue.getReturnValue();
        setVolumeType(createdVolume);
        setBrickOrder(createdVolume.getBricks());
        addVolumeToDb(createdVolume);

        // set all options of the volume
        setVolumeOptions(createdVolume);

        getReturnValue().setActionReturnValue(createdVolume.getId());
    }

    private void setVolumeType(GlusterVolumeEntity createdVolume) {
        if (createdVolume.getVolumeType() == GlusterVolumeType.REPLICATE &&
                createdVolume.getBricks().size() > createdVolume.getReplicaCount()) {
            createdVolume.setVolumeType(GlusterVolumeType.DISTRIBUTED_REPLICATE);
        } else if (createdVolume.getVolumeType() == GlusterVolumeType.DISTRIBUTED_REPLICATE &&
                createdVolume.getBricks().size() == createdVolume.getReplicaCount()) {
            createdVolume.setVolumeType(GlusterVolumeType.REPLICATE);
        } else if (createdVolume.getVolumeType() == GlusterVolumeType.STRIPE &&
                createdVolume.getBricks().size() > createdVolume.getStripeCount()) {
            createdVolume.setVolumeType(GlusterVolumeType.DISTRIBUTED_STRIPE);
        } else if (createdVolume.getVolumeType() == GlusterVolumeType.DISTRIBUTED_STRIPE &&
                createdVolume.getBricks().size() == createdVolume.getStripeCount()) {
            createdVolume.setVolumeType(GlusterVolumeType.STRIPE);
        }
    }

    /**
     * Sets all options of a volume by invoking the action {@link VdcActionType#SetGlusterVolumeOption} in a loop. <br>
     * Errors if any are collected and added to "executeFailedMessages"
     *
     * @param volume
     */
    private void setVolumeOptions(GlusterVolumeEntity volume) {
        List<String> errors = new ArrayList<String>();
        for (GlusterVolumeOptionEntity option : volume.getOptions()) {
            // make sure that volume id is set
            option.setVolumeId(volume.getId());
            VdcReturnValueBase setOptionReturnValue =
                    runBllAction(
                            VdcActionType.SetGlusterVolumeOption,
                            new GlusterVolumeOptionParameters(option));
            if (!getSucceeded()) {
                errors.addAll(setOptionReturnValue.getCanDoActionMessages());
                errors.addAll(setOptionReturnValue.getExecuteFailedMessages());
            }
        }

        if (!errors.isEmpty()) {
            handleVdsErrors(AuditLogType.GLUSTER_VOLUME_OPTION_SET_FAILED, errors);
        }
    }

    /**
     * Validates the the number of bricks against the replica count or stripe count based on volume type
     *
     * @param volume
     * @return
     */
    private boolean validateBricks(GlusterVolumeEntity volume) {
        List<GlusterBrickEntity> bricks = volume.getBricks();
        if (bricks.isEmpty()) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_BRICKS_REQUIRED);
            return false;
        }

        int brickCount = bricks.size();
        int replicaCount = volume.getReplicaCount();
        int stripeCount = volume.getStripeCount();

        switch (volume.getVolumeType()) {
        case REPLICATE:
            if (replicaCount < 2) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_REPLICA_COUNT_MIN_2);
                return false;
            }

            if (brickCount != replicaCount) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_INVALID_BRICK_COUNT_FOR_REPLICATE);
                return false;
            }
            break;
        case DISTRIBUTED_REPLICATE:
            if (replicaCount < 2) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_REPLICA_COUNT_MIN_2);
                return false;
            }
            if (brickCount < replicaCount || Math.IEEEremainder(brickCount, replicaCount) != 0) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_INVALID_BRICK_COUNT_FOR_DISTRIBUTED_REPLICATE);
                return false;
            }
            break;
        case STRIPE:
            if (stripeCount < 4) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_STRIPE_COUNT_MIN_4);
                return false;
            }
            if (brickCount != stripeCount) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_INVALID_BRICK_COUNT_FOR_STRIPE);
                return false;
            }
            break;
        case DISTRIBUTED_STRIPE:
            if (stripeCount < 4) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_STRIPE_COUNT_MIN_4);
                return false;
            }
            if (brickCount < stripeCount || Math.IEEEremainder(brickCount, stripeCount) != 0) {
                addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_INVALID_BRICK_COUNT_FOR_DISTRIBUTED_STRIPE);
                return false;
            }
            break;
        }

        return updateBrickServerNames(bricks, true);
    }

    private void setBrickOrder(List<GlusterBrickEntity> bricks) {
        for (int i = 0; i < bricks.size(); i++) {
            bricks.get(i).setBrickOrder(i);
        }
    }

    private void addVolumeToDb(final GlusterVolumeEntity createdVolume) {
        // volume fetched from VDSM doesn't contain cluster id as
        // GlusterFS is not aware of multiple clusters
        createdVolume.setClusterId(getVdsGroupId());
        DbFacade.getInstance().getGlusterVolumeDao().save(createdVolume);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (getSucceeded()) {
            return AuditLogType.GLUSTER_VOLUME_CREATE;
        } else {
            return errorType == null ? AuditLogType.GLUSTER_VOLUME_CREATE_FAILED : errorType;
        }
    }
}
