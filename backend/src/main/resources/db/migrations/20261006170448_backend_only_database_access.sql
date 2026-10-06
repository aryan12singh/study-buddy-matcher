-- Browser clients use the Spring API, never PostgREST or direct database roles.
-- Backend table owners retain JDBC access. Do not FORCE RLS on the backend owner.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='anon') THEN CREATE ROLE anon NOLOGIN; END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='authenticated') THEN CREATE ROLE authenticated NOLOGIN; END IF;
END $$;

DO $$
DECLARE
    application_table record;
    policy_row record;
    owner_row record;
BEGIN
    FOR application_table IN SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename IN (
        'users','students','courses','student_courses','student_study_goals','availability_slots',
        'match_requests','connections','study_groups','study_group_goals','group_availability_slots',
        'group_memberships','group_join_requests','notifications','matching_configs','matching_strategy_settings')
    LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY',application_table.tablename);
        EXECUTE format('REVOKE ALL ON TABLE public.%I FROM PUBLIC,anon,authenticated',application_table.tablename);
        -- Existing allow policies would conflict with the agreed backend-only architecture.
        FOR policy_row IN SELECT policyname FROM pg_policies
            WHERE schemaname='public' AND tablename=application_table.tablename
        LOOP
            EXECUTE format('DROP POLICY %I ON public.%I',policy_row.policyname,application_table.tablename);
        END LOOP;
    END LOOP;
    FOR owner_row IN SELECT DISTINCT tableowner FROM pg_tables
        WHERE schemaname='public' AND tablename='users'
    LOOP
        EXECUTE format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public REVOKE ALL ON TABLES FROM PUBLIC,anon,authenticated',owner_row.tableowner);
        EXECUTE format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public REVOKE ALL ON SEQUENCES FROM PUBLIC,anon,authenticated',owner_row.tableowner);
        EXECUTE format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC,anon,authenticated',owner_row.tableowner);
    END LOOP;
END $$;

REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM PUBLIC,anon,authenticated;
