

----------------------------------------------------------------
-- [network] Table
--



Create or replace FUNCTION Insertnetwork(v_addr VARCHAR(50) ,
	v_description VARCHAR(4000) ,
	v_id UUID,
	v_name VARCHAR(50),
	v_subnet VARCHAR(20) ,
	v_gateway VARCHAR(20) ,
	v_type INTEGER ,
	v_vlan_id INTEGER ,
	v_stp BOOLEAN ,
    	v_storage_pool_id UUID,
	v_mtu INTEGER,
	v_vm_network BOOLEAN)
RETURNS VOID
   AS $procedure$
BEGIN
INSERT INTO network(addr, description, id, name, subnet, gateway, type, vlan_id, stp, storage_pool_id, mtu, vm_network)
	VALUES(v_addr, v_description, v_id, v_name, v_subnet, v_gateway, v_type, v_vlan_id, v_stp, v_storage_pool_id, v_mtu, v_vm_network );
END; $procedure$
LANGUAGE plpgsql;




Create or replace FUNCTION Updatenetwork(v_addr VARCHAR(50) ,
	v_description VARCHAR(4000) ,
	v_id UUID,
	v_name VARCHAR(50),
	v_subnet VARCHAR(20) ,
	v_gateway VARCHAR(20) ,
	v_type INTEGER ,
	v_vlan_id INTEGER ,
	v_stp BOOLEAN ,
	v_storage_pool_id UUID,
	v_mtu INTEGER,
	v_vm_network BOOLEAN)
RETURNS VOID

	--The [network] table doesn't have a timestamp column. Optimistic concurrency logic cannot be generated
   AS $procedure$
BEGIN
      UPDATE network
      SET addr = v_addr,description = v_description,name = v_name,subnet = v_subnet,
      gateway = v_gateway,type = v_type,vlan_id = v_vlan_id,
      stp = v_stp,storage_pool_id = v_storage_pool_id, mtu = v_mtu,
      vm_network = v_vm_network
      WHERE id = v_id;
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION Deletenetwork(v_id UUID)
RETURNS VOID
   AS $procedure$
   DECLARE
   v_val  UUID;
BEGIN

	-- Get (and keep) a shared lock with "right to upgrade to exclusive"
	-- in order to force locking parent before children
   select   id INTO v_val FROM network  WHERE id = v_id     FOR UPDATE;

   DELETE FROM network
   WHERE id = v_id;

   -- Delete the network's permissions
   DELETE FROM permissions WHERE object_id = v_id;

END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION GetAllFromnetwork(v_user_id uuid, v_is_filtered boolean) RETURNS SETOF network
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM network
   WHERE NOT v_is_filtered OR EXISTS (SELECT 1
                                      FROM   user_network_permissions_view
                                      WHERE  user_id = v_user_id AND entity_id = network.id);

END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION GetnetworkByid(v_id UUID) RETURNS SETOF network
   AS $procedure$
BEGIN
RETURN QUERY SELECT *
   FROM network
   WHERE id = v_id;

END; $procedure$
LANGUAGE plpgsql;






Create or replace FUNCTION GetnetworkByName(v_networkName VARCHAR(50))
RETURNS SETOF network
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM network
   WHERE name = v_networkName;

END; $procedure$
LANGUAGE plpgsql;



Create or replace FUNCTION GetNetworkByNameAndDataCenter(v_name VARCHAR(50), v_storage_pool_id UUID)
RETURNS SETOF network
   AS $procedure$
BEGIN
   RETURN QUERY SELECT network.*
   FROM network
   WHERE network.name = v_name
   AND   network.storage_pool_id = v_storage_pool_id;

END; $procedure$
LANGUAGE plpgsql;



Create or replace FUNCTION GetNetworkByNameAndCluster(v_name VARCHAR(50), v_cluster_id UUID)
RETURNS SETOF network
   AS $procedure$
BEGIN
   RETURN QUERY SELECT network.*
   FROM network
   WHERE network.name = v_name
   AND EXISTS (SELECT 1
               FROM network_cluster
               WHERE network.id = network_cluster.network_id
               AND   network_cluster.cluster_id = v_cluster_id);

END; $procedure$
LANGUAGE plpgsql;



Create or replace FUNCTION GetAllNetworkByStoragePoolId(v_id UUID, v_user_id uuid, v_is_filtered boolean)
RETURNS SETOF network
   AS $procedure$
BEGIN
RETURN QUERY SELECT *
   FROM network
   WHERE storage_pool_id = v_id
   AND (NOT v_is_filtered OR EXISTS (SELECT 1
                                     FROM   user_network_permissions_view
                                     WHERE  user_id = v_user_id AND entity_id = network.id));

END; $procedure$
LANGUAGE plpgsql;


DROP TYPE IF EXISTS networkViewClusterType CASCADE;
CREATE TYPE networkViewClusterType AS(id uuid,name VARCHAR(50),description VARCHAR(4000),type INTEGER,
            addr VARCHAR(50),subnet VARCHAR(20),gateway VARCHAR(20),vlan_id INTEGER,stp BOOLEAN,storage_pool_id UUID,
	    mtu INTEGER, vm_network BOOLEAN, network_id UUID,cluster_id UUID, status INTEGER, is_display BOOLEAN,
	    required BOOLEAN);
Create or replace FUNCTION GetAllNetworkByClusterId(v_id UUID, v_user_id uuid, v_is_filtered boolean)
RETURNS SETOF networkViewClusterType
   AS $procedure$
BEGIN
RETURN QUERY SELECT
    DISTINCT
    network.id,
    network.name,
    network.description,
    network.type,
    network.addr,
    network.subnet,
    network.gateway,
    network.vlan_id,
    network.stp,
    network.storage_pool_id,
    network.mtu,
    network.vm_network,
    network_cluster.network_id,
    network_cluster.cluster_id,
    network_cluster.status,
    network_cluster.is_display,
    network_cluster.required
   FROM network
   INNER JOIN network_cluster
   ON network.id = network_cluster.network_id
   WHERE network_cluster.cluster_id = v_id
   AND (NOT v_is_filtered OR EXISTS (SELECT 1
                                     FROM   user_network_permissions_view
                                     WHERE  user_id = v_user_id AND entity_id = network.id))
   ORDER BY network.name;
END; $procedure$
LANGUAGE plpgsql;




--The GetByFK stored procedure cannot be created because the [network] table doesn't have at least one foreign key column or the foreign keys are also primary keys.

----------------------------------------------------------------
-- [vds_interface] Table
--


Create or replace FUNCTION Insertvds_interface(v_addr VARCHAR(20) ,
 v_bond_name VARCHAR(50) ,
 v_bond_type INTEGER ,
 v_gateway VARCHAR(20) ,
 v_id UUID,
 v_is_bond BOOLEAN ,
 v_bond_opts VARCHAR(4000) ,
 v_mac_addr VARCHAR(20) ,
 v_name VARCHAR(50),
 v_network_name VARCHAR(50) ,
 v_speed INTEGER ,
 v_subnet VARCHAR(20) ,
 v_boot_protocol INTEGER ,
 v_type INTEGER ,
 v_vds_id UUID,
 v_vlan_id INTEGER,
 v_mtu INTEGER,
 v_bridged BOOLEAN)
RETURNS VOID
   AS $procedure$
BEGIN
INSERT INTO vds_interface(addr, bond_name, bond_type, gateway, id, is_bond, bond_opts, mac_addr, name, network_name, speed, subnet, boot_protocol, type, VDS_ID, vlan_id, mtu, bridged)
	VALUES(v_addr, v_bond_name, v_bond_type, v_gateway, v_id, v_is_bond, v_bond_opts, v_mac_addr, v_name, v_network_name, v_speed, v_subnet, v_boot_protocol, v_type, v_vds_id, v_vlan_id, v_mtu, v_bridged);
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION Updatevds_interface(v_addr VARCHAR(20) ,
 v_bond_name VARCHAR(50) ,
 v_bond_type INTEGER ,
 v_gateway VARCHAR(20) ,
 v_id UUID,
 v_is_bond BOOLEAN ,
 v_bond_opts VARCHAR(4000) ,
 v_mac_addr VARCHAR(20) ,
 v_name VARCHAR(50),
 v_network_name VARCHAR(50) ,
 v_speed INTEGER ,
 v_subnet VARCHAR(20) ,
 v_boot_protocol INTEGER ,
 v_type INTEGER ,
 v_vds_id UUID,
 v_vlan_id INTEGER,
 v_mtu INTEGER,
 v_bridged BOOLEAN)
RETURNS VOID

	--The [vds_interface] table doesn't have a timestamp column. Optimistic concurrency logic cannot be generated
   AS $procedure$
BEGIN
      UPDATE vds_interface
      SET addr = v_addr,bond_name = v_bond_name,bond_type = v_bond_type,gateway = v_gateway,
      is_bond = v_is_bond,bond_opts = v_bond_opts,mac_addr = v_mac_addr,
      name = v_name,network_name = v_network_name,speed = v_speed,
      subnet = v_subnet,boot_protocol = v_boot_protocol,
      type = v_type,VDS_ID = v_vds_id,vlan_id = v_vlan_id,_update_date = LOCALTIMESTAMP, mtu = v_mtu,
      bridged = v_bridged
      WHERE id = v_id;
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION Deletevds_interface(v_id UUID)
RETURNS VOID
   AS $procedure$
BEGIN

   DELETE FROM vds_interface
   WHERE id = v_id;

END; $procedure$
LANGUAGE plpgsql;







Create or replace FUNCTION Getvm_interfaceByvm_id(v_vm_id UUID, v_user_id UUID, v_is_filtered BOOLEAN)
RETURNS SETOF vm_interface_view
   AS $procedure$
BEGIN
RETURN QUERY SELECT *
   FROM vm_interface_view
   WHERE vm_guid = v_vm_id
   AND (NOT v_is_filtered OR EXISTS (SELECT 1
                                     FROM   user_vm_permissions_view
                                     WHERE  user_id = v_user_id AND entity_id = v_vm_id));

END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION Getinterface_viewByvds_id(v_vds_id UUID, v_user_id UUID, v_is_filtered boolean)
RETURNS SETOF vds_interface_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM vds_interface_view
   WHERE vds_id = v_vds_id
   AND (NOT v_is_filtered OR EXISTS (SELECT 1
                                     FROM   user_vds_permissions_view
                                     WHERE  user_id = v_user_id AND entity_id = v_vds_id));

END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION Getinterface_viewByAddr(v_cluster_id UUID, v_addr VARCHAR(50))
RETURNS SETOF vds_interface_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT vds_interface_view.*
   FROM vds_interface_view
   INNER JOIN vds_static
   ON vds_interface_view.vds_id = vds_static.vds_id
   WHERE vds_interface_view.addr = v_addr
   AND vds_static.vds_group_id = v_cluster_id;
END; $procedure$
LANGUAGE plpgsql;




Create or replace FUNCTION GetVdsManagedInterfaceByVdsId(v_vds_id UUID, v_user_id UUID, v_is_filtered boolean)
RETURNS SETOF vds_interface_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM vds_interface_view
   -- Checking if the 2nd bit in the type column is set, meaning that the interface is managed
   WHERE vds_id = v_vds_id AND (type & 2) = 2
   AND (NOT v_is_filtered OR EXISTS (SELECT 1
                                     FROM   user_vds_permissions_view
                                     WHERE  user_id = v_user_id AND entity_id = v_vds_id));

END; $procedure$
LANGUAGE plpgsql;



Create or replace FUNCTION GetVdsInterfacesByNetworkId(v_network_id UUID) RETURNS SETOF vds_interface_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT vds_interface_view.*
   FROM vds_interface_view
   INNER JOIN vds
   ON vds.vds_id = vds_interface_view.vds_id
   INNER JOIN network_cluster
   ON network_cluster.cluster_id = vds.vds_group_id
   INNER JOIN network
   ON network.id = network_cluster.network_id
   AND network.name = vds_interface_view.network_name
   WHERE network.id = v_network_id;
END; $procedure$
LANGUAGE plpgsql;



Create or replace FUNCTION GetVdsInterfaceById(v_vds_interface_id UUID) RETURNS SETOF vds_interface_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM vds_interface_view
   WHERE id = v_vds_interface_id;
END; $procedure$
LANGUAGE plpgsql;


----------------------------------------------------------------
-- [vm_interface] Table
--


Create or replace FUNCTION Insertvm_interface(v_id UUID,
	v_mac_addr VARCHAR(20) ,
	v_name VARCHAR(50),
	v_network_name VARCHAR(50) ,
	v_speed INTEGER ,
	v_vm_guid UUID ,
	v_vmt_guid UUID ,
    v_type INTEGER,
    v_port_mirroring BOOLEAN,
    v_linked BOOLEAN)
RETURNS VOID
   AS $procedure$
BEGIN
INSERT INTO vm_interface(id, mac_addr, name, network_name, speed, VM_GUID, VMT_GUID, type, port_mirroring, linked)
	VALUES(v_id, v_mac_addr, v_name, v_network_name, v_speed, v_vm_guid, v_vmt_guid, v_type, v_port_mirroring, v_linked);
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION Updatevm_interface(v_id UUID,
	v_mac_addr VARCHAR(20) ,
	v_name VARCHAR(50),
	v_network_name VARCHAR(50) ,
	v_speed INTEGER ,
	v_vm_guid UUID ,
	v_vmt_guid UUID ,
    v_type INTEGER,
    v_port_mirroring BOOLEAN,
    v_linked BOOLEAN)
RETURNS VOID

	--The [vm_interface] table doesn't have a timestamp column. Optimistic concurrency logic cannot be generated
   AS $procedure$
BEGIN
      UPDATE vm_interface
      SET mac_addr = v_mac_addr,name = v_name,network_name = v_network_name,
      speed = v_speed,VM_GUID = v_vm_guid,VMT_GUID = v_vmt_guid,type = v_type,
      _update_date = LOCALTIMESTAMP, port_mirroring = v_port_mirroring, linked = v_linked
      WHERE id = v_id;
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION Deletevm_interface(v_id UUID)
RETURNS VOID
   AS $procedure$
   DECLARE
   v_val  UUID;
BEGIN

	-- Get (and keep) a shared lock with "right to upgrade to exclusive"
	-- in order to force locking parent before children
   select   id INTO v_val FROM vm_interface  WHERE id = v_id     FOR UPDATE;

   DELETE FROM vm_interface
   WHERE id = v_id;

END; $procedure$
LANGUAGE plpgsql;






Create or replace FUNCTION GetAllFromvm_interface() RETURNS SETOF vm_interface_view
   AS $procedure$
BEGIN
RETURN QUERY SELECT *
   FROM vm_interface_view;

END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION Getvm_interfaceByid(v_id UUID) RETURNS SETOF vm_interface_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM vm_interface_view
   WHERE id = v_id;

END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION Getvm_interfaceByvm_guidAndByvmt_guid(v_vm_guid UUID,
	v_vmt_guid UUID) RETURNS SETOF vm_interface
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM vm_interface
   WHERE VM_GUID = v_vm_guid AND VMT_GUID = v_vmt_guid;

END; $procedure$
LANGUAGE plpgsql;






----------------------------------------------------------------
-- [vm_interface_statistics] Table
--


Create or replace FUNCTION Getvm_interface_statisticsById(v_id UUID) RETURNS SETOF vm_interface_statistics
   AS $procedure$
BEGIN
RETURN QUERY SELECT *
   FROM vm_interface_statistics
   WHERE id = v_id;

END; $procedure$
LANGUAGE plpgsql;




Create or replace FUNCTION Insertvm_interface_statistics(v_id UUID,
	v_rx_drop DECIMAL(18,0) ,
	v_rx_rate DECIMAL(18,0) ,
	v_tx_drop DECIMAL(18,0) ,
	v_tx_rate DECIMAL(18,0) ,
	v_iface_status INTEGER ,
	v_vm_id UUID)
RETURNS VOID
   AS $procedure$
BEGIN
INSERT INTO vm_interface_statistics(id, rx_drop, rx_rate, tx_drop, tx_rate, vm_id, iface_status)
	VALUES(v_id, v_rx_drop, v_rx_rate, v_tx_drop, v_tx_rate, v_vm_id,v_iface_status);
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION Updatevm_interface_statistics(v_id UUID,
 v_rx_drop DECIMAL(18,0) ,
 v_rx_rate DECIMAL(18,0) ,
 v_tx_drop DECIMAL(18,0) ,
 v_tx_rate DECIMAL(18,0) ,
 v_iface_status INTEGER ,
 v_vm_id UUID)
RETURNS VOID

	--The [vm_interface_statistics] table doesn't have a timestamp column. Optimistic concurrency logic cannot be generated
   AS $procedure$
BEGIN
      UPDATE vm_interface_statistics
      SET rx_drop = v_rx_drop,rx_rate = v_rx_rate,tx_drop = v_tx_drop,tx_rate = v_tx_rate,
      vm_id = v_vm_id,iface_status = v_iface_status
      WHERE id = v_id;
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION Deletevm_interface_statistics(v_id UUID)
RETURNS VOID
   AS $procedure$
   DECLARE
   v_val  UUID;
BEGIN

	-- Get (and keep) a shared lock with "right to upgrade to exclusive"
	-- in order to force locking parent before children
   select   id INTO v_val FROM vm_interface_statistics  WHERE id = v_id     FOR UPDATE;

   DELETE FROM vm_interface_statistics
   WHERE id = v_id;

END; $procedure$
LANGUAGE plpgsql;




----------------------------------------------------------------
-- [network_cluster] Table
--
Create or replace FUNCTION GetVmGuestAgentInterfacesByVmId(v_vm_id UUID, v_user_id UUID, v_filtered BOOLEAN)
RETURNS SETOF vm_guest_agent_interfaces
   AS $procedure$
BEGIN
RETURN QUERY SELECT *
   FROM vm_guest_agent_interfaces
   WHERE vm_id = v_vm_id
   AND (NOT v_filtered OR EXISTS (SELECT 1
                                  FROM   user_vm_permissions_view
                                  WHERE  user_id = v_user_id AND entity_id = v_vm_id));
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION DeleteVmGuestAgentInterfacesByVmId(v_vm_id UUID)
RETURNS VOID
   AS $procedure$
BEGIN
   DELETE FROM vm_guest_agent_interfaces
   WHERE vm_id = v_vm_id;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION InsertVmGuestAgentInterface(v_vm_id UUID,
   v_interface_name VARCHAR(50),
   v_mac_address VARCHAR(59),
   v_ipv4_addresses text,
   v_ipv6_addresses text)
RETURNS VOID
   AS $procedure$
BEGIN
INSERT INTO vm_guest_agent_interfaces(vm_id, interface_name, mac_address, ipv4_addresses, ipv6_addresses)
       VALUES(v_vm_id, v_interface_name, v_mac_address, v_ipv4_addresses, v_ipv6_addresses);
END; $procedure$
LANGUAGE plpgsql;


----------------------------------------------------------------
-- [vds_interface_statistics] Table
--


Create or replace FUNCTION Insertvds_interface_statistics(v_id UUID,
	v_rx_drop DECIMAL(18,0) ,
	v_rx_rate DECIMAL(18,0) ,
	v_tx_drop DECIMAL(18,0) ,
	v_tx_rate DECIMAL(18,0) ,
	v_iface_status INTEGER ,
	v_vds_id UUID)
RETURNS VOID
   AS $procedure$
BEGIN
INSERT INTO vds_interface_statistics(id, rx_drop, rx_rate, tx_drop, tx_rate, vds_id, iface_status)
	VALUES(v_id, v_rx_drop, v_rx_rate, v_tx_drop, v_tx_rate, v_vds_id,v_iface_status);
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION Updatevds_interface_statistics(v_id UUID,
 v_rx_drop DECIMAL(18,0) ,
 v_rx_rate DECIMAL(18,0) ,
 v_tx_drop DECIMAL(18,0) ,
 v_tx_rate DECIMAL(18,0) ,
 v_iface_status INTEGER ,
 v_vds_id UUID)
RETURNS VOID

	--The [vds_interface_statistics] table doesn't have a timestamp column. Optimistic concurrency logic cannot be generated
   AS $procedure$
BEGIN
      UPDATE vds_interface_statistics
      SET rx_drop = v_rx_drop,rx_rate = v_rx_rate,tx_drop = v_tx_drop,tx_rate = v_tx_rate,
      vds_id = v_vds_id,iface_status = v_iface_status
      WHERE id = v_id;
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION Deletevds_interface_statistics(v_id UUID)
RETURNS VOID
   AS $procedure$
   DECLARE
   v_val  UUID;
BEGIN

	-- Get (and keep) a shared lock with "right to upgrade to exclusive"
	-- in order to force locking parent before children
   select   id INTO v_val FROM vds_interface_statistics  WHERE id = v_id     FOR UPDATE;

   DELETE FROM vds_interface_statistics
   WHERE id = v_id;

END; $procedure$
LANGUAGE plpgsql;






----------------------------------------------------------------
-- [network_cluster] Table
--


Create or replace FUNCTION Insertnetwork_cluster(v_cluster_id UUID,
   v_network_id UUID,
   v_status INTEGER,
   v_is_display BOOLEAN,
   v_required BOOLEAN)
RETURNS VOID
   AS $procedure$
BEGIN
INSERT INTO network_cluster(cluster_id, network_id, status, is_display, required)
	VALUES(v_cluster_id, v_network_id, v_status, v_is_display, v_required);
END; $procedure$
LANGUAGE plpgsql;




Create or replace FUNCTION Updatenetwork_cluster(v_cluster_id UUID,
    v_network_id UUID,
    v_status INTEGER,
    v_is_display BOOLEAN,
    v_required BOOLEAN)
RETURNS VOID
   AS $procedure$
BEGIN
   UPDATE network_cluster
   SET status = v_status,is_display = v_is_display, required = v_required
   WHERE cluster_id = v_cluster_id AND network_id = v_network_id;
END; $procedure$
LANGUAGE plpgsql;


Create or replace FUNCTION Updatenetwork_cluster_status(v_cluster_id UUID,
        v_network_id UUID,
        v_status INTEGER)
RETURNS VOID
   AS $procedure$
BEGIN
   UPDATE network_cluster
   SET status = v_status
   WHERE cluster_id = v_cluster_id AND network_id = v_network_id;
END; $procedure$
LANGUAGE plpgsql;






Create or replace FUNCTION Deletenetwork_cluster(v_cluster_id UUID,
	v_network_id UUID)
RETURNS VOID
   AS $procedure$
BEGIN

   DELETE FROM network_cluster
   WHERE cluster_id = v_cluster_id AND network_id = v_network_id;

END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION GetAllFromnetwork_cluster() RETURNS SETOF network_cluster
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM network_cluster;

END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION GetAllFromnetwork_clusterByClusterId(v_cluster_id UUID)
RETURNS SETOF network_cluster
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM network_cluster
   WHERE cluster_id = v_cluster_id;

END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION GetAllFromnetwork_clusterByNetworkId(v_network_id UUID)
RETURNS SETOF network_cluster
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM network_cluster
   WHERE network_id = v_network_id;

END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION Getnetwork_clusterBycluster_idAndBynetwork_id(v_cluster_id UUID,
 v_network_id UUID) RETURNS SETOF network_cluster
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM network_cluster
   WHERE cluster_id = v_cluster_id AND network_id = v_network_id;

END; $procedure$
LANGUAGE plpgsql;







Create or replace FUNCTION GetvmStaticByGroupIdAndNetwork(v_groupId UUID,
     v_networkName VARCHAR(50)) RETURNS SETOF vm_static
   AS $procedure$
BEGIN
   RETURN QUERY SELECT
   vm_static.* from vm_static
   inner join vm_interface_view
   on vm_static.vm_guid = vm_interface_view.vm_guid
   and network_name = v_networkName
   and vm_static.vds_group_id = v_groupId;


END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION Getvm_interfaceBytemplate_id(v_template_id UUID, v_user_id UUID, v_is_filtered boolean)
RETURNS SETOF vm_interface_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT *
   FROM vm_interface_view
   WHERE vmt_guid = v_template_id
   AND (NOT v_is_filtered OR EXISTS (SELECT 1
                                     FROM   user_vm_template_permissions_view
                                     WHERE  user_id = v_user_id AND entity_id = v_template_id));

END; $procedure$
LANGUAGE plpgsql;



Create or replace FUNCTION GetVmInterfacesByNetworkId(v_network_id UUID) RETURNS SETOF vm_interface_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT vm_interface_view.*
   FROM vm_interface_view
   INNER JOIN network_cluster
   ON network_cluster.cluster_id = vm_interface_view.vds_group_id
   INNER JOIN network
   ON network.id = network_cluster.network_id
   AND network.name = vm_interface_view.network_name
   WHERE network.id = v_network_id
   AND vm_interface_view.vm_entity_type = 'VM';
END; $procedure$
LANGUAGE plpgsql;



Create or replace FUNCTION GetVmTemplateInterfacesByNetworkId(v_network_id UUID) RETURNS SETOF vm_interface_view
   AS $procedure$
BEGIN
   RETURN QUERY SELECT vm_interface_view.*
   FROM vm_interface_view
   INNER JOIN network_cluster
   ON network_cluster.cluster_id = vm_interface_view.vds_group_id
   INNER JOIN network
   ON network.id = network_cluster.network_id
   AND network.name = vm_interface_view.network_name
   WHERE network.id = v_network_id
   AND vm_interface_view.vm_entity_type = 'TEMPLATE';
END; $procedure$
LANGUAGE plpgsql;



Create or replace FUNCTION set_network_exclusively_as_display(v_cluster_id UUID, v_network_id UUID)
RETURNS VOID
   AS $procedure$
BEGIN

   UPDATE network_cluster
   SET is_display = true
   WHERE cluster_id = v_cluster_id AND network_id = v_network_id;

   IF FOUND THEN
       UPDATE network_cluster
       SET is_display = false
       WHERE cluster_id = v_cluster_id AND network_id != v_network_id;
   END IF;

END; $procedure$
LANGUAGE plpgsql;

