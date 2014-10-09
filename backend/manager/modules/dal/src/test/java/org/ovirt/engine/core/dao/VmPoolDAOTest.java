package org.ovirt.engine.core.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmPoolMap;
import org.ovirt.engine.core.common.businessentities.vm_pools;
import org.ovirt.engine.core.compat.Guid;

public class VmPoolDAOTest extends BaseDAOTestCase {
    private static final Guid USER_ID = new Guid("9bf7c640-b620-456f-a550-0348f366544b");
    private static final Guid VDS_GROUP_ID = new Guid("b399944a-81ab-4ec5-8266-e19ba7c3c9d1");
    private static final Guid DELETABLE_VM_POOL_ID = new Guid("103cfd1d-18b1-4790-8a0c-1e52621b0078");
    private static final Guid EXISTING_VM_POOL_ID = new Guid("103cfd1d-18b1-4790-8a0c-1e52621b0076");
    private static final Guid FREE_VM_ID = new Guid("77296e00-0cad-4e5a-9299-008a7b6f4356");
    private static final Guid EXISTING_VM_ID = new Guid("77296e00-0cad-4e5a-9299-008a7b6f4355");
    private static final int VM_POOL_COUNT = 3;
    private VmPoolDAO dao;
    private vm_pools existingVmPool;
    private vm_pools deletableVmPool;
    private vm_pools newVmPool;
    private VmPoolMap newVmPoolMap;
    private VmPoolMap existingVmPoolMap;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        dao = prepareDAO(dbFacade.getVmPoolDao());

        existingVmPool = dao.get(EXISTING_VM_POOL_ID);
        deletableVmPool = dao.get(DELETABLE_VM_POOL_ID);

        newVmPool = new vm_pools();
        newVmPool.setvm_pool_name("New VM Pool");
        newVmPool.setvm_pool_description("This is a new VM pool.");
        newVmPool.setvds_group_id(VDS_GROUP_ID);

        existingVmPoolMap = dao.getVmPoolMapByVmGuid(new Guid("77296e00-0cad-4e5a-9299-008a7b6f4355"));
        newVmPoolMap =
                new VmPoolMap(FREE_VM_ID, existingVmPool.getvm_pool_id());
    }

    @Test
    public void testRemoveVmFromPool() {
        int before = dao.getVmPoolsMapByVmPoolId(existingVmPoolMap.getvm_pool_id()).size();

        dao.removeVmFromVmPool(EXISTING_VM_ID);

        int after = dao.getVmPoolsMapByVmPoolId(existingVmPoolMap.getvm_pool_id()).size();

        assertEquals(before - 1, after);

        VmPoolMap result = dao.getVmPoolMapByVmGuid(EXISTING_VM_ID);

        assertNull(result);
    }

    /**
     * Ensures that null is returned when the id is invalid.
     */
    @Test
    public void testGetVmPoolWithInvalidId() {
        vm_pools result = dao.get(Guid.NewGuid());

        assertNull(result);
    }

    /**
     * Ensures that getting a VM pool works as expected.
     */
    @Test
    public void testGetVmPool() {
        vm_pools result = dao.get(existingVmPool.getvm_pool_id());

        assertGetResult(result);
    }

    @Test
    public void testGetFilteredWithPermissions() {
        vm_pools result = dao.get(existingVmPool.getvm_pool_id(), PRIVILEGED_USER_ID, true);

        assertGetResult(result);
    }

    @Test
    public void testGetFilteredWithPermissionsNoPermissions() {
        vm_pools result = dao.get(existingVmPool.getvm_pool_id(), UNPRIVILEGED_USER_ID, true);

        assertNull(result);
    }

    @Test
    public void testGetFilteredWithPermissionsNoPermissionsAndNoFilter() {
        vm_pools result = dao.get(existingVmPool.getvm_pool_id(), UNPRIVILEGED_USER_ID, false);

        assertGetResult(result);
    }

    private void assertGetResult(vm_pools result) {
        assertNotNull(result);
        assertEquals(existingVmPool, result);
    }

    /**
     * Ensures that getting a VM pool by an invalid name returns null.
     */
    @Test
    public void testGetByNameWithInvalidName() {
        vm_pools result = dao.getByName("farkle");

        assertNull(result);
    }

    /**
     * Ensures that getting a VM pool by name works as expected.
     */
    @Test
    public void testGetByName() {
        vm_pools result = dao.getByName(existingVmPool.getvm_pool_name());

        assertNotNull(result);
        assertEquals(existingVmPool, result);
    }

    /**
     * Ensures the right number of pools are returned.
     */
    @Test
    public void testGetAllVmPools() {
        List<vm_pools> result = dao.getAll();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(VM_POOL_COUNT, result.size());
    }

    /**
     * Ensures an empty collection is returned.
     */
    @Test
    public void testGetAllVmPoolsForUserWithNoVmPools() {
        List<vm_pools> result = dao.getAllForUser(Guid.NewGuid());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Ensures a collection of pools are returned.
     */
    @Test
    public void testGetAllVmPoolsForUser() {
        List<vm_pools> result = dao.getAllForUser(USER_ID);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    /**
     * Ensures that saving a VM pool works as expected.
     */
    @Test
    public void testSaveVmPool() {
        dao.save(newVmPool);

        vm_pools result = dao.getByName(newVmPool.getvm_pool_name());

        assertNotNull(result);
        assertEquals(newVmPool, result);
    }

    /**
     * Ensures that updating a VM pool works as expected.
     */
    @Test
    public void testUpdateVmPool() {
        existingVmPool.setvm_pool_description("This is an updated VM pool.");

        dao.update(existingVmPool);

        vm_pools result = dao.get(existingVmPool.getvm_pool_id());

        assertEquals(existingVmPool, result);
    }

    /**
     * Ensures removing a VM pool works as expected.
     */
    @Test
    public void testRemoveVmPool() {
        dao.remove(deletableVmPool.getvm_pool_id());

        vm_pools result = dao.get(deletableVmPool.getvm_pool_id());

        assertNull(result);
    }

    @Test
    public void testGetVmPoolMap() {
        VmPoolMap result = dao.getVmPoolMapByVmGuid(EXISTING_VM_ID);

        assertNotNull(result);
        assertEquals(existingVmPoolMap, result);
    }

    @Test
    public void testAddVmToPool() {
        int before = dao.getVmPoolsMapByVmPoolId(newVmPoolMap.getvm_pool_id()).size();

        dao.addVmToPool(newVmPoolMap);

        int after = dao.getVmPoolsMapByVmPoolId(newVmPoolMap.getvm_pool_id()).size();

        assertEquals(before + 1, after);

        VmPoolMap result = dao.getVmPoolMapByVmGuid(newVmPoolMap.getvm_guid());

        assertNotNull(result);
        assertEquals(newVmPoolMap, result);
    }

    @Test
    public void testGetVmMapsInVmPoolByVmPoolIdAndStatus() {
        List<VmPoolMap> result = dao.getVmMapsInVmPoolByVmPoolIdAndStatus(
                existingVmPool.getvm_pool_id(), VMStatus.MigratingFrom);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    /**
     * Ensures that a VM from a vm pool is returned for a privileged user with filtering enabled.
     */
    @Test
    public void getVmDataFromPoolByPoolGuidWithPermissionsForPriviligedUser() {
        VM result = dao.getVmDataFromPoolByPoolGuid(EXISTING_VM_POOL_ID, PRIVILEGED_USER_ID, true);
        assertCorrectGetVmDataResult(result);
    }

    /**
     * Ensures a VM from a vm pool by is returned for a non privileged user with filtering disabled.
     */
    @Test
    public void getVmDataFromPoolByPoolGuidWithoutPermissionsForNonPriviligedUser() {
        VM result = dao.getVmDataFromPoolByPoolGuid(EXISTING_VM_POOL_ID, UNPRIVILEGED_USER_ID, false);
        assertCorrectGetVmDataResult(result);
    }

    /**
     * Ensures that no VM is returned for a non privileged user with filtering enabled.
     */
    @Test
    public void getVmDataFromPoolByPoolGuidWithPermissionsForNonPriviligedUser() {
        VM result = dao.getVmDataFromPoolByPoolGuid(EXISTING_VM_POOL_ID, UNPRIVILEGED_USER_ID, true);
        assertNull(result);
    }

    private void assertCorrectGetVmDataResult(VM result) {
        assertNotNull(result);
        assertEquals(result.getVmPoolId(), EXISTING_VM_POOL_ID);
    }
}
