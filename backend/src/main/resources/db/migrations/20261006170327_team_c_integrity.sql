-- Abort on incompatible existing data; never delete rows to install a constraint.
DO $$
BEGIN
    IF EXISTS (SELECT lower(btrim(email)) FROM public.users
               GROUP BY lower(btrim(email)) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Duplicate normalised emails exist; resolve them before migrating';
    END IF;
    IF EXISTS (SELECT least(sender_id,receiver_id), greatest(sender_id,receiver_id)
               FROM public.match_requests WHERE status='PENDING'
               GROUP BY least(sender_id,receiver_id),greatest(sender_id,receiver_id)
               HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Duplicate pending buddy requests exist; resolve them before migrating';
    END IF;
    IF EXISTS (SELECT least(student_a_id,student_b_id), greatest(student_a_id,student_b_id)
               FROM public.connections WHERE ended_at IS NULL
               GROUP BY least(student_a_id,student_b_id),greatest(student_a_id,student_b_id)
               HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Duplicate active connections exist; resolve them before migrating';
    END IF;
    IF EXISTS (SELECT study_group_id,student_id FROM public.group_join_requests
               WHERE status='PENDING' GROUP BY study_group_id,student_id HAVING count(*)>1) THEN
        RAISE EXCEPTION 'Duplicate pending group requests exist; resolve them before migrating';
    END IF;
    IF EXISTS (SELECT 1 FROM public.match_requests WHERE sender_id=receiver_id)
       OR EXISTS (SELECT 1 FROM public.connections WHERE student_a_id=student_b_id)
       OR EXISTS (SELECT 1 FROM public.students WHERE year_of_study<1)
       OR EXISTS (SELECT 1 FROM public.study_groups WHERE max_group_size<2)
       OR EXISTS (SELECT 1 FROM public.availability_slots WHERE start_time>=end_time
                  OR extract(second FROM start_time)<>0 OR extract(second FROM end_time)<>0)
       OR EXISTS (SELECT 1 FROM public.group_availability_slots WHERE start_time>=end_time
                  OR extract(second FROM start_time)<>0 OR extract(second FROM end_time)<>0) THEN
        RAISE EXCEPTION 'Invalid existing domain data prevents migration';
    END IF;
    IF EXISTS (SELECT 1 FROM public.study_groups g JOIN public.group_memberships m
               ON m.study_group_id=g.id GROUP BY g.id,g.max_group_size
               HAVING count(*)>g.max_group_size) THEN
        RAISE EXCEPTION 'Existing group membership exceeds capacity; resolve it before migrating';
    END IF;
END $$;

UPDATE public.users SET email=lower(btrim(email)) WHERE email<>lower(btrim(email));
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS token_version bigint NOT NULL DEFAULT 0;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS last_login_at timestamptz;
ALTER TABLE public.match_requests ADD COLUMN IF NOT EXISTS origin varchar(20) NOT NULL DEFAULT 'PROFILE';
ALTER TABLE public.match_requests ADD COLUMN IF NOT EXISTS context_course_id bigint;
ALTER TABLE public.match_requests ADD COLUMN IF NOT EXISTS context_study_goal varchar(30);
ALTER TABLE public.notifications ALTER COLUMN message TYPE text;
ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS resource_type varchar(30);
ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS resource_id bigint;
ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS event_key varchar(160);
ALTER TABLE public.study_groups ALTER COLUMN description TYPE varchar(4000);

-- Legacy LocalDateTime values were written in campus local time. Only convert once.
DO $$
DECLARE
    timestamp_column record;
BEGIN
    FOR timestamp_column IN
        SELECT c.table_name,c.column_name FROM information_schema.columns c
        WHERE c.table_schema='public' AND c.data_type='timestamp without time zone'
        AND (c.table_name,c.column_name) IN (
            ('users','created_at'),('match_requests','created_at'),('match_requests','responded_at'),
            ('connections','created_at'),('connections','ended_at'),('study_groups','created_at'),
            ('group_memberships','joined_at'),('group_join_requests','created_at'),
            ('group_join_requests','responded_at'),('notifications','created_at'))
    LOOP
        EXECUTE format('ALTER TABLE public.%I ALTER COLUMN %I TYPE timestamptz USING %I AT TIME ZONE %L',
            timestamp_column.table_name,timestamp_column.column_name,
            timestamp_column.column_name,'Asia/Singapore');
    END LOOP;
END $$;

-- Constraints are named deliberately and installed idempotently for existing databases.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_match_request_context_course'
                   AND conrelid='public.match_requests'::regclass) THEN
        ALTER TABLE public.match_requests ADD CONSTRAINT fk_match_request_context_course
            FOREIGN KEY (context_course_id) REFERENCES public.courses(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_match_request_different_students'
                   AND conrelid='public.match_requests'::regclass) THEN
        ALTER TABLE public.match_requests ADD CONSTRAINT ck_match_request_different_students CHECK(sender_id<>receiver_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_connection_different_students'
                   AND conrelid='public.connections'::regclass) THEN
        ALTER TABLE public.connections ADD CONSTRAINT ck_connection_different_students CHECK(student_a_id<>student_b_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_student_year'
                   AND conrelid='public.students'::regclass) THEN
        ALTER TABLE public.students ADD CONSTRAINT ck_student_year CHECK(year_of_study>=1);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_group_minimum_capacity'
                   AND conrelid='public.study_groups'::regclass) THEN
        ALTER TABLE public.study_groups ADD CONSTRAINT ck_group_minimum_capacity CHECK(max_group_size>=2);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_student_slot_order'
                   AND conrelid='public.availability_slots'::regclass) THEN
        ALTER TABLE public.availability_slots ADD CONSTRAINT ck_student_slot_order CHECK(start_time<end_time);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_group_slot_order'
                   AND conrelid='public.group_availability_slots'::regclass) THEN
        ALTER TABLE public.group_availability_slots ADD CONSTRAINT ck_group_slot_order CHECK(start_time<end_time);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_student_slot_whole_minutes'
                   AND conrelid='public.availability_slots'::regclass) THEN
        ALTER TABLE public.availability_slots ADD CONSTRAINT ck_student_slot_whole_minutes
            CHECK(extract(second FROM start_time)=0 AND extract(second FROM end_time)=0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_group_slot_whole_minutes'
                   AND conrelid='public.group_availability_slots'::regclass) THEN
        ALTER TABLE public.group_availability_slots ADD CONSTRAINT ck_group_slot_whole_minutes
            CHECK(extract(second FROM start_time)=0 AND extract(second FROM end_time)=0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_match_request_origin'
                   AND conrelid='public.match_requests'::regclass) THEN
        ALTER TABLE public.match_requests ADD CONSTRAINT ck_match_request_origin CHECK(origin IN ('PROFILE','MATCHING'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ck_match_request_context_goal'
                   AND conrelid='public.match_requests'::regclass) THEN
        ALTER TABLE public.match_requests ADD CONSTRAINT ck_match_request_context_goal
            CHECK(context_study_goal IN ('CONCEPT_REVIEW','PROBLEM_SOLVING','EXAM_PREPARATION','PROJECT_DISCUSSION'));
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_normalised_email ON public.users(lower(email));
CREATE UNIQUE INDEX IF NOT EXISTS ux_match_request_pending_pair ON public.match_requests
    (least(sender_id,receiver_id),greatest(sender_id,receiver_id)) WHERE status='PENDING';
CREATE UNIQUE INDEX IF NOT EXISTS ux_connection_active_pair ON public.connections
    (least(student_a_id,student_b_id),greatest(student_a_id,student_b_id)) WHERE ended_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_group_request_pending_applicant ON public.group_join_requests
    (study_group_id,student_id) WHERE status='PENDING';
CREATE UNIQUE INDEX IF NOT EXISTS ux_notification_recipient_event ON public.notifications
    (recipient_id,event_key) WHERE event_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_match_request_receiver_created ON public.match_requests(receiver_id,created_at DESC);
CREATE INDEX IF NOT EXISTS ix_match_request_sender_created ON public.match_requests(sender_id,created_at DESC);
CREATE INDEX IF NOT EXISTS ix_connection_student_a ON public.connections(student_a_id);
CREATE INDEX IF NOT EXISTS ix_connection_student_b ON public.connections(student_b_id);
CREATE INDEX IF NOT EXISTS ix_group_membership_student ON public.group_memberships(student_id);
CREATE INDEX IF NOT EXISTS ix_group_request_student ON public.group_join_requests(student_id);
CREATE INDEX IF NOT EXISTS ix_notification_recipient_created ON public.notifications(recipient_id,created_at DESC);
CREATE INDEX IF NOT EXISTS ix_notification_unread ON public.notifications(recipient_id) WHERE NOT read;
CREATE INDEX IF NOT EXISTS ix_group_course ON public.study_groups(course_id);
CREATE INDEX IF NOT EXISTS ix_group_leader ON public.study_groups(leader_id);
CREATE INDEX IF NOT EXISTS ix_student_target_course ON public.students(target_course_id);
CREATE INDEX IF NOT EXISTS ix_student_course_course ON public.student_courses(course_id);
CREATE INDEX IF NOT EXISTS ix_student_availability_student ON public.availability_slots(student_id);
CREATE INDEX IF NOT EXISTS ix_group_availability_group ON public.group_availability_slots(study_group_id);
CREATE INDEX IF NOT EXISTS ix_match_request_context_course ON public.match_requests(context_course_id);

-- Closure signals extend the original enum without removing history.
ALTER TABLE public.notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE public.notifications ADD CONSTRAINT notifications_type_check CHECK(type IN (
    'MATCH_REQUEST_RECEIVED','MATCH_REQUEST_ACCEPTED','MATCH_REQUEST_DECLINED','CONNECTION_ENDED',
    'GROUP_JOIN_REQUEST_RECEIVED','GROUP_JOIN_REQUEST_ACCEPTED','GROUP_JOIN_REQUEST_REJECTED',
    'GROUP_MEMBER_REMOVED','GROUP_CLOSED'));
