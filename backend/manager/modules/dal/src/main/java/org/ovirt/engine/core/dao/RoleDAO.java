package org.ovirt.engine.core.dao;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.Role;
import org.ovirt.engine.core.compat.Guid;

/**
 * <code>RoleDAO</code> defines a type for performing CRUD operations on instances of {@link Role}.
 *
 *
 */
public interface RoleDAO extends GenericDao<Role, Guid> {

    /**
     * Retrieves the role with the specified name.
     *
     * @param name
     *            the role name
     * @return the role
     */
    Role getByName(String name);

    /**
     * Retrieves all roles for the specified Ad element.
     *
     * @param id
     *            the Ad element
     * @return the list of roles
     */
    List<Role> getAllForAdElement(Guid id);

    /**
     * This seems to be a redundant method, but the stored procedure is different from the one for
     * {@link #getAllForAdElement(Guid)}.
     *
     * @param id
     *            the Ad element
     * @return the list of roles
     */
    List<Role> getForAdElement(Guid id);

    /**
     * This method gets the Roles for the given user and its groups. The purpose of this method is to be able to get
     * roles even if the user is not already added to DB, by getting the roles for his groups
     *
     * @param userId
     *            ID of the user to obtain roles for
     * @param groupIds
     *            comma delimited list of group IDs of the user
     * @return the list of the roles
     */
    List<Role> getAllForUserAndGroups(Guid userId, String groupIds);
}
