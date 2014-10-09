package org.ovirt.engine.core.bll.utils;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.VdsDAO;
import org.ovirt.engine.core.utils.RandomUtils;

public class ClusterUtils {

    private static ClusterUtils instance = new ClusterUtils();

    public static ClusterUtils getInstance() {
        return instance;
    }

    /**
     * Returns a server that is in {@link VDSStatus#Up} status.<br>
     * This server is chosen randomly from all the Up servers.
     *
     * @param clusterId
     * @return One of the servers in up status
     */
    public VDS getUpServer(Guid clusterId) {
        List<VDS> servers = getVdsDao()
                .getAllForVdsGroupWithStatus(clusterId, VDSStatus.Up);

        if (servers == null || servers.isEmpty()) {
            return null;
        }
        return RandomUtils.instance().pickRandom(servers);
    }

    public boolean hasMultipleServers(Guid clusterId) {
        return getServerCount(clusterId) > 1;
    }

    public boolean hasServers(Guid clusterId) {
        return getServerCount(clusterId) > 0;
    }

    private int getServerCount(Guid clusterId) {
        return getVdsDao().getAllForVdsGroup(clusterId).size();
    }

    public VdsDAO getVdsDao() {
        return DbFacade.getInstance()
                .getVdsDao();
    }
}
