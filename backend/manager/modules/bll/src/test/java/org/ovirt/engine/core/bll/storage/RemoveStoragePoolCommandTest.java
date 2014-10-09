package org.ovirt.engine.core.bll.storage;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.action.StoragePoolParametersBase;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.storage_domains;

public class RemoveStoragePoolCommandTest {

    private static RemoveStoragePoolCommand<StoragePoolParametersBase> createCommand(StoragePoolParametersBase param) {
        return new RemoveStoragePoolCommand<StoragePoolParametersBase>(param);
    }

    @Test
    public void testEmptyDomainList() {
        StoragePoolParametersBase param = new StoragePoolParametersBase();
        RemoveStoragePoolCommand<StoragePoolParametersBase> cmd = createCommand(param);
        List<storage_domains> domainsList = new ArrayList<storage_domains>();
        List<storage_domains> listReturned = cmd.getActiveOrLockedDomainList(domainsList);
        assertTrue(listReturned.isEmpty());
    }

    /**
     * Test when Active domain is in the Data Center
     */
    @Test
    public void testActiveDomainInList() {
        StoragePoolParametersBase param = new StoragePoolParametersBase();
        RemoveStoragePoolCommand<StoragePoolParametersBase> cmd = createCommand(param);
        List<storage_domains> domainsList = new ArrayList<storage_domains>();
        storage_domains tempStorageDomains = new storage_domains();
        tempStorageDomains.setstatus(StorageDomainStatus.Active);
        domainsList.add(tempStorageDomains);
        List<storage_domains> listReturned = cmd.getActiveOrLockedDomainList(domainsList);
        assertTrue(!listReturned.isEmpty());
    }

    /**
     * Test when there is locked domain is in the Data Center.
     */
    @Test
    public void testLockedDomainInList() {
        StoragePoolParametersBase param = new StoragePoolParametersBase();
        RemoveStoragePoolCommand<StoragePoolParametersBase> cmd = createCommand(param);
        List<storage_domains> domainsList = new ArrayList<storage_domains>();
        storage_domains tempStorageDomains = new storage_domains();
        tempStorageDomains.setstatus(StorageDomainStatus.Locked);
        domainsList.add(tempStorageDomains);
        List<storage_domains> listReturned = cmd.getActiveOrLockedDomainList(domainsList);
        assertTrue(!listReturned.isEmpty());
    }

    /**
     * Test when there is locked domain and active is in the Data Center.
     */
    @Test
    public void testLockedAndActiveDomainInList() {
        StoragePoolParametersBase param = new StoragePoolParametersBase();
        RemoveStoragePoolCommand<StoragePoolParametersBase> cmd = createCommand(param);
        List<storage_domains> domainsList = new ArrayList<storage_domains>();

        // Add first locked storage
        storage_domains tempStorageDomains = new storage_domains();
        tempStorageDomains.setstatus(StorageDomainStatus.Locked);
        domainsList.add(tempStorageDomains);

        // Add second active storage
        tempStorageDomains.setstatus(StorageDomainStatus.Active);
        domainsList.add(tempStorageDomains);

        List<storage_domains> listReturned = cmd.getActiveOrLockedDomainList(domainsList);
        assertTrue(listReturned.size() == 2);
    }

    /**
     * Test when there is in active domain.
     */
    @Test
    public void testInActiveDomainInList() {
        StoragePoolParametersBase param = new StoragePoolParametersBase();
        RemoveStoragePoolCommand<StoragePoolParametersBase> cmd = createCommand(param);
        List<storage_domains> domainsList = new ArrayList<storage_domains>();

        // Add first locked storage
        storage_domains tempStorageDomains = new storage_domains();
        tempStorageDomains.setstatus(StorageDomainStatus.InActive);
        domainsList.add(tempStorageDomains);

        List<storage_domains> listReturned = cmd.getActiveOrLockedDomainList(domainsList);
        assertTrue(listReturned.isEmpty());
    }
}
