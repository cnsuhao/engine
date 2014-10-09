package org.ovirt.engine.core.dao;

import java.io.Serializable;
import java.util.Collection;

import org.ovirt.engine.core.common.businessentities.BusinessEntity;

/**
 * Implementation for the {@link MassOperationsDao} which provides a default
 * {@link MassOperationsDao#updateAll(Collection)} method that uses the {@link DefaultGenericDaoDbFacade#update} method
 * in a more smart way, reusing the {@link org.springframework.jdbc.core.simple.SimpleJdbcCallOperations}.
 *
 * @param <T>
 *            The type of entity.
 * @param <ID>
 *            The type of the entity's id.
 */
public abstract class MassOperationsGenericDaoDbFacade<T extends BusinessEntity<ID>, ID extends Serializable>
        extends DefaultGenericDaoDbFacade<T, ID> implements MassOperationsDao<T, ID> {

    public MassOperationsGenericDaoDbFacade(String entityStoredProcedureName) {
        super(entityStoredProcedureName);
    }

    @Override
    public void updateAll(Collection<T> entities) {
        updateAll(getProcedureNameForUpdate(),entities);
    }

    @Override
    /**
     * Enables to send update procedure name as a parameter that overrides the default
     * one.
     * In case this parameter is null the default procedure is used.
     */
    public void updateAll(String procedureName, Collection<T> entities) {
        for (T entity : entities) {
            update(entity, procedureName == null ? getProcedureNameForUpdate() : procedureName);
        }
    }

    @Override
    public void removeAll(Collection<ID> ids) {
        for (ID id : ids) {
            remove(id);
        }
    }
}
