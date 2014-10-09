package org.ovirt.engine.core.bll.storage;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.bll.AbstractUserQueryTest;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.queries.StoragePoolQueryParametersBase;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.StorageDomainDAO;

/** A test case for the {@link GetStorageDomainsByStoragePoolIdQuery} class. */
public class GetStorageDomainsByStoragePoolIdQueryTest extends AbstractUserQueryTest<StoragePoolQueryParametersBase, GetStorageDomainsByStoragePoolIdQuery<StoragePoolQueryParametersBase>> {

    @Test
    public void testExecuteQuery() {
        Guid storagePoolID = Guid.NewGuid();
        when(getQueryParameters().getStoragePoolId()).thenReturn(storagePoolID);

        storage_domains domain = new storage_domains();

        StorageDomainDAO storageDomainDAOMock = mock(StorageDomainDAO.class);
        when(storageDomainDAOMock.getAllForStoragePool
                (storagePoolID,
                        getUser().getUserId(),
                        getQueryParameters().isFiltered())).
                thenReturn(Collections.singletonList(domain));

        DbFacade dbFacadeMock = getDbFacadeMockInstance();
        when(dbFacadeMock.getStorageDomainDao()).thenReturn(storageDomainDAOMock);

        getQuery().executeQueryCommand();

        @SuppressWarnings("unchecked")
        List<storage_domains> result = (List<storage_domains>) getQuery().getQueryReturnValue().getReturnValue();
        assertEquals("Wrong number of domains returned", 1, result.size());
        assertEquals("Wrong domain returned", domain, result.get(0));
    }
}
