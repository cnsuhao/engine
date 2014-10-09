package org.ovirt.engine.core.dao;

import org.ovirt.engine.core.compat.Guid;

/**
 * A utility class for DAO testing which maps the fixtures entities to constants, for easy testing.
 */
public class FixturesTool {
    /**
     * Predefined NFS storage pool.
     */
    protected static final Guid STORAGE_POOL_NFS = new Guid("72b9e200-f48b-4687-83f2-62828f249a47");

    /**
     * Predefined NFS storage pool.
     */
    protected static final Guid STORAGE_POOL_NFS_2 = new Guid("386bffd1-e7ed-4b08-bce9-d7df10f8c9a0");

    /**
     * Predefined ISCSI storage pool.
     */
    protected static final Guid STORAGE_POOL_RHEL6_ISCSI = new Guid("6d849ebf-755f-4552-ad09-9a090cda105");

    /**
     * Another predefined ISCSI storage pool.
     */
    protected static final Guid STORAGE_POOL_RHEL6_ISCSI_OTHER = new Guid("6d849ebf-755f-4552-ad09-9a090cda105d");

    /**
     * Predefined NFS master storage domain.
     */
    protected static final Guid STORAGE_DOAMIN_NFS_MASTER = new Guid("c2211b56-8869-41cd-84e1-78d7cb96f31d");

    /**
     * Predefined NFS iso storage domain.
     */
    protected static final Guid STORAGE_DOAMIN_NFS_ISO = new Guid("17e7489d-d490-4681-a322-073ca19bd33d");

    /**
     * Predefined shared iso storage domain for both the storgae pools
     */
    protected static final Guid SHARED_ISO_STORAGE_DOAMIN_FOR_SP2_AND_SP3 =
            new Guid("d034f3b2-fb9c-414a-b1be-1e642cfe57ae");

    /**
     * Predefined scale storage domain.
     */
    protected static final Guid STORAGE_DOAMIN_SCALE_SD5 = new Guid("72e3a666-89e1-4005-a7ca-f7548004a9ab");

    /**
     * Predefined vds group.
     */
    protected static final Guid VDS_GROUP_RHEL6_ISCSI = new Guid("b399944a-81ab-4ec5-8266-e19ba7c3c9d1");

    /**
     * Predefined vds group.
     */
    protected static final Guid VDS_GROUP_RHEL6_NFS = new Guid("0e57070e-2469-4b38-84a2-f111aaabd49d");

    /**
     * Predefined vds group.
     */
    protected static final Guid VDS_GROUP_RHEL6_NFS_2 = new Guid("eba797fb-8e3b-4777-b63c-92e7a5957d7c");

    /**
     * Predefined vds group, with no specific quotas associated to it.
     */
    protected static final Guid VDS_GROUP_RHEL6_NFS_NO_SPECIFIC_QUOTAS =
            new Guid("eba797fb-8e3b-4777-b63c-92e7a5957d7e");

    /**
     * Predefined vds group with no running VMs
     */
    protected static final Guid VDS_GROUP_NO_RUNNING_VMS = new Guid("b399944a-81ab-4ec5-8266-e19ba7c3c9d3");

    /**
     * Predefined NFS storage pool.
     */
    protected static final Guid VDS_RHEL6_NFS_SPM = new Guid("afce7a39-8e8c-4819-ba9c-796d316592e6");

    /**
     * Predefined VDS gluster2-vdsc.redhat.com
     */
    protected static final Guid VDS_GLUSTER_SERVER2 = new Guid("2001751e-549b-4e7a-aff6-32d36856c125");

    /**
     * Predefined quota with general limitations. Its GUID is 88296e00-0cad-4e5a-9291-008a7b7f4399.<BR/>
     * The <code>Quota</code> has the following limitations:
     * <ul>Global limitation:
     * <li>virtual_cpu = 100</li>
     * <li>mem_size_mb = 10000</li>
     * <li>storage_size_gb = 1000000</li></ul>
     * Specific cluster ID c2211b56-8869-41cd-84e1-78d7cb96f31d(STORAGE_DOAMIN_NFS_MASTER) with no limitations
     */
    protected static final Guid QUOTA_GENERAL = new Guid("88296e00-0cad-4e5a-9291-008a7b7f4399");

    /**
     * The default unlimited quota with quota id 88296e00-0cad-4e5a-9291-008a7b7f4404 fir storage pool rhel6.NFS
     * (72b9e200-f48b-4687-83f2-62828f249a47)
     */
    protected static final Guid DEFAULT_QUOTA_GENERAL = new Guid("88296e00-0cad-4e5a-9291-008a7b7f4404");

    /**
     * Predefined quota with specific limitations, Its GUID is 88296e00-0cad-4e5a-9291-008a7b7f4400.
     * <ul>Global limitation:
     * <li>virtual_cpu = 100</li></ul>
     * Specific storage ID c2211b56-8869-41cd-84e1-78d7cb96f31d(STORAGE_DOAMIN_NFS_MASTER) <ul><li>storage_size_gb = 1000</li></ul>
     * Specific vdsGroup ID b399944a-81ab-4ec5-8266-e19ba7c3c9d1(VDS_GROUP_RHEL6_ISCSI) <ul><li>virtual_cpu = 10</li><li>mem_size_mb = -1</li></ul>
     * Specific vdsGroup ID 0e57070e-2469-4b38-84a2-f111aaabd49d(VDS_GROUP_RHEL6_NFS) <ul><li>virtual_cpu = null</li><li>mem_size_mb = -1</li></ul>
     */
    protected static final Guid QUOTA_SPECIFIC = new Guid("88296e00-0cad-4e5a-9291-008a7b7f4400");

    /**
     * Predefined quota with general and specific limitations.
     */
    protected static final Guid QUOTA_SPECIFIC_AND_GENERAL = new Guid("88296e00-0cad-4e5a-9291-008a7b7f4401");

    /**
     * Predefined quota with no limitations.
     */
    protected static final Guid QUOTA_EMPTY = new Guid("88296e00-0cad-4e5a-9291-008a7b7f4405");

    /**
     * Predefined VM for testing with the following properties :
     * <ul>
     * <li>VM name: rhel5-pool-50</li>
     * <li>Vds group: rhel6.iscsi (b399944a-81ab-4ec5-8266-e19ba7c3c9d1)</li>
     * <li>Based on template: 1 (1b85420c-b84c-4f29-997e-0eb674b40b79)</li></ul>
     */
    protected static final Guid VM_RHEL5_POOL_50 = new Guid("77296e00-0cad-4e5a-9299-008a7b6f4354");

    /**
     * Predefined VM for testing with the following properties :
     * <ul>
     * <li>VM name: rhel5-pool-51</li>
     * <li>Vds group: rhel6.iscsi (b399944a-81ab-4ec5-8266-e19ba7c3c9d1)</li>
     * <li>Based on template: 1 (1b85420c-b84c-4f29-997e-0eb674b40b79)</li>
     * </ul>
     */
    protected static final Guid VM_RHEL5_POOL_51 = new Guid("77296e00-0cad-4e5a-9299-008a7b6f4356");

    /**
     * Predefined VM for testing with the following properties :
     * <ul>
     * <li>VM name: rhel5-pool-57</li>
     * <li>Vds group: rhel6.iscsi (b399944a-81ab-4ec5-8266-e19ba7c3c9d1)</li>
     * <li>Based on template: 1 (1b85420c-b84c-4f29-997e-0eb674b40b79)</li>
     * </ul>
     */
    protected static final Guid VM_RHEL5_POOL_57 = new Guid("77296e00-0cad-4e5a-9299-008a7b6f4355");

    /**
     * Predefined VM for testing with the following properties :
     * <ul>
     * <li>VM name: rhel5-pool-59</li>
     * <li>Vds group: rhel6.nfs2 (0e57070e-2469-4b38-84a2-f111aaabd49d)</li>
     * <li>Based on template: 1 (1b85420c-b84c-4f29-997e-0eb674b40b79)</li>
     * </ul>
     */
    protected static final Guid VM_RHEL5_POOL_59 = new Guid("77296e00-0cad-4e5a-9299-008a7b6f4359");

    /**
     * Predefined VM for testing with the following properties :
     * <ul>
     * <li>VM name: rhel5-pool-60</li>
     * <li>Vds group: rhel6.nfs2 (0e57070e-2469-4b38-84a2-f111aaabd49d)</li>
     * <li>Based on template: 1 (1b85420c-b84c-4f29-997e-0eb674b40b79)</li>
     * </ul>
     */
    protected static final Guid VM_RHEL5_POOL_60 = new Guid("77296e00-0cad-4e5a-9299-008a7b6f4360");

    /**
     * Predefined template for testing with the following properties :
     * <ul>
     * <li>Vds group: rhel6.iscsi (b399944a-81ab-4ec5-8266-e19ba7c3c9d1)</li>
     * </ul>
     */
    protected static final Guid VM_TEMPLATE_RHEL5 = new Guid("1b85420c-b84c-4f29-997e-0eb674b40b79");

    /**
     * Predefined template for testing with the following properties :
     * <ul>
     * <li>Vds group: rhel6.iscsi (b399944a-81ab-4ec5-8266-e19ba7c3c9d1)</li>
     * </ul>
     */
    protected static final Guid VM_TEMPLATE_RHEL5_2 = new Guid("1b85420c-b84c-4f29-997e-0eb674b40b80");

    /**
     * Predefined template for testing with the following properties :
     * <ul>
     * <li>Vds group: rhel6.nfs2 (0e57070e-2469-4b38-84a2-f111aaabd49d)</li>
     * </ul>
     */
    protected static final Guid VM_TEMPLATE_RHEL6_1 = new Guid("1b85420c-b84c-4f29-997e-0eb674b40b81");

    /**
     * Predefined template for testing with the following properties :
     * <ul>
     * <li>Vds group: rhel6.nfs2 (0e57070e-2469-4b38-84a2-f111aaabd49d)</li>
     * </ul>
     */
    protected static final Guid VM_TEMPLATE_RHEL6_2 = new Guid("1b85420c-b84c-4f29-997e-0eb674b40b82");

    /**
     * Predefined user for testing with the following properties :
     * <ul>
     * <li>Ad group id : 9bf7c640-b620-456f-a550-0348f366544b</li>
     * <li>Group name : philosophers</li>
     * <li>Status : 1</li>
     * <li>Domain : rhel</li>
     * <li>Role : jUnitTestRole</li>
     * <li>Action group ids : 4, 901</li></ul>
     */
    protected static final Guid USER_EXISTING_ID = new Guid("9bf7c640-b620-456f-a550-0348f366544b");

    /**
     * Predefined image for testing.
     */
    protected static final Guid IMAGE_ID = new Guid("42058975-3d5e-484a-80c1-01c31207f578");

    /**
     * Predefined disk for testing.
     */
    protected static final Guid DISK_ID = new Guid("1b26a52b-b60f-44cb-9f46-3ef333b04a34");

    /**
     * Predefined image on a VM template for testing. <BR/>
     * The image is defined on storage domain STORAGE_DOAMIN_SCALE_SD5.
     */
    protected static final Guid TEMPLATE_IMAGE_ID = new Guid("52058975-3d5e-484a-80c1-01c31207f578");

    /**
     * Predefined image group for testing.
     */
    protected static final Guid IMAGE_GROUP_ID = new Guid("1b26a52b-b60f-44cb-9f46-3ef333b04a35");

    /**
     * Predefined floating disk for testing.
     */
    protected static final Guid FLOATING_DISK_ID = new Guid("1b26a52b-b60f-44cb-9f46-3ef333b04a36");

    /**
     * Predefined floating lun for testing.
     */
    protected static final Guid FLOATING_LUN_ID = new Guid("1b26a52b-b60f-44cb-9f46-3ef333b04a37");

    /**
     * Predefined entity that has associated tasks.
     */
    protected static final Guid ENTITY_WITH_TASKS_ID = new Guid("72e3a666-89e1-4005-a7ca-f7548004a9ab");

    /**
     * Predefined async task
     */
    protected static final Guid EXISTING_TASK_ID = new Guid("340fd52b-3400-4cdd-8d3f-C9d03704b0aa");

    /**
     * ID of an existing snapshot
     */
    protected static final Guid EXISTING_SNAPSHOT_ID = new Guid("a7bb24df-9fdf-4bd6-b7a9-f5ce52da0f89");

    /**
     * ID of an existing storage connection
     */
    protected static final Guid EXISTING_STORAGE_CONNECTION_ID = new Guid("7fe9a439-a68e-4c15-8885-29d34eb6cabf");

    /**
     * ID of an existing NFS storage connection with version 'auto'
     */
    protected static final Guid EXISTING_STORAGE_CONNECTION_NFS_AUTO_ID = new Guid("0cc146e8-e5ed-482c-8814-270bc48c2981");

    /**
     * ID of an existing storage domain with the storage connection which ID is defined in
     * EXISTING_STORAGE_CONNECTION_ID
     */
    protected static final Guid EXISTING_DOMAIN_ID_FOR_CONNECTION_ID = new Guid("c2211b56-8869-41cd-84e1-78d7cb96f31d");

     /** Predefined Network for testing with the following properties :
     * <ul>
     * <li>name: engine2</li>
     * <li>description: Management Network</li>
     * <li>stp: 0</li>
     * <li>storage_pool_id: rhel6.iscsi (6d849ebf-755f-4552-ad09-9a090cda105d)</li>
     * </ul>
     */
    protected static final Guid NETWORK_ENGINE_2 = new Guid("58d5c1c6-cb15-4832-b2a4-023770607189");

    /**
     * Predefined Network for testing with the following properties :
     * <ul>
     * <li>name: engine</li>
     * <li>description: Management Network</li>
     * <li>stp: 0</li>
     * <li>storage_pool_id: rhel6.iscsi (6d849ebf-755f-4552-ad09-9a090cda105d)</li>
     * </ul>
     */
    public static final Guid NETWORK_ENGINE = new Guid("58d5c1c6-cb15-4832-b2a4-023770607188");

    /**
     * Predefined Network without entries in network_cluster with the following properties :
     * <ul>
     * <li>name: engine3</li>
     * <li>description: Management Network</li>
     * <li>stp: 0</li>
     * <li>storage_pool_id: rhel6.iscsi (6d849ebf-755f-4552-ad09-9a090cda105d)</li>
     * </ul>
     */
    public static final Guid NETWORK_NO_CLUSTERS_ATTACHED = new Guid("58d5c1c6-cb15-4832-b2a4-023770607190");

    /**
     * Predefined VmNetworkInterface with the following properties :
     * <ul>
     * <li>network_name: engine</li>
     * <li>vm_guid: 77296e00-0cad-4e5a-9299-008a7b6f4355</li>
     * <li>vmt_guid: 1b85420c-b84c-4f29-997e-0eb674b40b79</li>
     * <li>mac_addr: 00:1a:4a:16:87:d9</li>
     * <li>name: nic1</li>
     * </ul>
     */
    public static final Guid VM_NETWORK_INTERFACE = new Guid("e2817b12-f873-4046-b0da-0098293c14fd");

    /**
     * Predefined VmNetworkInterface with the following properties:
     * <ul>
     * <li>network_name: engine</li>
     * <li>vmt_guid: 1b85420c-b84c-4f29-997e-0eb674b40b79</li>
     * <li>mac_addr: 00:1a:4a:16:87:d9</li>
     * <li>name: nic1</li>
     * </ul>
     */
    public static final Guid TEMPLATE_NETWORK_INTERFACE = new Guid("e2817b12-f873-4046-b0da-0098293c0000");

    /**
     * Predefined VdsNetworkInterface with the following properties :
     * <ul>
     * <li>name: eth0</li>
     * <li>network_name: engine</li>
     * <li>vds_id: afce7a39-8e8c-4819-ba9c-796d316592e6</li>
     * <li>mac_addr: 78:E7:D1:E4:8C:70</li>
     * </ul>
     */
    public static final Guid VDS_NETWORK_INTERFACE = new Guid("ba31682e-6ae7-4f9d-8c6f-04c93acca9db");

    public static final int NUMBER_OF_VM_INTERFACES = 2;
}
