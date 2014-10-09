package org.ovirt.engine.core.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;

public class VdsGroupDAOTest extends BaseDAOTestCase {
    private static final int NUMBER_OF_GROUPS = 8;
    private static final int NUMBER_OF_GROUPS_FOR_PRIVELEGED_USER = 1;

    private VdsGroupDAO dao;
    private VDS existingVds;
    private VDSGroup existingVdsGroup;
    private VDSGroup newGroup;
    private VDSGroup groupWithNoRunningVms;
    private storage_pool storagePool;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        VdsDAO vdsDAO = prepareDAO(dbFacade.getVdsDao());

        existingVds = vdsDAO.get(FixturesTool.VDS_RHEL6_NFS_SPM);

        StoragePoolDAO storagePoolDAO = prepareDAO(dbFacade.getStoragePoolDao());

        storagePool = storagePoolDAO.get(FixturesTool.STORAGE_POOL_RHEL6_ISCSI_OTHER);

        dao = prepareDAO(dbFacade.getVdsGroupDao());

        existingVdsGroup = dao.get(existingVds.getvds_group_id());
        groupWithNoRunningVms = dbFacade.getVdsGroupDao().get(FixturesTool.VDS_GROUP_NO_RUNNING_VMS);

        newGroup = new VDSGroup();
        newGroup.setname("New VDS Group");
        newGroup.setcompatibility_version(new Version("3.0"));
        newGroup.setVirtService(true);
        newGroup.setGlusterService(false);
    }

    /**
     * Ensures that the id must be valid.
     */
    @Test
    public void testGetWithInvalidId() {
        VDSGroup result = dao.get(Guid.NewGuid());

        assertNull(result);
    }

    /**
     * Ensures that null is returned.
     */
    @Test
    public void testGetWithRunningVmsWhereThereAreNone() {
        VDSGroup result = dao.getWithRunningVms(groupWithNoRunningVms.getId());

        assertNull(result);
    }

    /**
     * Ensures that the VDS group is returned.
     */
    @Test
    public void testGetWithRunningVms() {
        VDSGroup result = dao.getWithRunningVms(existingVdsGroup.getId());

        assertNotNull(result);
    }

    /**
     * Ensures that retrieving a group works as expected.
     */
    @Test
    public void testGet() {
        VDSGroup result = dao.get(existingVdsGroup.getId());

        assertCorrectVDSGroup(result);
    }

    /**
     * Ensures that retrieving a group works as expected with a privileged user and optional filtering.
     */
    @Test
    public void testGetFilteredWithPermissions() {
        VDSGroup result = dao.get(existingVdsGroup.getId(), PRIVILEGED_USER_ID, true);

        assertCorrectVDSGroup(result);
    }

    /**
     * Ensures that retrieving a group works as expected with an unprivileged user and optional filtering disabled.
     */
    @Test
    public void testGetFilteredWithNoPermissionsAndNoFilter() {
        VDSGroup result = dao.get(existingVdsGroup.getId(), UNPRIVILEGED_USER_ID, false);

        assertCorrectVDSGroup(result);
    }

    /**
     * Ensures that retrieving a group works as expected with an unprivileged user.
     */
    @Test
    public void testGetFilteredWithNoPermissions() {
        VDSGroup result = dao.get(existingVdsGroup.getId(), UNPRIVILEGED_USER_ID, true);

        assertNull(result);
    }

    /**
     * Asserts that the given {@link VDSGroup} is indeed the existing VDS Group the test uses.
     *
     * @param group
     *            The group to check
     */
    private void assertCorrectVDSGroup(VDSGroup group) {
        assertNotNull(group);
        assertEquals(existingVdsGroup, group);
    }

    /**
     * Ensures that a bad name result in a null group.
     */
    @Test
    public void testGetByNameWithBadName() {
        VDSGroup result = dao.getByName("farkle");

        assertNull(result);
    }

    /**
     * Ensures that the right group is returned.
     */
    @Test
    public void testGetByName() {
        VDSGroup result = dao.getByName(existingVdsGroup.getname());

        assertCorrectVDSGroup(result);
    }

    @Test
    public void testGetByNameForPrivilegedUser() {
        VDSGroup result = dao.getByName(existingVdsGroup.getname(), PRIVILEGED_USER_ID, false);
        assertCorrectVDSGroup(result);
    }

    @Test
    public void testGetByNameForUnprivilegedUser() {
        VDSGroup result = dao.getByName(existingVdsGroup.getname(), UNPRIVILEGED_USER_ID, true);
        assertNull(result);
    }

    /**
     * Ensures that an empty collection is returned.
     */
    @Test
    public void testGetAllForStoragePoolWithInvalidPool() {
        List<VDSGroup> result = dao.getAllForStoragePool(Guid.NewGuid());
        assertGetAllForStoragePoolInvalidResult(result);
    }

    /**
     * Ensures that the right group is returned.
     */
    @Test
    public void testGetAllForStoragePool() {
        List<VDSGroup> result = dao.getAllForStoragePool(storagePool.getId());
        assertGetAllForStoragePoolValidResult(result);
    }

    /**
     * Ensures that no groups are returned if the issuing user does not have the right permissions.
     */
    @Test
    public void testGetAllForStoragePoolFilteredWithNoPermissions() {
        List<VDSGroup> result = dao.getAllForStoragePool(storagePool.getId(), UNPRIVILEGED_USER_ID, true);
        assertGetAllForStoragePoolInvalidResult(result);
    }

    /**
     * Ensures that the right group is returned if the filtering mechanism is disabled.
     */
    @Test
    public void testGetAllForStoragePoolFilteredWithNoPermissionsAndNoFilter() {
        List<VDSGroup> result = dao.getAllForStoragePool(storagePool.getId(), UNPRIVILEGED_USER_ID, false);
        assertGetAllForStoragePoolValidResult(result);
    }

    /**
     * Ensures that no groups are returned if the issuing user has the right permissions.
     */
    @Test
    public void testGetAllForStoragePoolFilteredWithPermissions() {
        List<VDSGroup> result = dao.getAllForStoragePool(storagePool.getId(), PRIVILEGED_USER_ID, true);
        assertGetAllForStoragePoolValidResult(result);
    }

    /**
     * Asserts the result of a invalid call to {@link VdsGroupDAO#getAllForStoragePool(Guid, Guid, boolean)}
     *
     * @param result
     */
    private static void assertGetAllForStoragePoolInvalidResult(List<VDSGroup> result) {
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Asserts the result of a valid call to {@link VdsGroupDAO#getAllForStoragePool(Guid, Guid, boolean)}
     *
     * @param result
     */
    private void assertGetAllForStoragePoolValidResult(List<VDSGroup> result) {
        assertNotNull(result);
        assertFalse(result.isEmpty());
        for (VDSGroup group : result) {
            assertEquals(storagePool.getId(), group.getStoragePoolId());
        }
    }

    /**
     * Ensures that the right number of groups are returned.
     */
    @Test
    public void testGetAll() {
        List<VDSGroup> result = dao.getAll();

        assertCorrectGetAllResult(result);
    }

    /**
     * Ensures that retrieving VDS groups works as expected for a privileged user.
     */
    @Test
    public void testGetAllWithPermissionsPrivilegedUser() {
        List<VDSGroup> result = dao.getAll(PRIVILEGED_USER_ID, true);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(NUMBER_OF_GROUPS_FOR_PRIVELEGED_USER, result.size());
        assertEquals(result.iterator().next(), existingVdsGroup);
    }

    /**
     * Ensures that retrieving VDS groups works as expected with filtering disabled for an unprivileged user.
     */
    @Test
    public void testGetAllWithPermissionsDisabledUnprivilegedUser() {
        List<VDSGroup> result = dao.getAll(UNPRIVILEGED_USER_ID, false);

        assertCorrectGetAllResult(result);
    }

    /**
     * Ensures that no VDS group is retrieved for an unprivileged user with filtering enabled.
     */
    @Test
    public void testGetWithPermissionsUnprivilegedUser() {
        List<VDSGroup> result = dao.getAll(UNPRIVILEGED_USER_ID, true);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Ensures saving a group works as expected.
     */
    @Test
    public void testSave() {
        dao.save(newGroup);

        VDSGroup result = dao.getByName(newGroup.getname());

        assertNotNull(result);
        assertEquals(newGroup, result);
    }

    /**
     * Ensures that updating a group works as expected.
     */
    @Test
    public void testUpdate() {
        String oldName = existingVdsGroup.getname();

        existingVdsGroup.setname("This is the new name");
        existingVdsGroup.setVirtService(false);
        existingVdsGroup.setGlusterService(true);

        dao.update(existingVdsGroup);

        VDSGroup result = dao.get(existingVdsGroup.getId());

        assertCorrectVDSGroup(result);

        result = dao.getByName(oldName);

        assertNull(result);
    }

    /**
     * Ensures that removing a group works as expected.
     */
    @Test
    public void testRemove() {
        dao.remove(groupWithNoRunningVms.getId());

        VDSGroup result = dao.get(groupWithNoRunningVms.getId());

        assertNull(result);
    }

    /**
     * Asserts the result from {@link VdsGroupDAO#getAll()} is correct without filtering
     *
     * @param result
     */
    private void assertCorrectGetAllResult(List<VDSGroup> result) {
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(NUMBER_OF_GROUPS, result.size());
    }
}
