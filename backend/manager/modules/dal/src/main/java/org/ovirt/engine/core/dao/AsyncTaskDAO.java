package org.ovirt.engine.core.dao;

import java.util.List;

import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.businessentities.AsyncTasks;
import org.ovirt.engine.core.compat.Guid;

/**
 * <code>AsyncTaskDAO</code> defines a type which performs CRUD operations on instances of {@link AsyncTasks}.
 *
 *
 */
public interface AsyncTaskDAO extends DAO {
    /**
     * Retrieves the task with the specified id.
     *
     * @param id the task id
     * @return the task
     */
    AsyncTasks get(Guid id);

    /**
     * Gets async task Ids by a given entity Id.
     * @param entityId the Id of the entity to return the tasks for
     * @return
     */
    List<Guid> getAsyncTaskIdsByEntity(Guid entityId);

    /**
     * Gets async task Ids of tasks that are running
     * on the given storage pool
     * @param storagePoolId ID of storage pool to return running tasks for
     * @return
     */
    List<Guid> getAsyncTaskIdsByStoragePoolId(Guid storagePoolId);


    /**
     * Retrieves all tasks.
     *
     * @return the list of tasks
     */
    List<AsyncTasks> getAll();

    /**
     * Saves the specified task.
     *
     * @param task the task
     * @param enitytType type of entities to be associated with the task
     * @param entityIds IDs of entities to be associated with the task
     */
    void save(AsyncTasks task, VdcObjectType enitytType, Guid... entityIds);

    /**
     * Saves or updates the specified task and its associated entities
     *
     * @param task the task
     * @param enitytType type of entities to be associated with the task
     * @param entityIds IDs of entities to be associated with the task
     */
    void saveOrUpdate(AsyncTasks task, VdcObjectType entityType, Guid... entityIds);

    /**
     * Updates the specified task.
     *
     * @param task the task
     */
    void update(AsyncTasks task);

    /**
     * Removes the task with the specified id.
     *
     * @param id
     */
    int remove(Guid id);

    /**
     * Saves the specified task.
     *
     * @param task the task to save
     */
    void save(AsyncTasks newAsyncTask);

    /**
     * Saves or updates the async task
     * @param asyncTask the task to update or save
     */
    void saveOrUpdate(AsyncTasks asyncTask);

}
