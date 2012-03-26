package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.queries.GetAllAttachableDisks;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

public class GetAllAttachableDisksQuery<P extends GetAllAttachableDisks> extends QueriesCommandBase<P> {

    public GetAllAttachableDisksQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        setReturnValue(DbFacade.getInstance()
                .getDiskImageDAO()
                .getAllAttachableDisksByPoolId(getParameters().getStoragePoolId(),
                        getUserID(),
                        getParameters().isFiltered()));
    }
}
