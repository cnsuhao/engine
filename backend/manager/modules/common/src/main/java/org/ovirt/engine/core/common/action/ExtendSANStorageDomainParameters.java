package org.ovirt.engine.core.common.action;

import org.ovirt.engine.core.compat.*;
import org.ovirt.engine.core.common.businessentities.*;

import java.util.ArrayList;

public class ExtendSANStorageDomainParameters extends StorageDomainParametersBase {
    private static final long serialVersionUID = 1051216804598069936L;
    private java.util.ArrayList<String> privateLunIds;

    public java.util.ArrayList<String> getLunIds() {
        return privateLunIds == null ? new ArrayList<String>() : privateLunIds;
    }

    public void setLunIds(java.util.ArrayList<String> value) {
        privateLunIds = value;
    }

    private java.util.ArrayList<LUNs> privateLunsList;

    public java.util.ArrayList<LUNs> getLunsList() {
        return privateLunsList == null ? new ArrayList<LUNs>() : privateLunsList;
    }

    public void setLunsList(java.util.ArrayList<LUNs> value) {
        privateLunsList = value;
    }

    private boolean force;

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public ExtendSANStorageDomainParameters(Guid storageDomainId, ArrayList<String> lunIds) {
        super(storageDomainId);
        setLunIds(lunIds);
    }

    public ExtendSANStorageDomainParameters(Guid storageDomainId, ArrayList<String> lunIds, boolean force) {
        this(storageDomainId, lunIds);
        setForce(force);
    }

    public ExtendSANStorageDomainParameters() {
    }
}
