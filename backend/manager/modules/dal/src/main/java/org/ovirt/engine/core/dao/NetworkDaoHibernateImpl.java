package org.ovirt.engine.core.dao;

import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.hibernate.criterion.Restrictions;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.network.NetworkDao;

/**
 * <code>NetworkDaoHibernateImpl</code> provides an implementation of {@Link NetworkDAO} using Hibernate.
 *
 */
public class NetworkDaoHibernateImpl extends BaseDAOHibernateImpl<Network, Guid> implements NetworkDao {
    public NetworkDaoHibernateImpl() {
        super(Network.class);
    }

    @Override
    public List<Network> getAllForDataCenter(Guid id) {
        return findByCriteria(Restrictions.eq("storage_pool_id", id));
    }

    @Override
    public List<Network> getAllForCluster(Guid id) {
        return findByCriteria(Restrictions.eq("cluster.clusterId", id));
    }

    @Override
    public List<Network> getAllForCluster(Guid id, Guid userID, boolean isFiltered) {
        throw new NotImplementedException();
    }

    @Override
    public Network getByNameAndDataCenter(String name, Guid storagePoolId) {
        throw new NotImplementedException();
    }

    @Override
    public Network getByNameAndCluster(String name, Guid clusterId) {
        throw new NotImplementedException();
    }

    @Override
    public List<Network> getAll(Guid userID, boolean isFiltered) {
        throw new NotImplementedException();
    }

    @Override
    public List<Network> getAllForDataCenter(Guid id, Guid userID, boolean isFiltered) {
        throw new NotImplementedException();
    }
}
