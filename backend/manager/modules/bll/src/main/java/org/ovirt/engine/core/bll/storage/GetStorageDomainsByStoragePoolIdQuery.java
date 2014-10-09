package org.ovirt.engine.core.bll.storage;

import org.ovirt.engine.core.bll.QueriesCommandBase;
import org.ovirt.engine.core.common.queries.StoragePoolQueryParametersBase;

public class GetStorageDomainsByStoragePoolIdQuery<P extends StoragePoolQueryParametersBase>
        extends QueriesCommandBase<P> {
    public GetStorageDomainsByStoragePoolIdQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        getQueryReturnValue().setReturnValue(getDbFacade().
                getStorageDomainDao().getAllForStoragePool(getParameters().getStoragePoolId(),
                        getUserID(),
                        getParameters().isFiltered()));
    }
}
