package org.ovirt.engine.core.bll.tasks;

import org.ovirt.engine.core.bll.SPMAsyncTask;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

/**
 * Helper class for async tasks handling
 */
public class AsyncTaskUtils {

    /**
     * Adds a task to DB or updates it if already
     * exists in DB
     * @param asyncTask task to be added or updated
     */
    public static void addOrUpdateTaskInDB(SPMAsyncTask asyncTask) {
        try {
            if (asyncTask.getParameters().getDbAsyncTask() != null) {
                DbFacade.getInstance().getAsyncTaskDao().saveOrUpdate(asyncTask.getParameters().getDbAsyncTask());
            }
        } catch (RuntimeException e) {
            log.error(String.format(
                    "Adding/Updating task %1$s to DataBase threw an exception.",
                    asyncTask.getTaskID()), e);
        }
    }

    /**
     * Saves async task in DB
     * @param asyncTask
     *            async task entity to be saved in DB
     */
    public static void saveAsyncTaskInDB(SPMAsyncTask asyncTask) {
        DbFacade.getInstance().getAsyncTaskDao().save(asyncTask.getParameters().getDbAsyncTask(),asyncTask.getEntityType(),asyncTask.getAssociatedEntities());

    }

    /**
     * Updates existing task in DB
     * @param asyncTask async task entity to be updated in DB
     */
    public static void updateAsyncTaskInDB(SPMAsyncTask asyncTask) {
        DbFacade.getInstance().getAsyncTaskDao().update(asyncTask.getParameters().getDbAsyncTask());

    }

    private static final Log log = LogFactory.getLog(AsyncTaskUtils.class);

}
