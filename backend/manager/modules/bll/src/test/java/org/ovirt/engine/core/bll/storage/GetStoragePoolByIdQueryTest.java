package org.ovirt.engine.core.bll.storage;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.ovirt.engine.core.bll.AbstractUserQueryTest;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.queries.StoragePoolQueryParametersBase;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.StoragePoolDAO;

/**
 * A test case for {@link GetStoragePoolByIdQuery}.
 * It does not test database implementation, but rather tests that the right delegations to the DAO occur.
 */
public class GetStoragePoolByIdQueryTest extends AbstractUserQueryTest<StoragePoolQueryParametersBase, GetStoragePoolByIdQuery<StoragePoolQueryParametersBase>> {

    @Test
    public void testExecuteQuery() {
        Guid storagePoolID = Guid.NewGuid();
        storage_pool expectedResult = mock(storage_pool.class);

        StoragePoolQueryParametersBase paramsMock = getQueryParameters();
        when(paramsMock.getStoragePoolId()).thenReturn(storagePoolID);

        StoragePoolDAO storagePoolDAOMock = mock(StoragePoolDAO.class);
        when(storagePoolDAOMock.get(storagePoolID, getUser().getUserId(), paramsMock.isFiltered())).thenReturn(expectedResult);

        DbFacade dbFacadeMock = getDbFacadeMockInstance();
        when(dbFacadeMock.getStoragePoolDao()).thenReturn(storagePoolDAOMock);

        getQuery().executeQueryCommand();

        storage_pool result = (storage_pool) getQuery().getQueryReturnValue().getReturnValue();

        assertEquals("Wrong storage pool returned", expectedResult, result);
    }
}
