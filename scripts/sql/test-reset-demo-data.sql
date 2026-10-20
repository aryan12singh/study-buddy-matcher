-- Called by test-migrations.sh against its disposable, migrated legacy fixture only.
BEGIN;
DO $$ BEGIN
    IF current_database() <> 'studybuddy_migration_test' THEN
        RAISE EXCEPTION 'Demo reset tests require the disposable migration-test database';
    END IF;
END $$;

-- Existing non-demo group 20, connection 50 and their session history must survive.
INSERT INTO study_sessions(id,study_group_id,connection_id,created_by,title,start_local,end_local,
    start_at,end_at,time_zone,occurrence_count,status,created_at,updated_at) VALUES
    (92,20,NULL,NULL,'Retained group session','2026-10-20 09:00','2026-10-20 10:00',
     '2026-10-20 01:00+00','2026-10-20 02:00+00','Asia/Singapore',1,'SCHEDULED',
     '2026-10-01 01:15+00','2026-10-01 01:15+00'),
    (93,NULL,50,10,'Retained buddy session','2026-10-20 09:00','2026-10-20 10:00',
     '2026-10-20 01:00+00','2026-10-20 02:00+00','Asia/Singapore',1,'SCHEDULED',
     '2026-10-01 01:15+00','2026-10-01 01:15+00');
INSERT INTO notifications(id,created_at,message,read,type,recipient_id,resource_type,resource_id,event_key) VALUES
    (90,'2026-10-01 01:15+00','Retained group session event',true,'GROUP_SESSION_UPDATED',10,'SESSION',92,'reset:keep:group'),
    (91,'2026-10-01 01:15+00','Retained buddy session event',false,'BUDDY_SESSION_CREATED',11,'SESSION',93,'reset:keep:buddy'),
    (92,'2026-10-01 01:15+00','Retained group event',true,'GROUP_JOIN_REQUEST_ACCEPTED',11,'GROUP',20,'reset:keep:group-resource');

-- Compare every retained table row, including profile data, room state, courses and config.
CREATE TEMP TABLE reset_expected_rows(table_name text PRIMARY KEY, rows jsonb NOT NULL) ON COMMIT DROP;
DO $$
DECLARE table_name text; table_rows jsonb;
BEGIN
    FOR table_name IN SELECT tablename FROM pg_tables WHERE schemaname='public' LOOP
        EXECUTE format('SELECT COALESCE(jsonb_agg(to_jsonb(t) ORDER BY to_jsonb(t)::text), ''[]''::jsonb) FROM public.%I t', table_name)
            INTO table_rows;
        INSERT INTO reset_expected_rows VALUES (table_name, table_rows);
    END LOOP;
END $$;

INSERT INTO users(id,active,created_at,email,password_hash,role) VALUES
    (100,true,'2026-10-01 01:15+00','mixed@demo.example.test','fixture-only-placeholder','STUDENT'),
    (101,true,'2026-10-01 01:15+00','admin@demo.example.test','fixture-only-placeholder','ADMIN');
INSERT INTO students(id,contact_number,name,programme,school,year_of_study,target_course_id) VALUES
    (100,'Synthetic demo contact','Demo Student','IS','SCIS',3,30);
INSERT INTO student_courses(student_id,course_id) VALUES (100,30);
INSERT INTO student_study_goals(student_id,study_goal) VALUES (100,'CONCEPT_REVIEW');
INSERT INTO availability_slots(id,student_id,day_of_week,start_time,end_time) VALUES (100,100,'TUESDAY','09:00','11:00');
INSERT INTO study_groups(id,active,created_at,description,max_group_size,name,course_id,leader_id) VALUES
    (100,true,'2026-10-01 01:15+00','Mixed demo group',2,'Demo revision',30,100);
INSERT INTO group_memberships(id,joined_at,student_id,study_group_id) VALUES
    (100,'2026-10-01 01:15+00',100,100), (101,'2026-10-01 01:15+00',10,100),
    (102,'2026-10-01 01:15+00',100,20);
INSERT INTO group_join_requests(id,created_at,status,student_id,study_group_id) VALUES
    (100,'2026-10-01 01:15+00','PENDING',11,100), (101,'2026-10-01 01:15+00','PENDING',100,20);
INSERT INTO study_group_goals(study_group_id,study_goal) VALUES (100,'CONCEPT_REVIEW');
INSERT INTO group_availability_slots(id,study_group_id,day_of_week,start_time,end_time) VALUES (100,100,'TUESDAY','09:00','11:00');
INSERT INTO match_requests(id,created_at,status,sender_id,receiver_id) VALUES (100,'2026-10-01 01:15+00','PENDING',100,11);
INSERT INTO connections(id,created_at,student_a_id,student_b_id) VALUES
    (100,'2026-10-01 01:15+00',100,10), (101,'2026-10-01 01:15+00',11,100);

-- Cover demo membership in a retained group, both buddy orientations and a retained
-- session created by the demo student. Only the latter's creator reference becomes null.
UPDATE study_sessions SET created_by=100 WHERE id=92;
INSERT INTO study_sessions(id,study_group_id,connection_id,created_by,title,start_local,end_local,
    start_at,end_at,time_zone,occurrence_count,status,created_at,updated_at) VALUES
    (20,100,NULL,10,'Mixed group session','2026-10-20 09:00','2026-10-20 10:00',
     '2026-10-20 01:00+00','2026-10-20 02:00+00','Asia/Singapore',1,'SCHEDULED',
     '2026-10-01 01:15+00','2026-10-01 01:15+00'),
    (100,NULL,100,10,'Mixed buddy session A','2026-10-20 09:00','2026-10-20 10:00',
     '2026-10-20 01:00+00','2026-10-20 02:00+00','Asia/Singapore',1,'SCHEDULED',
     '2026-10-01 01:15+00','2026-10-01 01:15+00'),
    (101,NULL,101,11,'Mixed buddy session B','2026-10-20 09:00','2026-10-20 10:00',
     '2026-10-20 01:00+00','2026-10-20 02:00+00','Asia/Singapore',1,'SCHEDULED',
     '2026-10-01 01:15+00','2026-10-01 01:15+00');
INSERT INTO notifications(id,created_at,message,read,type,recipient_id,resource_type,resource_id,event_key) VALUES
    (100,'2026-10-01 01:15+00','Mixed group session event',false,'GROUP_SESSION_CREATED',10,'SESSION',20,'reset:remove:group-session'),
    (101,'2026-10-01 01:15+00','Mixed buddy session A event',true,'BUDDY_SESSION_CREATED',10,'SESSION',100,'reset:remove:buddy-a'),
    (102,'2026-10-01 01:15+00','Mixed buddy session B event',false,'BUDDY_SESSION_UPDATED',11,'SESSION',101,'reset:remove:buddy-b'),
    (103,'2026-10-01 01:15+00','Demo recipient event',false,'GROUP_SESSION_CREATED',100,'SESSION',92,'reset:remove:recipient'),
    (104,'2026-10-01 01:15+00','Demo student event',false,'CONNECTION_ENDED',10,'STUDENT',100,'reset:remove:student'),
    (105,'2026-10-01 01:15+00','Demo group event',false,'GROUP_JOIN_REQUEST_ACCEPTED',10,'GROUP',100,'reset:remove:group'),
    (106,'2026-10-01 01:15+00','Demo request event',false,'MATCH_REQUEST_RECEIVED',11,'MATCH_REQUEST',100,'reset:remove:request');

\ir reset-demo-data.sql
DO $$ BEGIN
    IF EXISTS(SELECT 1 FROM notifications WHERE resource_type='SESSION' AND resource_id IN (20,100,101)) THEN
        RAISE EXCEPTION 'Demo reset left group or buddy session notifications for non-demo recipients';
    END IF;
    IF EXISTS(SELECT 1 FROM users WHERE email LIKE '%@demo.example.test')
       OR EXISTS(SELECT 1 FROM study_sessions WHERE id IN (20,100,101)) THEN
        RAISE EXCEPTION 'Demo reset left demo accounts or their sessions';
    END IF;
END $$;

-- Repeating a reset without demo accounts must also preserve the same rows.
\ir reset-demo-data.sql
DO $$
DECLARE expected record; actual_rows jsonb;
BEGIN
    FOR expected IN SELECT * FROM reset_expected_rows LOOP
        EXECUTE format('SELECT COALESCE(jsonb_agg(to_jsonb(t) ORDER BY to_jsonb(t)::text), ''[]''::jsonb) FROM public.%I t', expected.table_name)
            INTO actual_rows;
        IF actual_rows <> expected.rows THEN
            RAISE EXCEPTION 'Demo reset changed retained rows or left demo data in %', expected.table_name;
        END IF;
    END LOOP;
END $$;
ROLLBACK;
