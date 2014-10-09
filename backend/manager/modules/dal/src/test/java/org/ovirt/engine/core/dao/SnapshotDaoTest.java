package org.ovirt.engine.core.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.core.common.businessentities.Snapshot.SnapshotStatus;
import org.ovirt.engine.core.common.businessentities.Snapshot.SnapshotType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.RandomUtils;

/**
 * Unit tests to validate {@link BaseDiskDao}.
 */
public class SnapshotDaoTest extends BaseGenericDaoTestCase<Guid, Snapshot, SnapshotDao> {

    private static final Guid EXISTING_VM_ID = new Guid("77296e00-0cad-4e5a-9299-008a7b6f4355");
    private static final Guid EXISTING_SNAPSHOT_ID = new Guid("a7bb24df-9fdf-4bd6-b7a9-f5ce52da0f89");
    private static final int TOTAL_SNAPSHOTS = 2;

    @Override
    protected Guid generateNonExistingId() {
        return Guid.NewGuid();
    }

    @Override
    protected int getEneitiesTotalCount() {
        return TOTAL_SNAPSHOTS;
    }

    @Override
    protected Snapshot generateNewEntity() {
        return new Snapshot(Guid.NewGuid(),
                RandomUtils.instance().nextEnum(SnapshotStatus.class),
                EXISTING_VM_ID,
                RandomUtils.instance().nextString(200000),
                RandomUtils.instance().nextEnum(SnapshotType.class),
                RandomUtils.instance().nextString(1000),
                new Date(),
                RandomUtils.instance().nextString(200000));
    }

    @Override
    protected void updateExistingEntity() {
        existingEntity.setDescription(RandomUtils.instance().nextString(1000));
    }

    @Override
    protected SnapshotDao prepareDao() {
        return prepareDAO(dbFacade.getSnapshotDao());
    }

    @Override
    protected Guid getExistingEntityId() {
        return EXISTING_SNAPSHOT_ID;
    }

    @Test
    public void updateStatus() throws Exception {
        Snapshot snapshot = dao.get(getExistingEntityId());

        snapshot.setStatus(SnapshotStatus.LOCKED);
        dao.updateStatus(snapshot.getId(), snapshot.getStatus());

        assertEquals(snapshot, dao.get(snapshot.getId()));
    }

    @Test
    public void updateStatusForNonExistingSnapshot() throws Exception {
        Guid snapshotId = new Guid();

        assertNull(dao.get(snapshotId));
        dao.updateStatus(snapshotId, SnapshotStatus.LOCKED);

        assertNull(dao.get(snapshotId));
    }

    @Test
    public void updateId() throws Exception {
        Snapshot snapshot = dao.get(getExistingEntityId());

        assertNotNull(snapshot);
        Guid oldId = snapshot.getId();
        snapshot.setId(new Guid());

        dao.updateId(oldId, snapshot.getId());

        assertEquals(snapshot, dao.get(snapshot.getId()));
    }

    @Test
    public void updateIdForNonExistingSnapshot() throws Exception {
        Guid snapshotId = new Guid();
        Guid newSnapshotId = new Guid();

        assertNull(dao.get(snapshotId));
        dao.updateId(snapshotId, newSnapshotId);

        assertNull(dao.get(snapshotId));
        assertNull(dao.get(newSnapshotId));
    }

    @Test
    public void getSnaphsotByTypeReturnsIdForExistingByTypeAndStatus() throws Exception {
        assertNotNull(dao.get(EXISTING_VM_ID, SnapshotType.REGULAR));
    }

    @Test
    public void getIdByTypeReturnsIdForExistingByTypeAndStatus() throws Exception {
        assertEquals(getExistingEntityId(), dao.getId(EXISTING_VM_ID, SnapshotType.REGULAR));
    }

    @Test
    public void getIdByTypeReturnsNullForNonExistingVm() throws Exception {
        assertEquals(null, dao.getId(new Guid(), SnapshotType.REGULAR));
    }

    @Test
    public void getIdByTypeReturnsNullForNonExistingType() throws Exception {
        assertEquals(null, dao.getId(EXISTING_VM_ID, SnapshotType.PREVIEW));
    }

    @Test
    public void getIdByTypeAndStatusReturnsIdForExistingByTypeAndStatus() throws Exception {
        assertEquals(getExistingEntityId(), dao.getId(EXISTING_VM_ID, SnapshotType.REGULAR, SnapshotStatus.OK));
    }

    @Test
    public void getIdByTypeAndStatusReturnsNullForNonExistingVm() throws Exception {
        assertEquals(null, dao.getId(new Guid(), SnapshotType.REGULAR, SnapshotStatus.OK));
    }

    @Test
    public void getIdByTypeAndStatusReturnsNullForNonExistingType() throws Exception {
        assertEquals(null, dao.getId(EXISTING_VM_ID, SnapshotType.PREVIEW, SnapshotStatus.OK));
    }

    @Test
    public void getIdByTypeAndStatusReturnsNullForNonExistingStatus() throws Exception {
        assertEquals(null, dao.getId(EXISTING_VM_ID, SnapshotType.REGULAR, SnapshotStatus.IN_PREVIEW));
    }

    @Test
    public void getAllByVmWithConfiguration() {
        List<Snapshot> snapshots = dao.getAllWithConfiguration(FixturesTool.VM_RHEL5_POOL_50);
        assertEquals("VM should have a snapshot", 1, snapshots.size());
        for (Snapshot snapshot : snapshots) {
            assertEquals("Snapshot should have configuration", "test!", snapshot.getVmConfiguration());
            assertTrue("Snapshot should have configuration available", snapshot.isVmConfigurationAvailable());
        }
    }

    @Test
    public void getAllByVm() {
        List<Snapshot> snapshots = dao.getAll(FixturesTool.VM_RHEL5_POOL_57);
        assertFullGetAllByVmResult(snapshots);
    }

    @Test
    public void getAllByVmFilteredWithPermissions() {
        // test user 3 - has permissions
        List<Snapshot> snapshots = dao.getAll(FixturesTool.VM_RHEL5_POOL_57, PRIVILEGED_USER_ID, true);
        assertFullGetAllByVmResult(snapshots);
    }

    @Test
    public void getAllByVmFilteredWithPermissionsNoPermissions() {
        // test user 2 - hasn't got permissions
        List<Snapshot> snapshots = dao.getAll(FixturesTool.VM_RHEL5_POOL_57, UNPRIVILEGED_USER_ID, true);
        assertTrue("VM should have no snapshots viewable to the user", snapshots.isEmpty());
    }

    @Test
    public void getAllByVmFilteredWithPermissionsNoPermissionsAndNoFilter() {
        // test user 2 - hasn't got permissions, but no filtering was requested
        List<Snapshot> snapshots = dao.getAll(FixturesTool.VM_RHEL5_POOL_57, UNPRIVILEGED_USER_ID, false);
        assertFullGetAllByVmResult(snapshots);
    }

    @Test
    public void getFilteredWithPermissions() {
        Snapshot snapshot = dao.get(EXISTING_SNAPSHOT_ID, PRIVILEGED_USER_ID, true);
        assertNotNull(snapshot);
        assertEquals(existingEntity, snapshot);
    }

    @Test
    public void getFilteredWithPermissionsNoPermissions() {
        Snapshot snapshot = dao.get(EXISTING_SNAPSHOT_ID, UNPRIVILEGED_USER_ID, true);
        assertNull(snapshot);
    }

    @Test
    public void getFilteredWithPermissionsNoPermissionsAndNoFilter() {
        Snapshot snapshot = dao.get(EXISTING_SNAPSHOT_ID, UNPRIVILEGED_USER_ID, false);
        assertNotNull(snapshot);
        assertEquals(existingEntity, snapshot);
    }

    /**
     * Asserts the result of {@link SnapshotDao#getAll(Guid)} contains the correct snapshots.
     *
     * @param snapshots
     *            The result to check
     */
    private static void assertFullGetAllByVmResult(List<Snapshot> snapshots) {
        assertEquals("VM should have a snapshot", 1, snapshots.size());
        for (Snapshot snapshot : snapshots) {
            assertFalse("Snapshot shouldn't have configuration available", snapshot.isVmConfigurationAvailable());
            assertTrue("Snapshot should have no configuration", StringUtils.isEmpty(snapshot.getVmConfiguration()));
        }
    }

    @Test
    public void existsReturnsTrueForExistingByVmAndType() throws Exception {
        assertTrue(dao.exists(EXISTING_VM_ID, SnapshotType.REGULAR));
    }

    @Test
    public void existsWithTypeReturnsFalseForNonExistingVm() throws Exception {
        assertFalse(dao.exists(new Guid(), SnapshotType.REGULAR));
    }

    @Test
    public void existsWithTypeReturnsFalseForNonExistingStatus() throws Exception {
        assertFalse(dao.exists(EXISTING_VM_ID, SnapshotType.PREVIEW));
    }

    @Test
    public void existsReturnsTrueForExistingByVmAndStatus() throws Exception {
        assertTrue(dao.exists(EXISTING_VM_ID, SnapshotStatus.OK));
    }

    @Test
    public void existsWithStatusReturnsFalseForNonExistingVm() throws Exception {
        assertFalse(dao.exists(new Guid(), SnapshotStatus.OK));
    }

    @Test
    public void existsWithStatusReturnsFalseForNonExistingStatus() throws Exception {
        assertFalse(dao.exists(EXISTING_VM_ID, SnapshotStatus.LOCKED));
    }

    @Test
    public void existsReturnsTrueForExistingByVmAndSansphot() throws Exception {
        assertTrue(dao.exists(EXISTING_VM_ID, getExistingEntityId()));
    }

    @Test
    public void existsWithSnapshotReturnsFalseForNonExistingVm() throws Exception {
        assertFalse(dao.exists(new Guid(), getExistingEntityId()));
    }

    @Test
    public void existsWithSnapshotReturnsFalseForNonExistingSnapshot() throws Exception {
        assertFalse(dao.exists(EXISTING_VM_ID, new Guid()));
    }
}
