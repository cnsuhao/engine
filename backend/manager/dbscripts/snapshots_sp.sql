

----------------------------------------------------------------
-- [snapshots] Table
--




Create or replace FUNCTION InsertSnapshot(
    v_snapshot_id UUID,
    v_vm_id UUID,
    v_snapshot_type VARCHAR(32),
    v_status VARCHAR(32),
    v_description VARCHAR(4000),
    v_creation_date TIMESTAMP WITH TIME ZONE,
    v_app_list TEXT,
    v_vm_configuration TEXT)
RETURNS VOID
AS $procedure$
BEGIN
    INSERT INTO snapshots(
        snapshot_id,
        status,
        vm_id,
        snapshot_type,
        description,
        creation_date,
        app_list,
        vm_configuration)
    VALUES(
        v_snapshot_id,
        v_status,
        v_vm_id,
        v_snapshot_type,
        v_description,
        v_creation_date,
        v_app_list,
        v_vm_configuration);
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION UpdateSnapshot(
    v_snapshot_id UUID,
    v_vm_id UUID,
    v_snapshot_type VARCHAR(32),
    v_status VARCHAR(32),
    v_description VARCHAR(4000),
    v_creation_date TIMESTAMP WITH TIME ZONE,
    v_app_list TEXT,
    v_vm_configuration TEXT)
RETURNS VOID
AS $procedure$
BEGIN
    UPDATE snapshots
    SET    status = v_status,
           vm_id = v_vm_id,
           snapshot_type = v_snapshot_type,
           description = v_description,
           creation_date = v_creation_date,
           app_list = v_app_list,
           vm_configuration = v_vm_configuration,
           _update_date = NOW()
    WHERE  snapshot_id = v_snapshot_id;
END; $procedure$
LANGUAGE plpgsql;






Create or replace FUNCTION UpdateSnapshotStatus(
    v_snapshot_id UUID,
    v_status VARCHAR(32))
RETURNS VOID
AS $procedure$
BEGIN
    UPDATE snapshots
    SET    status = v_status
    WHERE  snapshot_id = v_snapshot_id;
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION UpdateSnapshotId(
    v_snapshot_id UUID,
    v_new_snapshot_id UUID)
RETURNS VOID
AS $procedure$
BEGIN
    UPDATE snapshots
    SET    snapshot_id = v_new_snapshot_id
    WHERE  snapshot_id = v_snapshot_id;
END; $procedure$
LANGUAGE plpgsql;






Create or replace FUNCTION DeleteSnapshot(v_snapshot_id UUID)
RETURNS VOID
AS $procedure$
BEGIN
    DELETE
    FROM   snapshots
    WHERE  snapshot_id = v_snapshot_id;
END; $procedure$
LANGUAGE plpgsql;





Create or replace FUNCTION GetAllFromSnapshots() RETURNS SETOF snapshots
AS $procedure$
BEGIN
    RETURN QUERY
    SELECT *
    FROM   snapshots;
END; $procedure$
LANGUAGE plpgsql;



Create or replace FUNCTION GetSnapshotByVmIdAndType(
    v_vm_id UUID,
    v_snapshot_type VARCHAR(32))
RETURNS SETOF snapshots
AS $procedure$
BEGIN
    RETURN QUERY
    SELECT *
    FROM   snapshots
    WHERE  vm_id = v_vm_id
    AND    snapshot_type = v_snapshot_type
    ORDER BY creation_date ASC
    LIMIT 1;
END; $procedure$
LANGUAGE plpgsql;






DROP TYPE IF EXISTS GetAllFromSnapshotsByVmId_rs CASCADE;
CREATE TYPE GetAllFromSnapshotsByVmId_rs AS (snapshot_id UUID, vm_id UUID, snapshot_type VARCHAR(32), status VARCHAR(32), description VARCHAR(4000), creation_date TIMESTAMP WITH TIME ZONE, app_list TEXT, vm_configuration TEXT, vm_configuration_available BOOLEAN);
Create or replace FUNCTION GetAllFromSnapshotsByVmId(
    v_vm_id UUID,
    v_user_id UUID,
    v_is_filtered BOOLEAN,
    v_fill_configuration BOOLEAN) RETURNS SETOF GetAllFromSnapshotsByVmId_rs
AS $procedure$
BEGIN
    RETURN QUERY
    SELECT snapshot_id,
           vm_id,
           snapshot_type,
           status,
           description,
           creation_date,
           app_list,
           CASE WHEN v_fill_configuration = TRUE THEN vm_configuration ELSE NULL END,
           vm_configuration IS NOT NULL AND LENGTH(vm_configuration) > 0
    FROM   snapshots
    WHERE  vm_id = v_vm_id
    AND (NOT v_is_filtered OR EXISTS (SELECT 1
                                      FROM   user_vm_permissions_view
                                      WHERE  user_id = v_user_id AND entity_id = v_vm_id))
    ORDER BY creation_date ASC;
END; $procedure$
LANGUAGE plpgsql;




Create or replace FUNCTION GetSnapshotBySnapshotId(v_snapshot_id UUID, v_user_id UUID, v_is_filtered BOOLEAN)
RETURNS SETOF snapshots
AS $procedure$
BEGIN
    RETURN QUERY
    SELECT *
    FROM   snapshots
    WHERE  snapshot_id = v_snapshot_id AND (NOT v_is_filtered OR EXISTS (SELECT 1
                                        FROM   user_vm_permissions_view
                                        WHERE  user_id = v_user_id AND entity_id = (SELECT vm_id
                                        FROM snapshots where snapshot_id = v_snapshot_id)));
END; $procedure$
LANGUAGE plpgsql;




Create or replace FUNCTION GetSnapshotIdsByVmIdAndType(
    v_vm_id UUID,
    v_snapshot_type VARCHAR(32))
RETURNS SETOF idUuidType
AS $procedure$
BEGIN
    RETURN QUERY
    SELECT snapshot_id
    FROM   snapshots
    WHERE  vm_id = v_vm_id
    AND    snapshot_type = v_snapshot_type
    ORDER BY creation_date ASC;
END; $procedure$
LANGUAGE plpgsql;




Create or replace FUNCTION GetSnapshotIdsByVmIdAndTypeAndStatus(
    v_vm_id UUID,
    v_snapshot_type VARCHAR(32),
    v_status VARCHAR(32))
RETURNS SETOF idUuidType
AS $procedure$
BEGIN
    RETURN QUERY
    SELECT snapshot_id
    FROM   snapshots
    WHERE  vm_id = v_vm_id
    AND    snapshot_type = v_snapshot_type
    AND    status = v_status
    ORDER BY creation_date ASC;
END; $procedure$
LANGUAGE plpgsql;




Create or replace FUNCTION CheckIfSnapshotExistsByVmIdAndType(
    v_vm_id UUID,
    v_snapshot_type VARCHAR(32))
RETURNS SETOF booleanResultType
AS $procedure$
BEGIN
    RETURN QUERY
    SELECT EXISTS(
        SELECT *
        FROM   snapshots
        WHERE  vm_id = v_vm_id
        AND    snapshot_type = v_snapshot_type);
END; $procedure$
LANGUAGE plpgsql;




Create or replace FUNCTION CheckIfSnapshotExistsByVmIdAndStatus(
    v_vm_id UUID,
    v_status VARCHAR(32))
RETURNS SETOF booleanResultType
AS $procedure$
BEGIN
    RETURN QUERY
    SELECT EXISTS(
        SELECT *
        FROM   snapshots
        WHERE  vm_id = v_vm_id
        AND    status = v_status);
END; $procedure$
LANGUAGE plpgsql;




Create or replace FUNCTION CheckIfSnapshotExistsByVmIdAndSnapshotId(
    v_vm_id UUID,
    v_snapshot_id UUID)
RETURNS SETOF booleanResultType
AS $procedure$
BEGIN
    RETURN QUERY
    SELECT EXISTS(
        SELECT *
        FROM   snapshots
        WHERE  vm_id = v_vm_id
        AND    snapshot_id = v_snapshot_id);
END; $procedure$
LANGUAGE plpgsql;

