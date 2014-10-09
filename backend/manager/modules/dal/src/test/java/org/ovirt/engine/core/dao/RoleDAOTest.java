package org.ovirt.engine.core.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.Role;
import org.ovirt.engine.core.common.businessentities.RoleType;
import org.ovirt.engine.core.compat.Guid;

public class RoleDAOTest extends BaseDAOTestCase {
    private static final String GROUP_IDS = "26df4393-659b-4b8a-b0f6-3ee94d32e82f,08963ba9-b1c8-498d-989f-75cf8142eab7";
    private static final Guid USER_ID = new Guid("9bf7c640-b620-456f-a550-0348f366544b");
    private static final int ROLE_COUNT = 4;

    private RoleDAO dao;
    private Role existingRole;
    private Role newRole;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        dao = prepareDAO(dbFacade.getRoleDao());

        existingRole = dao.get(new Guid("f5972bfa-7102-4d33-ad22-9dd421bfba78"));

        newRole = new Role();
        newRole.setname("new role");
        newRole.setdescription("This is a new role.");
        newRole.setType(RoleType.USER);
        newRole.setAllowsViewingChildren(false);
    }

    /**
     * Ensures that the id must be valid.
     */
    @Test
    public void testGetRoleWithInvalidId() {
        Role result = dao.get(Guid.NewGuid());

        assertNull(result);
    }

    /**
     * Ensures that retrieving a role works as expected.
     */
    @Test
    public void testGetRole() {
        Role result = dao.get(existingRole.getId());

        assertNotNull(result);
        assertEquals(existingRole, result);
    }

    /**
     * Ensures that an invalid name results in a null role.
     */
    @Test
    public void testGetRoleByNameWithInvalidName() {
        Role result = dao.getByName("Farkle");

        assertNull(result);
    }

    /**
     * Ensures that retrieving a role by name works as expected.
     */
    @Test
    public void testGetRoleByName() {
        Role result = dao.getByName(existingRole.getname());

        assertNotNull(result);
        assertEquals(existingRole, result);
    }

    /**
     * Ensures the right number of roles are returned.
     */
    @Test
    public void testGetAllRoles() {
        List<Role> result = dao.getAll();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(ROLE_COUNT, result.size());
    }

    /**
     * Ensures an empty collection is returned when no roles exist for the specified ad element.
     */
    @Test
    public void testGetAllRolesForAdElementWithInvalidId() {
        List<Role> result = dao.getAllForAdElement(Guid.NewGuid());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Ensures that a collection of roles are returned.
     */
    @Test
    public void testGetAllRolesForAdElement() {
        List<Role> result = dao.getAllForAdElement(USER_ID);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    /**
     * Ensures an empty collection is returned if the ad element is invalid.
     */
    @Test
    public void testGetAllRolesForUserAndGroupByAdElementWithInvalidAdElement() {
        List<Role> result = dao.getAllForAdElement(Guid.NewGuid());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Ensures the right collection of roles is returned.
     */
    @Test
    public void testGetAllRolesForUserAndGroupByAdElement() {
        List<Role> result = dao.getAllForAdElement(USER_ID);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    /**
     * Ensures the right collection of roles is returned
     */
    @Test
    public void testGetAllForUsersAndGroups() {
        List<Role> result = dao.getAllForUserAndGroups(USER_ID,
                GROUP_IDS);
        assertNotNull(result);
        assertFalse(result.isEmpty());

    }

    @Test
    public void testGetAllForUsersAndGroupsInvalidUserAndGroups() {
        List<Role> result = dao.getAllForUserAndGroups(Guid.NewGuid(),
                Guid.NewGuid().toString());
        assertNotNull(result);
        assertTrue(result.isEmpty());

    }


    /**
     * Ensures that saving a role works as expected.
     */
    @Test
    public void testSaveRole() {
        dao.save(newRole);

        Role result = dao.getByName(newRole.getname());

        assertNotNull(result);
        assertEquals(newRole, result);
    }

    /**
     * Ensures that updating a role works as expected.
     */
    @Test
    public void testUpdateRole() {
        existingRole.setdescription("This is an updated description");

        dao.update(existingRole);

        Role result = dao.get(existingRole.getId());

        assertNotNull(result);
        assertEquals(existingRole, result);
    }

    /**
     * Asserts removing a role works as expected
     */
    @Test
    public void testRemoveRole() {
        dao.remove(existingRole.getId());

        Role result = dao.get(existingRole.getId());

        assertNull(result);
    }
}
