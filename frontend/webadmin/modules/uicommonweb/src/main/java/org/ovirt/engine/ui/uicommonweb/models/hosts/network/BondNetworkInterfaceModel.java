package org.ovirt.engine.ui.uicommonweb.models.hosts.network;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.ui.uicommonweb.models.hosts.HostSetupNetworksModel;

/**
 * A Model for Bond Interfaces
 */
public class BondNetworkInterfaceModel extends NetworkInterfaceModel {

    private final List<NetworkInterfaceModel> bonded;

    public BondNetworkInterfaceModel(VdsNetworkInterface bondNic,
            List<LogicalNetworkModel> nicNetworks,
            List<NetworkInterfaceModel> bonded, HostSetupNetworksModel setupModel) {
        super(bondNic, nicNetworks, setupModel);
        this.bonded = bonded;
        for (NetworkInterfaceModel bondedNic : bonded) {
            bondedNic.setBond(this);
        }
    }

    public List<NetworkInterfaceModel> getBonded() {
        return bonded;
    }

    /**
     * Break the Bond
     */
    public void breakBond() {
        // remove bond name
        for (NetworkInterfaceModel bonded : getBonded()) {
            bonded.getEntity().setBondName(null);
        }
    }
}
