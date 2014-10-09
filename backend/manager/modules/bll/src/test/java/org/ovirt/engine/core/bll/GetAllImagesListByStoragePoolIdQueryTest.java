package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.ovirt.engine.core.common.businessentities.FileTypeExtension;
import org.ovirt.engine.core.common.businessentities.RepoFileMetaData;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.queries.GetAllImagesListByStoragePoolIdParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.StorageDomainDAO;
import org.ovirt.engine.core.dao.StoragePoolDAO;

@RunWith(Parameterized.class)
public class GetAllImagesListByStoragePoolIdQueryTest
        extends AbstractUserQueryTest<GetAllImagesListByStoragePoolIdParameters, AbstractGetAllImagesListByStoragePoolIdQuery<? extends GetAllImagesListByStoragePoolIdParameters>> {

    private Class<? extends AbstractGetAllImagesListByStoragePoolIdQuery<GetAllImagesListByStoragePoolIdParameters>> queryClass;
    private FileTypeExtension expectedType;
    private Guid storageDomainId;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]
        { { GetAllIsoImagesListByStoragePoolIdQuery.class, FileTypeExtension.ISO },
                { GetAllFloppyImagesListByStoragePoolIdQuery.class, FileTypeExtension.Floppy } });
    }

    public GetAllImagesListByStoragePoolIdQueryTest(Class<? extends AbstractGetAllImagesListByStoragePoolIdQuery<GetAllImagesListByStoragePoolIdParameters>> queryClass,
            FileTypeExtension expectedType) {
        this.queryClass = queryClass;
        this.expectedType = expectedType;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        storageDomainId = Guid.NewGuid();
    }

    @Override
    protected void setUpMockQueryParameters() {
        super.setUpMockQueryParameters();
        when(getQueryParameters().getStoragePoolId()).thenReturn(Guid.NewGuid());
    }

    @Override
    protected void setUpSpyQuery() throws Exception {
        super.setUpSpyQuery();
        RepoFileMetaData rfmd = new RepoFileMetaData();
        rfmd.setFileType(expectedType);
        doReturn(Collections.singletonList(rfmd)).when(getQuery()).getUserRequestForStorageDomainRepoFileList();
    }

    @Override
    protected Class<? extends AbstractGetAllImagesListByStoragePoolIdQuery<? extends GetAllImagesListByStoragePoolIdParameters>> getQueryType() {
        return queryClass;
    }

    @Test
    public void testFileTypeExtension() {
        assertEquals("Query fetch wrong type of extension", expectedType, getQuery().getFileTypeExtension());
    }

    @Test
    public void testGetStorageDomainIdWithPermissions() {
        mockStoragePoolDAO(new storage_pool());

        StorageDomainDAO storageDomainDAOMock = mock(StorageDomainDAO.class);
        when(storageDomainDAOMock.getIsoStorageDomainIdForPool(getQueryParameters().getStoragePoolId())).thenReturn(storageDomainId);
        when(getQuery().getDbFacade().getStorageDomainDao()).thenReturn(storageDomainDAOMock);

        assertEquals("wrong storage domain id", storageDomainId, getQuery().getStorageDomainId());
    }

    @Test
    public void testGetStorageDomainIdWithNoPermissions() {
        mockStoragePoolDAO(null);

        verifyNoMoreInteractions(getQuery().getDbFacade());
        assertNull("No storage domains should have been returned", getQuery().getStorageDomainId());
    }

    /**
     * Mocks the storage pool DAO to return the given storage pool
     * @param sp The storage_pool the DAO should return
     */
    private void mockStoragePoolDAO(storage_pool pool) {
        Guid storagePoolId = getQueryParameters().getStoragePoolId();
        if (pool != null) {
            pool.setId(storagePoolId);
        }

        StoragePoolDAO storagePoolDAOMock = mock(StoragePoolDAO.class);
        when(storagePoolDAOMock.get(
                storagePoolId,
                getUser().getUserId(),
                getQueryParameters().isFiltered())).thenReturn(pool);
        when(getQuery().getDbFacade().getStoragePoolDao()).thenReturn(storagePoolDAOMock);
    }
}
