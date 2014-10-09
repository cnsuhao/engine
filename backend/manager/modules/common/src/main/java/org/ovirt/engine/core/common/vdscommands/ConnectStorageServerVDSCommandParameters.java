package org.ovirt.engine.core.common.vdscommands;

import org.ovirt.engine.core.compat.*;
import org.ovirt.engine.core.common.businessentities.*;

import java.util.List;

public class ConnectStorageServerVDSCommandParameters extends GetStorageConnectionsListVDSCommandParameters {
    private StorageType privateStorageType = StorageType.forValue(0);

    public StorageType getStorageType() {
        return privateStorageType;
    }

    private void setStorageType(StorageType value) {
        privateStorageType = value;
    }

    private List<StorageServerConnections> privateConnectionList;

    public List<StorageServerConnections> getConnectionList() {
        return privateConnectionList;
    }

    private void setConnectionList(List<StorageServerConnections> value) {
        privateConnectionList = value;
    }

    public ConnectStorageServerVDSCommandParameters(Guid vdsId, Guid storagePoolId, StorageType storageType,
            List<StorageServerConnections> connectionList) {
        super(vdsId, storagePoolId);
        setStorageType(storageType);
        setConnectionList(connectionList);
    }

    public ConnectStorageServerVDSCommandParameters() {
    }

    @Override
    public String toString() {
        return String.format("%s, storageType = %s, connectionList = [%s]",
                super.toString(), getStorageType().name(), getPrintableConnectionsList());
    }

    private String getPrintableConnectionsList() {
        StringBuilder sb = new StringBuilder();
        for (StorageServerConnections con : getConnectionList()) {
            sb.append(con);
        }
        return sb.toString();
    }
}
