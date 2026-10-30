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

-- One row in every later table for the demo student, the group they lead and its session: a
-- study room with presence and a reaction, a planted tree, an earned badge, a group quiz for the
-- session with a retained member's attempt, an open personal quiz, and a personal and a session
-- study plan. None may outlive the reset.
INSERT INTO study_rooms(id,study_group_id,host_id,version,focus_minutes,break_minutes,participant_limit,
    phase,timer_status,remaining_millis,audio_preset,audio_playing,music_preset,music_playing,ambient_preset,ambient_playing) VALUES
    (100,100,100,0,25,5,4,'FOCUS','IDLE',1500000,'CALM_MUSIC',false,NULL,false,NULL,false);
INSERT INTO room_presences(id,study_room_id,student_id,client_id,expires_at,observed_at,presence) VALUES
    (100,100,100,'00000000-0000-0000-0000-000000000100','2026-10-01 01:16+00','2026-10-01 01:15+00','FOCUS');
INSERT INTO room_reactions(id,study_room_id,student_id,type,sent_at) VALUES (100,100,100,'CLAP','2026-10-01 01:15+00');
INSERT INTO focus_visits(id,student_id,study_group_id,group_name,started_at,last_seen_at,focused_millis,variant,plot,ended_at) VALUES
    (100,100,100,'Demo revision','2026-10-01 01:00+00','2026-10-01 02:00+00',3600000,1,100,'2026-10-01 02:00+00');
INSERT INTO earned_badges(id,student_id,badge,best_tier) VALUES (100,100,'FOUNDER','BRONZE');
INSERT INTO quizzes(id,scope,group_id,student_id,session_id,created_by,course_id,title,status,created_at,closes_at,closed_at) VALUES
    (100,'GROUP',100,NULL,20,100,30,'Demo group quiz','CLOSED','2026-10-01 01:00+00','2026-10-01 01:15+00',NULL),
    (101,'PERSONAL',NULL,100,NULL,100,30,'Demo practice quiz','OPEN','2026-10-01 01:00+00',NULL,NULL);
INSERT INTO quiz_questions(id,quiz_id,position,prompt,correct_option,explanation) VALUES
    (100,100,0,'Demo group question',0,'Demo explanation'),
    (101,101,0,'Demo practice question',1,'Demo explanation');
INSERT INTO quiz_question_options(question_id,position,text) VALUES
    (100,0,'A'),(100,1,'B'),(100,2,'C'),(100,3,'D'),(101,0,'A'),(101,1,'B'),(101,2,'C'),(101,3,'D');
INSERT INTO quiz_attempts(id,quiz_id,student_id,started_at,submitted_at,score,total) VALUES
    (100,100,100,'2026-10-01 01:01+00','2026-10-01 01:05+00',1,1),
    (101,100,10,'2026-10-01 01:01+00','2026-10-01 01:06+00',0,1);
INSERT INTO quiz_attempt_answers(attempt_id,question_id,chosen_option) VALUES (100,100,0),(101,100,2);
INSERT INTO study_plans(id,scope,student_id,session_id,created_by,course_id,first_block_date,block_count,minutes_per_block,
    exam_date,study_goal,source_kind,created_at,updated_at) VALUES
    (100,'PERSONAL',100,NULL,100,30,'2026-10-01',1,120,NULL,'CONCEPT_REVIEW','PASTE','2026-10-01 01:15+00','2026-10-01 01:15+00'),
    (101,'SESSION',NULL,20,100,30,'2026-10-20',1,60,NULL,NULL,'PASTE','2026-10-01 01:15+00','2026-10-01 01:15+00');
INSERT INTO study_plan_source_topics(plan_id,position,title) VALUES (100,0,'Demo topic'),(101,0,'Demo topic');
INSERT INTO study_plan_entries(id,plan_id,sequence,scheduled_date,position,title,planned_minutes,completed) VALUES
    (100,100,1,'2026-10-01',0,'Demo personal entry',60,true),
    (101,101,1,'2026-10-20',0,'Demo meeting entry',60,false);
INSERT INTO study_plan_entry_tasks(entry_id,position,description) VALUES (100,0,'Demo task'),(101,0,'Demo task');

\ir reset-demo-data.sql
DO $$ BEGIN
    IF EXISTS(SELECT 1 FROM study_rooms WHERE id=100) OR EXISTS(SELECT 1 FROM room_presences WHERE id=100)
       OR EXISTS(SELECT 1 FROM room_reactions WHERE id=100) OR EXISTS(SELECT 1 FROM focus_visits WHERE id=100)
       OR EXISTS(SELECT 1 FROM earned_badges WHERE id=100)
       OR EXISTS(SELECT 1 FROM quizzes WHERE id IN (100,101)) OR EXISTS(SELECT 1 FROM quiz_questions WHERE id IN (100,101))
       OR EXISTS(SELECT 1 FROM quiz_question_options WHERE question_id IN (100,101))
       OR EXISTS(SELECT 1 FROM quiz_attempts WHERE id IN (100,101))
       OR EXISTS(SELECT 1 FROM quiz_attempt_answers WHERE attempt_id IN (100,101))
       OR EXISTS(SELECT 1 FROM study_plans WHERE id IN (100,101))
       OR EXISTS(SELECT 1 FROM study_plan_source_topics WHERE plan_id IN (100,101))
       OR EXISTS(SELECT 1 FROM study_plan_entries WHERE id IN (100,101))
       OR EXISTS(SELECT 1 FROM study_plan_entry_tasks WHERE entry_id IN (100,101)) THEN
        RAISE EXCEPTION 'Demo reset left study room, forest, badge, quiz or study plan rows';
    END IF;
END $$;
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
