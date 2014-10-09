package org.ovirt.engine.core.vdsbroker.gluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.TransportType;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.VdsDAO;
import org.ovirt.engine.core.dao.network.InterfaceDao;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.vdsbroker.irsbroker.StatusReturnForXmlRpc;

/**
 * The XmlRpc return type to receive a list of gluster volumes. The constructor takes cluster id as well, so that
 * correct host can be identified when populating the bricks of a volume
 */
public final class GlusterVolumesListReturnForXmlRpc extends StatusReturnForXmlRpc {
    private static final String VOLUMES = "volumes";
    private static final String VOLUME_NAME = "volumeName";
    private static final String UUID = "uuid";
    private static final String VOLUME_TYPE = "volumeType";
    private static final String TRANSPORT_TYPE = "transportType";
    private static final String VOLUME_STATUS = "volumeStatus";
    private static final String BRICKS = "bricks";
    private static final String OPTIONS = "options";
    private static final String VOLUME_STATUS_ONLINE = "ONLINE";
    private static final String REPLICA_COUNT = "replicaCount";
    private static final String STRIPE_COUNT = "stripeCount";

    private Guid clusterId;
    private Map<Guid, GlusterVolumeEntity> volumes = new HashMap<Guid, GlusterVolumeEntity>();
    private static Log log = LogFactory.getLog(GlusterVolumesListReturnForXmlRpc.class);

    @SuppressWarnings("unchecked")
    public GlusterVolumesListReturnForXmlRpc(Guid clusterId, Map<String, Object> innerMap) {
        super(innerMap);
        this.clusterId = clusterId;

        if(mStatus.mCode != 0) {
            return;
        }

        Map<String, Object> volumesMap = (Map<String, Object>) innerMap.get(VOLUMES);

        for (Entry<String, Object> entry : volumesMap.entrySet()) {
            log.debugFormat("received volume {0}", entry.getKey());

            GlusterVolumeEntity volume = getVolume((Map<String, Object>)entry.getValue());
            volumes.put(volume.getId(), volume);
        }
    }

    @SuppressWarnings({ "unchecked", "incomplete-switch" })
    private GlusterVolumeEntity getVolume(Map<String, Object> map) {
        GlusterVolumeEntity volume = new GlusterVolumeEntity();

        volume.setClusterId(clusterId);
        volume.setId(Guid.createGuidFromString((String)map.get(UUID)));
        volume.setName((String)map.get(VOLUME_NAME));
        volume.setVolumeType((String)map.get(VOLUME_TYPE));

        switch (volume.getVolumeType()) {
        case REPLICATE:
        case DISTRIBUTED_REPLICATE:
            volume.setReplicaCount(Integer.valueOf((String) map.get(REPLICA_COUNT)));
            break;
        case STRIPE:
        case DISTRIBUTED_STRIPE:
            volume.setStripeCount(Integer.valueOf((String) map.get(STRIPE_COUNT)));
            break;
        }

        for(Object transportType : (Object[])map.get(TRANSPORT_TYPE)) {
            volume.addTransportType(TransportType.valueOf((String)transportType));
        }

        String volStatus = (String)map.get(VOLUME_STATUS);
        if(volStatus.toUpperCase().equals(VOLUME_STATUS_ONLINE)) {
            volume.setStatus(GlusterStatus.UP);
        } else {
            volume.setStatus(GlusterStatus.DOWN);
        }

        try {
            volume.setBricks(getBricks(volume.getId(), (Object[])map.get(BRICKS)));
        } catch (Exception e) {
            log.errorFormat("Could not populate bricks of volume {0} on cluster {1}.", volume.getName(), clusterId, e);
        }
        volume.setOptions(getOptions((Map<String, Object>)map.get(OPTIONS)));

        return volume;
    }

    private Map<String, String> getOptions(Map<String, Object> map) {
        Map<String, String> options = new HashMap<String, String>();
        for(Entry<String, Object> entry : map.entrySet()) {
            options.put(entry.getKey(), (String)entry.getValue());
        }
        return options;
    }

    /**
     * Gets list of bricks of the volume from given list of brick representations. This can return null in certain cases
     * of failure e.g. if the brick representation contains an ip address which is mapped to more than servers in the
     * database.
     *
     * @param volumeId
     * @param brickList
     * @return
     * @throws Exception
     */
    private List<GlusterBrickEntity> getBricks(Guid volumeId, Object[] brickList) throws Exception {
        List<GlusterBrickEntity> bricks = new ArrayList<GlusterBrickEntity>();

        int brickOrder = 0;

        try {
            for (Object brick : brickList) {
                bricks.add(getBrick(clusterId, volumeId, (String) brick, brickOrder++));
            }
        } catch (Exception e) {
            // We do not want the command to fail if bricks for one of the volumes could not be fetched. Hence log the
            // exception and return null. The client should have special handling if bricks list of any of the volumes
            // is null.
            log.errorFormat("Error while populating bricks of volume {0}.", volumeId, e);
            return null;
        }

        return bricks;
    }

    /**
     * Returns a brick object for given cluster and brick representation of the form hostnameOrIp:brickDir
     * @param clusterId ID of the Cluster to which the brick belongs
     * @param volumeId ID of the Volume to which the brick belongs
     * @param brickInfo brick representation of the form hostnameOrIp:brickDir
     * @param brickOrder Order number of the brick
     * @return The brick object if representation passed is valid
     */
    private GlusterBrickEntity getBrick(Guid clusterId, Guid volumeId, String brickInfo, int brickOrder) {
        String[] brickParts = brickInfo.split(":", -1);
        if(brickParts.length != 2) {
            throw new RuntimeException("Invalid brick representation [" + brickInfo + "]");
        }

        String hostnameOrIp = brickParts[0];
        String brickDir = brickParts[1];

        GlusterBrickEntity brick = new GlusterBrickEntity();
        brick.setVolumeId(volumeId);
        brick.setBrickOrder(brickOrder);
        brick.setBrickDirectory(brickDir);

        VDS server = getServer(clusterId, hostnameOrIp);
        if(server == null) {
            log.warnFormat("Could not find server {0} in cluster {1}", hostnameOrIp, clusterId);
        } else {
            brick.setServerId(server.getId());
            brick.setServerName(server.gethost_name());
        }
        return brick;
    }

    private VdsDAO getVdsDao() {
        return DbFacade.getInstance().getVdsDao();
    }

    private InterfaceDao getInterfaceDao() {
        return DbFacade.getInstance().getInterfaceDao();
    }

    /**
     * Returns a server from the given cluster, having give host name or IP address
     * @param clusterId
     * @param hostnameOrIp
     * @return
     */
    private VDS getServer(Guid clusterId, String hostnameOrIp) {
        List<VDS> servers = getVdsDao().getAllForHostname(hostnameOrIp);
        if(servers.size() > 0) {
            return getServerOfCluster(clusterId, servers);
        }

        List<VdsNetworkInterface> ifaces = getInterfaceDao().getAllInterfacesWithIpAddress(clusterId, hostnameOrIp);
        if(ifaces.size() == 1) {
            for(VdsNetworkInterface iface : ifaces) {
                VDS server = getVdsDao().get(iface.getVdsId());
                if(server.getvds_group_id().equals(clusterId)) {
                    return server;
                }
            }
        } else if(ifaces.size() > 1) {
            // There are multiple servers in the DB having this ip address. Throw an exception so that the gluster
            // manager doesn't try to update/add bricks belonging to such servers
            throw new RuntimeException(String.format("There are multiple servers in DB having same IP address %1$s! " +
                    "Cannot arrive at correct server id for bricks related to this ip address in cluster %2$s",
                    hostnameOrIp,
                    clusterId));
        }

        return null;
    }

    private VDS getServerOfCluster(Guid clusterId, List<VDS> servers) {
        for(VDS server : servers) {
            if(server.getvds_group_id().equals(clusterId)) {
                return server;
            }
        }
        return null;
    }

    public Map<Guid, GlusterVolumeEntity> getVolumes() {
        return volumes;
    }
}
