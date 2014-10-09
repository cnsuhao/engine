package org.ovirt.engine.core.bll.storage;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.VdcBllMessages;

public class StoragePoolValidatorTest {

    private StoragePoolValidator validator = null;
    private storage_pool storagePool = null;

    @Before
    public void setup() {
        storagePool =
                new storage_pool("test",
                        Guid.NewGuid(),
                        "test",
                        StorageType.UNKNOWN.getValue(),
                        StoragePoolStatus.Up.getValue());
        validator = spy(new StoragePoolValidator(storagePool, new ArrayList<String>()));
        mockPosixStorageEnabledConfigValue();
    }

    protected void mockPosixStorageEnabledConfigValue() {
        doReturn(true).when(validator).getConfigValue(ConfigValues.PosixStorageEnabled, Version.v3_1.toString());
        doReturn(false).when(validator).getConfigValue(ConfigValues.PosixStorageEnabled, Version.v3_0.toString());
        doReturn(false).when(validator).getConfigValue(ConfigValues.PosixStorageEnabled, Version.v2_2.toString());
        doReturn(false).when(validator).getConfigValue(ConfigValues.PosixStorageEnabled, "general");
    }

    @Test
    public void testPosixDcAndMatchingCompatiblityVersion() {
        storagePool.setcompatibility_version(Version.v3_1);
        storagePool.setstorage_pool_type(StorageType.POSIXFS);
        assertTrue(validator.isPosixDcAndMatchingCompatiblityVersion());
    }

    @Test
    public void testPosixDcAndNotMatchingCompatiblityVersion() {
        storagePool.setcompatibility_version(Version.v3_0);
        storagePool.setstorage_pool_type(StorageType.POSIXFS);
        assertFalse(validator.isPosixDcAndMatchingCompatiblityVersion());
        assertMessages(VdcBllMessages.DATA_CENTER_POSIX_STORAGE_NOT_SUPPORTED_IN_CURRENT_VERSION);
    }

    @Test
    public void testLocalDcAndMatchingCompatiblityVersion() {
        storagePool.setcompatibility_version(Version.v3_0);
        storagePool.setstorage_pool_type(StorageType.LOCALFS);
        assertTrue(validator.isPosixDcAndMatchingCompatiblityVersion());
    }

    @Test
    public void testIsNotLocalFsWithDefaultCluster() {
        storagePool.setstorage_pool_type(StorageType.LOCALFS);
        doReturn(false).when(validator).containsDefaultCluster();
        assertTrue(validator.isNotLocalfsWithDefaultCluster());
    }

    @Test
    public void testIsNotLocalFsWithDefaultClusterWhenClusterIsDefault() {
        storagePool.setstorage_pool_type(StorageType.LOCALFS);
        doReturn(true).when(validator).containsDefaultCluster();
        assertFalse(validator.isNotLocalfsWithDefaultCluster());
        assertMessages(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_POOL_WITH_DEFAULT_VDS_GROUP_CANNOT_BE_LOCALFS);
    }

    protected void assertMessages(VdcBllMessages bllMsg) {
        assertTrue("Wrong number of canDoActionMessages is returned", validator.getCanDoActionMessages().size() == 1);
        assertTrue("Wrong canDoAction message is returned", validator.getCanDoActionMessages()
                .contains(bllMsg.toString()));
    }

}
