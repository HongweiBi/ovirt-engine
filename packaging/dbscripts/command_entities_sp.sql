CREATE OR REPLACE FUNCTION InsertCommandEntity (v_command_id uuid,
       v_command_type int,
       v_root_command_id uuid,
       v_action_parameters text,
       v_action_parameters_class varchar(256),
       v_status varchar(20))
RETURNS VOID
   AS $procedure$
BEGIN
       INSERT INTO command_entities(command_id, command_type, root_command_id, action_parameters, action_parameters_class, created_at, status)
              VALUES(v_command_id, v_command_type, v_root_command_id, v_action_parameters, v_action_parameters_class, NOW(), v_status);
END; $procedure$
LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION UpdateCommandEntity (v_command_id uuid,
       v_command_type int,
       v_root_command_id uuid,
       v_action_parameters text,
       v_action_parameters_class varchar(256),
       v_status varchar(20))
RETURNS VOID
   AS $procedure$
BEGIN
      UPDATE command_entities
      SET command_type = v_command_type ,
          root_command_id = v_root_command_id,
          action_parameters = v_action_parameters,
          action_parameters_class = v_action_parameters_class,
          status = v_status
      WHERE command_id = v_command_id;
END; $procedure$
LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION UpdateCommandEntityStatus (v_command_id uuid,
       v_status varchar(20))
RETURNS VOID
   AS $procedure$
BEGIN
      UPDATE command_entities
      SET status = v_status
      WHERE command_id = v_command_id;
END; $procedure$
LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION InsertOrUpdateCommandEntity (v_command_id uuid,
       v_command_type int,
       v_root_command_id uuid,
       v_action_parameters text,
       v_action_parameters_class varchar(256),
       v_status varchar(20))
RETURNS VOID
   AS $procedure$
BEGIN
      IF NOT EXISTS (SELECT 1 from command_entities where command_id = v_command_id) THEN
            PERFORM InsertCommandEntity (v_command_id, v_command_type, v_root_command_id, v_action_parameters, v_action_parameters_class, v_status);
      ELSE
            PERFORM UpdateCommandEntity (v_command_id, v_command_type, v_root_command_id, v_action_parameters, v_action_parameters_class, v_status);
      END IF;
END; $procedure$
LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION GetCommandEntityByCommandEntityId (v_command_id uuid)
RETURNS SETOF command_entities
   AS $procedure$
BEGIN
      RETURN QUERY SELECT command_entities.*
      FROM command_entities
      WHERE command_id = v_command_id;
END; $procedure$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION GetAllFromCommandEntities ()
RETURNS SETOF command_entities
   AS $procedure$
BEGIN
      RETURN QUERY SELECT * from command_entities;
END; $procedure$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION DeleteCommandEntity(v_command_id uuid)
RETURNS VOID
   AS $procedure$
BEGIN
      BEGIN
              delete from command_entities where command_id = v_command_id;
      END;
      RETURN;
END; $procedure$
LANGUAGE plpgsql;

Create or replace FUNCTION DeleteCommandEntitiesOlderThanDate(v_date TIMESTAMP WITH TIME ZONE)
RETURNS VOID
   AS $procedure$
   DECLARE
   v_id  INTEGER;
   SWV_RowCount INTEGER;
BEGIN
      DELETE FROM command_entities
      WHERE CREATED_AT < v_date and
      command_id NOT IN(SELECT command_id FROM async_tasks);
END; $procedure$
LANGUAGE plpgsql;