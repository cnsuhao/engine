package org.ovirt.engine.core.bll.gluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.utils.ClusterUtils;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.SetNonOperationalVdsParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.NonOperationalReason;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.gluster.BrickDetails;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServerInfo;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeAdvancedDetails;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeOptionEntity;
import org.ovirt.engine.core.common.businessentities.gluster.TransportType;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.common.utils.ListUtils;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.common.utils.gluster.GlusterCoreUtil;
import org.ovirt.engine.core.common.vdscommands.RemoveVdsVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSParametersBase;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.VdsIdVDSCommandParametersBase;
import org.ovirt.engine.core.common.vdscommands.gluster.GlusterVolumeAdvancedDetailsVDSParameters;
import org.ovirt.engine.core.common.vdscommands.gluster.GlusterVolumesListVDSParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dao.VdsDAO;
import org.ovirt.engine.core.dao.VdsDynamicDAO;
import org.ovirt.engine.core.dao.VdsGroupDAO;
import org.ovirt.engine.core.dao.VdsStaticDAO;
import org.ovirt.engine.core.dao.VdsStatisticsDAO;
import org.ovirt.engine.core.dao.gluster.GlusterBrickDao;
import org.ovirt.engine.core.dao.gluster.GlusterOptionDao;
import org.ovirt.engine.core.dao.gluster.GlusterVolumeDao;
import org.ovirt.engine.core.dao.network.InterfaceDao;
import org.ovirt.engine.core.utils.lock.EngineLock;
import org.ovirt.engine.core.utils.lock.LockManager;
import org.ovirt.engine.core.utils.lock.LockManagerFactory;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.timer.OnTimerMethodAnnotation;
import org.ovirt.engine.core.utils.timer.SchedulerUtil;
import org.ovirt.engine.core.utils.timer.SchedulerUtilQuartzImpl;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

/**
 * This class is responsible for keeping the Gluster related data of engine in sync with the actual data retrieved from
 * GlusterFS. This helps to make sure that any changes done on Gluster servers using the Gluster CLI are propagated to
 * engine as well.
 */
public class GlusterManager {
    private final String ENTITY_BRICK = "brick";
    private final String ENTITY_OPTION = "option";
    private final Log log = LogFactory.getLog(GlusterManager.class);
    private final LockManager lockManager = LockManagerFactory.getLockManager();
    private static final GlusterManager instance = new GlusterManager();

    private GlusterManager() {
    }

    public static GlusterManager getInstance() {
        return instance;
    }

    public void init() {
        if (!glusterModeSupported()) {
            log.debug("Gluster mode not supported. Will not schedule jobs for refreshing Gluster data.");
            return;
        }

        log.debug("Initializing Gluster Manager");

        SchedulerUtil scheduler = SchedulerUtilQuartzImpl.getInstance();

        scheduler.scheduleAFixedDelayJob(this,
                "refreshLightWeightData",
                new Class[0],
                new Object[0],
                getGlusterRefreshRateLight(),
                getGlusterRefreshRateLight(),
                TimeUnit.SECONDS);

        scheduler.scheduleAFixedDelayJob(this,
                "refreshHeavyWeightData",
                new Class[0],
                new Object[0],
                getGlusterRefreshRateHeavy(),
                getGlusterRefreshRateHeavy(),
                TimeUnit.SECONDS);
    }

    private boolean glusterModeSupported() {
        Integer appMode = Config.<Integer> GetValue(ConfigValues.ApplicationMode);
        return ((appMode & ApplicationMode.GlusterOnly.getValue()) > 0);
    }

    /**
     * Acquires a lock on the cluster with given id and locking group {@link LockingGroup#GLUSTER}
     *
     * @param clusterId
     *            ID of the cluster on which the lock is to be acquired
     */
    protected void acquireLock(Guid clusterId) {
        lockManager.acquireLockWait(getEngineLock(clusterId));
    }

    /**
     * Releases the lock held on the cluster having given id and locking group {@link LockingGroup#GLUSTER}
     *
     * @param clusterId
     *            ID of the cluster on which the lock is to be released
     */
    protected void releaseLock(Guid clusterId) {
        lockManager.releaseLock(getEngineLock(clusterId));
    }

    /**
     * Returns an {@link EngineLock} instance that represents a lock on a cluster with given id and the locking group
     * {@link LockingGroup#GLUSTER}
     *
     * @param clusterId
     * @return
     */
    private EngineLock getEngineLock(Guid clusterId) {
        return new EngineLock(Collections.singletonMap(clusterId.toString(),
                LockingGroup.GLUSTER.name()), null);
    }

    /**
     * Refreshes details of all volume across all clusters being managed in the engine. It can end up doing the
     * following in engine DB to make sure that the volume details in engine DB are in sync with GlusterFS:<br>
     * <li>Insert volumes</li><li>Delete volumes</li><li>Update properties of volume e.g. status, volume type</li><li>
     * Add / remove bricks to / from volumes</li><li>Set / Unset volume options</li><br>
     * These are all fetched using the 'volume info' command on gluster CLI, which is relatively lightweight, and hence
     * this method is scheduled more frequently as compared to the other method <code>refreshHeavyWeightData</code>,
     * which uses 'volume status' to fetch and update status of volume bricks.
     */
    @OnTimerMethodAnnotation("refreshLightWeightData")
    public void refreshLightWeightData() {
        log.debug("Refreshing Gluster Data [lightweight]");
        List<VDSGroup> clusters = getClusterDao().getAll();

        for (VDSGroup cluster : clusters) {
            if (cluster.supportsGlusterService()) {
                try {
                    refreshClusterData(cluster);
                } catch (Exception e) {
                    log.errorFormat("Error while refreshing Gluster lightweight data of cluster {0}!",
                            cluster.getname(),
                            e);
                }
            }
        }
    }

    private void refreshClusterData(VDSGroup cluster) {
        log.debugFormat("Refreshing Gluster lightweight Data for cluster {0}", cluster.getname());

        List<VDS> existingServers = getVdsDao().getAllForVdsGroup(cluster.getId());
        VDS upServer = getClusterUtils().getUpServer(cluster.getId());
        if (upServer == null) {
            log.debugFormat("No server UP in cluster {0}. Can't refresh it's data at this point.", cluster.getname());
            return;
        }

        refreshServerData(cluster, upServer, existingServers);
        refreshVolumeData(cluster, upServer, existingServers);
    }

    /**
     * If any servers have been added to the Gluster cluster directly from the Gluster CLI, we still don't add them
     * automatically to the engine DB, as addition of servers requires user approval from the GUI. If the cluster is a
     * gluster-only cluster, and one or more servers have been removed directly from the Gluster CLI, we remove them
     * from the engine DB, and also invoke the corresponding VDS command.
     *
     * @param cluster
     * @param upServer
     * @param existingServers
     */
    private void refreshServerData(VDSGroup cluster, VDS upServer, List<VDS> existingServers) {
        if (cluster.supportsVirtService()) {
            // If the cluster supports virt service as well, we should not be removing any servers from it, even if they
            // have been removed from the Gluster cluster using the Gluster cli, as they could potentially be used for
            // running VMs
            log.debugFormat("As cluster {0} supports virt service as well, it's servers will not be synced with glusterfs",
                    cluster.getname());
            return;
        }

        acquireLock(cluster.getId());
        try {
            List<GlusterServerInfo> fetchedServers = fetchServers(cluster, upServer, existingServers);
            if (fetchedServers != null) {
                removeDetachedServers(existingServers, fetchedServers);
            }
        } finally {
            releaseLock(cluster.getId());
        }
    }

    private void removeDetachedServers(List<VDS> existingServers, List<GlusterServerInfo> fetchedServers) {
        for (VDS server : existingServers) {
            if (isRemovableStatus(server.getstatus()) && serverDetached(server, fetchedServers)) {
                log.debugFormat("Server {0} has been removed directly using the gluster CLI. Removing it from engine as well.",
                        server.getvds_name());
                logServerMessage(server, AuditLogType.GLUSTER_SERVER_REMOVED_FROM_CLI);

                try {
                    removeServerFromDb(server);
                    // remove the server from resource manager
                    runVdsCommand(VDSCommandType.RemoveVds, new RemoveVdsVDSCommandParameters(server.getId()));
                } catch (Exception e) {
                    log.errorFormat("Error while removing server {0} from database!", server.getvds_name(), e);
                }
            }
        }
    }

    private void removeServerFromDb(final VDS server) {
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
            @Override
            public Void runInTransaction() {

                removeVdsStatisticsFromDb(server);
                removeVdsDynamicFromDb(server);
                removeVdsStaticFromDb(server);

                return null;
            }
        });
    }

    protected VDSReturnValue runVdsCommand(VDSCommandType commandType, VDSParametersBase params) {
        return Backend.getInstance().getResourceManager().RunVdsCommand(commandType, params);
    }

    /**
     * We need to be particularly careful about what servers we remove from the DB. A newly added (bootstrapped) server
     * gets peer probed after it's first reboot, and we don't want to accidentally remove such legitimate servers just
     * before they are probed.
     *
     * @param status
     * @return
     */
    private boolean isRemovableStatus(VDSStatus status) {
        switch (status) {
        case Up:
        case Down:
        case Maintenance:
            return true;
        default:
            return false;
        }
    }

    /**
     * Returns true if the given server has been detached i.e. cannot be found in the list of fetched servers.
     *
     * @param server
     * @param fetchedServers
     * @return
     */
    private boolean serverDetached(VDS server, List<GlusterServerInfo> fetchedServers) {
        List<String> vdsIps = getVdsIps(server);
        for (GlusterServerInfo fetchedServer : fetchedServers) {
            if (fetchedServer.getHostnameOrIp().equals(server.gethost_name())
                    || vdsIps.contains(fetchedServer.getHostnameOrIp())) {
                return false;
            }
        }
        return true;
    }

    private List<String> getVdsIps(VDS vds) {
        List<String> vdsIps = new ArrayList<String>();
        for (VdsNetworkInterface iface : getInterfaceDao().getAllInterfacesForVds(vds.getId())) {
            if (iface.getAddress() != null) {
                vdsIps.add(iface.getAddress());
            }
        }
        return vdsIps;
    }

    private List<GlusterServerInfo> fetchServers(VDSGroup cluster, VDS upServer, List<VDS> existingServers) {
        // Create a copy of the existing servers as the fetchServer method can potentially remove elements from it
        List<VDS> tempServers = new ArrayList<VDS>(existingServers);
        List<GlusterServerInfo> fetchedServers = fetchServers(upServer, tempServers);

        if (fetchedServers == null) {
            log.errorFormat("gluster peer status command failed on all servers of the cluster {0}."
                    + "Can't refresh it's data at this point.", cluster.getname());
            return null;
        }

        if (fetchedServers.size() == 1 && existingServers.size() > 2) {
            // It's possible that the server we are using to get list of servers itself has been removed from the
            // cluster, and hence is returning a single server (itself)
            GlusterServerInfo server = fetchedServers.iterator().next();
            if (server.getHostnameOrIp().equals(upServer.gethost_name())
                    || getVdsIps(upServer).contains(server.getHostnameOrIp())) {
                // Find a different UP server, and get servers list from it
                tempServers.remove(upServer);
                upServer = getNewUpServer(tempServers, upServer);
                if (upServer == null) {
                    log.warnFormat("The only UP server in cluster {0} seems to have been removed from it using gluster CLI. "
                            + "Can't refresh it's data at this point.",
                            cluster.getname());
                    return null;
                }

                fetchedServers = fetchServers(upServer, tempServers);
                if (fetchedServers == null) {
                    log.warnFormat("The only UP server in cluster {0} (or the only one on which gluster peer status "
                            + "command is working) seems to have been removed from it using gluster CLI. "
                            + "Can't refresh it's data at this point.", cluster.getname());
                    return null;
                }
            }
        }
        return fetchedServers;
    }

    /**
     * Fetches list of gluster servers by executing the gluster peer status command on the given UP server. If the
     * gluster command fails, tries on other UP servers from the list of existing Servers recursively. Returns null if
     * the command fails on all the servers.
     *
     * @param upServer
     * @param existingServers
     * @return
     */
    private List<GlusterServerInfo> fetchServers(VDS upServer, List<VDS> existingServers) {
        List<GlusterServerInfo> fetchedServers = null;
        while (fetchedServers == null && !existingServers.isEmpty()) {
            fetchedServers = fetchServers(upServer);
            if (fetchedServers == null) {
                logServerMessage(upServer, AuditLogType.GLUSTER_SERVERS_LIST_FAILED);
                // Couldn't fetch servers from the up server. Mark it as non-operational
                setNonOperational(upServer);
                existingServers.remove(upServer);
                upServer = getNewUpServer(existingServers, upServer);
            }
        }
        return fetchedServers;
    }

    private void setNonOperational(VDS server) {
        SetNonOperationalVdsParameters nonOpParams =
                new SetNonOperationalVdsParameters(server.getId(), NonOperationalReason.GLUSTER_COMMAND_FAILED);
        nonOpParams.setSaveToDb(true);
        Backend.getInstance().runInternalAction(VdcActionType.SetNonOperationalVds,
                nonOpParams,
                ExecutionHandler.createInternalJobContext());
    }

    @SuppressWarnings("unchecked")
    protected List<GlusterServerInfo> fetchServers(VDS upServer) {
        VDSReturnValue result =
                Backend.getInstance()
                        .getResourceManager()
                        .RunVdsCommand(VDSCommandType.GlusterServersList,
                                new VdsIdVDSCommandParametersBase(upServer.getId()));

        return result.getSucceeded() ? (List<GlusterServerInfo>) result.getReturnValue() : null;
    }

    /**
     * Returns an UP server from given list of servers, provided it is not same as the given server.
     *
     * @param servers
     * @param exceptServer
     * @return
     */
    private VDS getNewUpServer(List<VDS> servers, VDS exceptServer) {
        for (VDS server : servers) {
            if (server.getstatus() == VDSStatus.Up && !server.getId().equals(exceptServer.getId())) {
                return server;
            }
        }
        return null;
    }

    private void refreshVolumeData(VDSGroup cluster, VDS upServer, List<VDS> existingServers) {
        acquireLock(cluster.getId());
        try {
            // Pass a copy of the existing servers as the fetchVolumes method can potentially remove elements from it
            Map<Guid, GlusterVolumeEntity> volumesMap = fetchVolumes(upServer, new ArrayList<VDS>(existingServers));
            if (volumesMap == null) {
                log.errorFormat("gluster volume info command failed on all servers of the cluster {0}."
                        + "Can't refresh it's data at this point.", cluster.getname());
                return;
            }

            // remove deleted volumes must happen before adding new ones,
            // to handle cases where user deleted a volume and created a
            // new one with same name in a very short time
            removeDeletedVolumes(cluster.getId(), volumesMap);
            updateExistingAndNewVolumes(cluster.getId(), volumesMap);
        } finally {
            releaseLock(cluster.getId());
        }
    }

    /**
     * Fetches list of gluster volumes by executing the gluster volume info command on the given UP server. If the
     * gluster command fails, tries on other UP servers from the list of existing Servers recursively. Returns null if
     * the command fails on all the servers.
     *
     * @param upServer
     * @param existingServers
     * @return
     */
    private Map<Guid, GlusterVolumeEntity> fetchVolumes(VDS upServer, List<VDS> existingServers) {
        Map<Guid, GlusterVolumeEntity> fetchedVolumes = null;
        while (fetchedVolumes == null && existingServers.size() > 0) {
            fetchedVolumes = fetchVolumes(upServer);
            if (fetchedVolumes == null) {
                // Couldn't fetch volumes from the up server. Mark it as non-operational
                logServerMessage(upServer, AuditLogType.GLUSTER_VOLUME_INFO_FAILED);
                setNonOperational(upServer);
                existingServers.remove(upServer);
                upServer = getNewUpServer(existingServers, upServer);
            }
        }
        return fetchedVolumes;
    }

    @SuppressWarnings("unchecked")
    protected Map<Guid, GlusterVolumeEntity> fetchVolumes(VDS upServer) {
        VDSReturnValue result =
                runVdsCommand(VDSCommandType.GlusterVolumesList, new GlusterVolumesListVDSParameters(upServer.getId(),
                        upServer.getvds_group_id()));

        return result.getSucceeded() ? (Map<Guid, GlusterVolumeEntity>) result.getReturnValue() : null;
    }

    private void removeDeletedVolumes(Guid clusterId, Map<Guid, GlusterVolumeEntity> volumesMap) {
        List<Guid> idsToRemove = new ArrayList<Guid>();
        for (GlusterVolumeEntity volume : getVolumeDao().getByClusterId(clusterId)) {
            if (!volumesMap.containsKey(volume.getId())) {
                idsToRemove.add(volume.getId());
                log.debugFormat("Volume {0} has been removed directly using the gluster CLI. Removing it from engine as well.",
                        volume.getName());
                logVolumeMessage(volume, AuditLogType.GLUSTER_VOLUME_DELETED_FROM_CLI);
            }
        }

        if (!idsToRemove.isEmpty()) {
            try {
                getVolumeDao().removeAll(idsToRemove);
            } catch (Exception e) {
                log.errorFormat("Error while removing volumes from database!", e);
            }
        }
    }

    private void updateExistingAndNewVolumes(Guid clusterId, Map<Guid, GlusterVolumeEntity> volumesMap) {
        for (Entry<Guid, GlusterVolumeEntity> entry : volumesMap.entrySet()) {
            GlusterVolumeEntity volume = entry.getValue();
            log.debugFormat("Analyzing volume {0}", volume.getName());

            GlusterVolumeEntity existingVolume = getVolumeDao().getById(entry.getKey());
            if (existingVolume == null) {
                try {
                    createVolume(volume);
                } catch (Exception e) {
                    log.errorFormat("Could not save volume {0} in database!", volume.getName(), e);
                }
            } else {
                try {
                    log.debugFormat("Volume {0} exists in engine. Checking if it needs to be updated.",
                            existingVolume.getName());
                    updateVolume(existingVolume, volume);
                } catch (Exception e) {
                    log.errorFormat("Error while updating Volume {0}!", volume.getName(), e);
                }
            }
        }
    }

    /**
     * Creates a new volume in engine
     *
     * @param volume
     */
    private void createVolume(final GlusterVolumeEntity volume) {
        if (volume.getBricks() == null) {
            log.warnFormat("Bricks of volume {0} were not fetched. " +
                    "Hence will not add it to engine at this point.", volume.getName());
            return;
        }

        for (GlusterBrickEntity brick : volume.getBricks()) {
            if (brick.getServerId() == null) {
                log.warnFormat("Volume {0} contains brick(s) from unknown hosts. " +
                        "Hence will not add it to engine at this point.",
                        volume.getName());
                return;
            }
        }

        logVolumeMessage(volume, AuditLogType.GLUSTER_VOLUME_CREATED_FROM_CLI);
        log.debugFormat("Volume {0} has been created directly using the gluster CLI. Creating it in engine as well.",
                volume.getName());
        getVolumeDao().save(volume);
    }

    private void logVolumeMessage(final GlusterVolumeEntity volume, final AuditLogType logType) {
        logAuditMessage(volume.getClusterId(), volume, null, logType, null, null);
    }

    protected void logServerMessage(final VDS server, final AuditLogType logType) {
        logAuditMessage(null, null, server, logType, null, null);
    }

    @SuppressWarnings("serial")
    protected void logAuditMessage(final Guid clusterId,
            final GlusterVolumeEntity volume,
            final VDS server,
            final AuditLogType logType,
            final String entityName,
            final String entityValue) {
        AuditLogDirector.log(new AuditLogableBase() {
            @Override
            protected VDS getVds() {
                return server;
            }

            @Override
            public Guid getVdsGroupId() {
                return clusterId;
            }

            @Override
            protected GlusterVolumeEntity getGlusterVolume() {
                return volume;
            }

            @Override
            public AuditLogType getAuditLogTypeValue() {
                return logType;
            }

            @Override
            public Map<String, String> getCustomValues() {
                if (entityName != null && entityValue != null) {
                    return Collections.singletonMap(entityName, entityValue);
                } else {
                    return new HashMap<String, String>();
                }
            }
        });
    }

    private void updateVolume(GlusterVolumeEntity existingVolume, GlusterVolumeEntity fetchedVolume) {
        updateVolumeProperties(existingVolume, fetchedVolume);
        updateBricks(existingVolume, fetchedVolume);
        updateOptions(existingVolume, fetchedVolume);
        updateTransportTypes(existingVolume, fetchedVolume);
    }

    private void updateTransportTypes(GlusterVolumeEntity existingVolume, GlusterVolumeEntity fetchedVolume) {
        Set<TransportType> existingTransportTypes = existingVolume.getTransportTypes();
        Set<TransportType> fetchedTransportTypes = fetchedVolume.getTransportTypes();
        if (ListUtils.listsEqual(existingTransportTypes, fetchedTransportTypes)) {
            // transport types not changed. return without updating DB.
            return;
        }

        Collection<TransportType> addedTransportTypes =
                ListUtils.getAddedElements(existingTransportTypes, fetchedTransportTypes);
        if (!addedTransportTypes.isEmpty()) {
            log.infoFormat("Adding transport type(s) {0} to volume {1}",
                    addedTransportTypes,
                    existingVolume.getName());
            getVolumeDao().addTransportTypes(existingVolume.getId(), addedTransportTypes);
        }

        Collection<TransportType> removedTransportTypes =
                ListUtils.getAddedElements(fetchedTransportTypes, existingTransportTypes);
        if (!removedTransportTypes.isEmpty()) {
            log.infoFormat("Removing transport type(s) {0} from volume {1}",
                    removedTransportTypes,
                    existingVolume.getName());
            getVolumeDao().removeTransportTypes(existingVolume.getId(), removedTransportTypes);
        }
    }

    private void updateBricks(GlusterVolumeEntity existingVolume, GlusterVolumeEntity fetchedVolume) {
        List<GlusterBrickEntity> fetchedBricks = fetchedVolume.getBricks();
        if (fetchedBricks == null) {
            log.warnFormat("Bricks of volume {0} were not fetched. " +
                    "Hence will not try to update them in engine at this point.",
                    fetchedVolume.getName());
            return;
        }

        removeDeletedBricks(existingVolume, fetchedBricks);
        updateExistingAndNewBricks(existingVolume, fetchedBricks);
    }

    private void removeDeletedBricks(GlusterVolumeEntity existingVolume, List<GlusterBrickEntity> fetchedBricks) {
        List<Guid> idsToRemove = new ArrayList<Guid>();
        for (GlusterBrickEntity existingBrick : existingVolume.getBricks()) {
            if (!GlusterCoreUtil.containsBrick(fetchedBricks, existingBrick)) {
                idsToRemove.add(existingBrick.getId());
                log.infoFormat("Brick {0} removed from volume {1} from CLI. Removing it from engine DB as well.",
                        existingBrick.getQualifiedName(),
                        existingVolume.getName());
                logAuditMessage(existingVolume.getClusterId(), existingVolume, null,
                        AuditLogType.GLUSTER_VOLUME_BRICK_REMOVED_FROM_CLI,
                        ENTITY_BRICK,
                        existingBrick.getQualifiedName());
            }
        }
        if (!idsToRemove.isEmpty()) {
            try {
                getBrickDao().removeAll(idsToRemove);
            } catch (Exception e) {
                log.errorFormat("Error while removing bricks from database!", e);
            }
        }
    }

    private void updateExistingAndNewBricks(GlusterVolumeEntity existingVolume, List<GlusterBrickEntity> fetchedBricks) {
        for (GlusterBrickEntity fetchedBrick : fetchedBricks) {
            GlusterBrickEntity existingBrick = GlusterCoreUtil.findBrick(existingVolume.getBricks(), fetchedBrick);
            if (existingBrick == null) {
                // server id could be null if the new brick resides on a server that is not yet added in the engine
                // adding such servers to engine required manual approval by user, and hence can't be automated.
                if (fetchedBrick.getServerId() != null) {
                    log.infoFormat("New brick {0} added to volume {1} from gluster CLI. Updating engine DB accordingly.",
                            fetchedBrick.getQualifiedName(),
                            existingVolume.getName());
                    fetchedBrick.setStatus(existingVolume.isOnline() ? GlusterStatus.UP : GlusterStatus.DOWN);
                    getBrickDao().save(fetchedBrick);
                    logAuditMessage(existingVolume.getClusterId(), existingVolume, null,
                            AuditLogType.GLUSTER_VOLUME_BRICK_ADDED_FROM_CLI,
                            ENTITY_BRICK,
                            fetchedBrick.getQualifiedName());
                }
            } else {
                // brick found. update it if required. Only property that could be different is the brick order
                if (!ObjectUtils.objectsEqual(existingBrick.getBrickOrder(), fetchedBrick.getBrickOrder())) {
                    log.infoFormat("Brick order for brick {0} changed from {1} to {2} because of direct CLI operations. Updating engine DB accordingly.",
                            existingBrick.getQualifiedName(),
                            existingBrick.getBrickOrder(),
                            fetchedBrick.getBrickOrder());
                    getBrickDao().updateBrickOrder(existingBrick.getId(), fetchedBrick.getBrickOrder());
                }
            }
        }
    }

    private void updateOptions(GlusterVolumeEntity existingVolume, GlusterVolumeEntity fetchedVolume) {
        Collection<GlusterVolumeOptionEntity> existingOptions = existingVolume.getOptions();
        Collection<GlusterVolumeOptionEntity> fetchedOptions = fetchedVolume.getOptions();

        updateExistingAndNewOptions(existingVolume, fetchedOptions);
        removeDeletedOptions(fetchedVolume, existingOptions);
    }

    private void removeDeletedOptions(GlusterVolumeEntity fetchedVolume,
            Collection<GlusterVolumeOptionEntity> existingOptions) {
        List<Guid> idsToRemove = new ArrayList<Guid>();
        for (GlusterVolumeOptionEntity existingOption : existingOptions) {
            if (fetchedVolume.getOption(existingOption.getKey()) == null) {
                idsToRemove.add(existingOption.getId());
                log.infoFormat("Option {0} unset on volume {1} from CLI. Removing it from engine DB as well.",
                        existingOption.getKey(),
                        fetchedVolume.getName());
                logAuditMessage(fetchedVolume.getClusterId(), fetchedVolume, null,
                        AuditLogType.GLUSTER_VOLUME_OPTION_RESET_FROM_CLI,
                        ENTITY_OPTION,
                        existingOption.getKey());
            }
        }
        if (!idsToRemove.isEmpty()) {
            try {
                getOptionDao().removeAll(idsToRemove);
            } catch (Exception e) {
                log.errorFormat("Error while removing options of volume {0} from database!", fetchedVolume.getName(), e);
            }
        }
    }

    private void updateExistingAndNewOptions(GlusterVolumeEntity existingVolume,
            Collection<GlusterVolumeOptionEntity> fetchedOptions) {
        for (GlusterVolumeOptionEntity fetchedOption : fetchedOptions) {
            GlusterVolumeOptionEntity existingOption = existingVolume.getOption(fetchedOption.getKey());
            if (existingOption == null) {
                logAuditMessage(existingVolume.getClusterId(), existingVolume, null,
                        AuditLogType.GLUSTER_VOLUME_OPTION_SET_FROM_CLI,
                        ENTITY_OPTION,
                        fetchedOption.toString());
                log.infoFormat("New option {0}={1} set on volume {2} from gluster CLI. Updating engine DB accordingly.",
                        fetchedOption.getKey(),
                        fetchedOption.getValue(),
                        existingVolume.getName());
                try {
                    getOptionDao().save(fetchedOption);
                } catch (Exception e) {
                    log.errorFormat("Could not save option {0} of volume {1) to database!",
                            fetchedOption,
                            existingVolume.getName(),
                            e);
                }
            } else if (!existingOption.getValue().equals(fetchedOption.getValue())) {
                logAuditMessage(existingVolume.getClusterId(), existingVolume, null,
                        AuditLogType.GLUSTER_VOLUME_OPTION_SET_FROM_CLI,
                        ENTITY_OPTION,
                        fetchedOption.toString());
                log.infoFormat("Value of option {0} of volume {1} changed from {2} to {3} from CLI. Updating engine DB accordingly.",
                        existingOption.getKey(),
                        existingVolume.getName(),
                        existingOption.getValue(),
                        fetchedOption.getValue());
                try {
                    getOptionDao().updateVolumeOption(existingOption.getId(), fetchedOption.getValue());
                } catch (Exception e) {
                    log.errorFormat("Error while updating option {0} of volume {1} in database!",
                            fetchedOption,
                            existingVolume.getName(),
                            e);
                }
            }
        }
    }

    /**
     * Updates basic properties of the volume. Does not include bricks, options, and transport types
     *
     * @param existingVolume
     *            Volume that is to be updated
     * @param fetchedVolume
     *            Volume fetched from GlusterFS, containing latest properties
     */
    @SuppressWarnings("incomplete-switch")
    public void updateVolumeProperties(GlusterVolumeEntity existingVolume, GlusterVolumeEntity fetchedVolume) {
        boolean changed = false;

        if (existingVolume.getVolumeType() != fetchedVolume.getVolumeType()) {
            existingVolume.setVolumeType(fetchedVolume.getVolumeType());
            changed = true;
        }

        switch (existingVolume.getVolumeType()) {
        case REPLICATE:
        case DISTRIBUTED_REPLICATE:
            if (!ObjectUtils.objectsEqual(existingVolume.getReplicaCount(), fetchedVolume.getReplicaCount())) {
                existingVolume.setReplicaCount(fetchedVolume.getReplicaCount());
                changed = true;
            }
            break;
        case STRIPE:
        case DISTRIBUTED_STRIPE:
            if (!ObjectUtils.objectsEqual(existingVolume.getStripeCount(), fetchedVolume.getStripeCount())) {
                existingVolume.setStripeCount(fetchedVolume.getStripeCount());
                changed = true;
            }
            break;
        }

        if (existingVolume.getStatus() != fetchedVolume.getStatus()) {
            existingVolume.setStatus(fetchedVolume.getStatus());
            changed = true;
        }

        if (changed) {
            log.infoFormat("Updating volume {0} with fetched properties.", existingVolume.getName());
            getVolumeDao().updateGlusterVolume(existingVolume);
            logVolumeMessage(existingVolume, AuditLogType.GLUSTER_VOLUME_PROPERTIES_CHANGED_FROM_CLI);
        }
    }

    /**
     * Refreshes the brick statuses from GlusterFS. This method is scheduled less frequently as it uses the 'volume
     * status' command, that adds significant overhead on Gluster processes, and hence should not be invoked too
     * frequently.
     */
    @OnTimerMethodAnnotation("refreshHeavyWeightData")
    public void refreshHeavyWeightData() {
        log.debug("Refreshing Gluster Data [heavyweight]");

        for (VDSGroup cluster : getClusterDao().getAll()) {
            try {
                refreshClusterHeavyWeightData(cluster);
            } catch (Exception e) {
                log.errorFormat("Error while refreshing Gluster heavyweight data of cluster {0}!",
                        cluster.getname(),
                        e);
            }
        }
    }

    private void refreshClusterHeavyWeightData(VDSGroup cluster) {
        VDS upServer = getClusterUtils().getUpServer(cluster.getId());
        if (upServer == null) {
            log.debugFormat("No server UP in cluster {0}. Can't refresh it's data at this point.", cluster.getname());
            return;
        }

        if (cluster.supportsGlusterService()) {
            for (GlusterVolumeEntity volume : getVolumeDao().getByClusterId(cluster.getId())) {
                log.debugFormat("Refreshing brick statuses for volume {0} of cluster {1}",
                        volume.getName(),
                        cluster.getname());
                acquireLock(cluster.getId());
                try {
                    refreshBrickStatuses(upServer, volume);
                } catch (Exception e) {
                    log.errorFormat("Error while refreshing brick statuses for volume {0} of cluster {1}",
                            volume.getName(),
                            cluster.getname(),
                            e);
                } finally {
                    releaseLock(cluster.getId());
                }
            }
        }
    }

    private void refreshBrickStatuses(VDS upServer, GlusterVolumeEntity volume) {
        List<GlusterBrickEntity> bricksToUpdate = new ArrayList<GlusterBrickEntity>();
        Map<Guid, GlusterStatus> brickStatusMap =
                getBrickStatusMap(getVolumeAdvancedDetails(upServer, volume.getClusterId(), volume.getName()));
        for (GlusterBrickEntity brick : volume.getBricks()) {
            GlusterStatus fetchedStatus = brickStatusMap.get(brick.getId());
            // if fetchedStatus is null, it means this is a new brick added from gluster cli and doesn't exist in engine
            // DB yet. Don't do anything, wait for it to be added by the 'lightweight' refresh job
            if (fetchedStatus != null && fetchedStatus != brick.getStatus()) {
                brick.setStatus(fetchedStatus);
                bricksToUpdate.add(brick);
            }
        }

        if (!bricksToUpdate.isEmpty()) {
            getBrickDao().updateBrickStatuses(bricksToUpdate);
        }
    }

    private Map<Guid, GlusterStatus> getBrickStatusMap(GlusterVolumeAdvancedDetails volumeDetails) {
        Map<Guid, GlusterStatus> brickStatusMap = new HashMap<Guid, GlusterStatus>();
        for (BrickDetails brickDetails : volumeDetails.getBrickDetails()) {
            brickStatusMap.put(brickDetails.getBrickProperties().getBrickId(), brickDetails.getBrickProperties()
                    .getStatus());
        }
        return brickStatusMap;
    }

    protected GlusterVolumeAdvancedDetails getVolumeAdvancedDetails(VDS upServer, Guid clusterId, String volumeName) {
        VDSReturnValue result = runVdsCommand(VDSCommandType.GetGlusterVolumeAdvancedDetails,
                new GlusterVolumeAdvancedDetailsVDSParameters(upServer.getId(),
                        clusterId,
                        volumeName,
                        null,
                        false));

        return result.getSucceeded() ? (GlusterVolumeAdvancedDetails) result.getReturnValue() : null;
    }

    private void removeVdsStatisticsFromDb(VDS server) {
        getVdsStatisticsDao().remove(server.getId());
    }

    private void removeVdsStaticFromDb(VDS server) {
        getVdsStaticDao().remove(server.getId());
    }

    private void removeVdsDynamicFromDb(VDS server) {
        getVdsDynamicDao().remove(server.getId());
    }

    protected ClusterUtils getClusterUtils() {
        return ClusterUtils.getInstance();
    }

    protected VdsStatisticsDAO getVdsStatisticsDao() {
        return DbFacade.getInstance().getVdsStatisticsDao();
    }

    protected VdsStaticDAO getVdsStaticDao() {
        return DbFacade.getInstance().getVdsStaticDao();
    }

    protected VdsDynamicDAO getVdsDynamicDao() {
        return DbFacade.getInstance().getVdsDynamicDao();
    }

    protected InterfaceDao getInterfaceDao() {
        return DbFacade.getInstance().getInterfaceDao();
    }

    protected VdsGroupDAO getClusterDao() {
        return DbFacade.getInstance().getVdsGroupDao();
    }

    protected VdsDAO getVdsDao() {
        return DbFacade.getInstance().getVdsDao();
    }

    protected GlusterVolumeDao getVolumeDao() {
        return DbFacade.getInstance().getGlusterVolumeDao();
    }

    protected GlusterOptionDao getOptionDao() {
        return DbFacade.getInstance().getGlusterOptionDao();
    }

    protected GlusterBrickDao getBrickDao() {
        return DbFacade.getInstance().getGlusterBrickDao();
    }

    private int getGlusterRefreshRateLight() {
        return Config.<Integer> GetValue(ConfigValues.GlusterRefreshRateLight);
    }

    private int getGlusterRefreshRateHeavy() {
        return Config.<Integer> GetValue(ConfigValues.GlusterRefreshRateHeavy);
    }
}
