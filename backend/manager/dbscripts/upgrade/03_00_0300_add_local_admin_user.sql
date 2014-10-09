-- insert local admin user to users table and assign superuser permissions
CREATE OR REPLACE FUNCTION __temp_add_admin_user()
  RETURNS void AS
$BODY$
   DECLARE
   v_user_id uuid ;
BEGIN
        v_user_id := 'fdfc627c-d875-11e0-90f0-83df133b58cc'; 
	insert into users(user_id,name,domain,username,groups,status) 
		select v_user_id, 'admin', 'internal', 'admin@internal','',1 
		where not exists (select 1 from users where user_id = v_user_id);

	insert into permissions(id,role_id,ad_element_id,object_id,object_type_id) 
		select uuid_generate_v1(), '00000000-0000-0000-0000-000000000001', v_user_id, getGlobalIds('system'), 1 
		where not exists
			(select 1 from permissions 
			 where role_id = '00000000-0000-0000-0000-000000000001' and 
                               ad_element_id = v_user_id and 
                               object_id= getGlobalIds('system') and 
                               object_type_id = 1);
END; $BODY$

LANGUAGE plpgsql;

select __temp_add_admin_user();
drop function __temp_add_admin_user();

