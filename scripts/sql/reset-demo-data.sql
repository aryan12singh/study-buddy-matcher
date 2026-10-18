-- Removes every @demo.example.test account and everything that depends on it, so the opt-in
-- demo seed can rebuild the demo story from scratch. Courses, matching configuration and every
-- non-demo account are kept. A non-demo account loses only its own links to demo accounts:
-- requests and connections with them, memberships or applications in groups they led,
-- and sessions and notifications belonging to those removed groups or connections.
--
-- Run the whole file once, as a reviewed operation, in the Supabase SQL editor or with
--   psql --set=ON_ERROR_STOP=1 --file=scripts/sql/reset-demo-data.sql
-- The work is a single DO statement, so it is atomic however the client sends it: any error
-- changes nothing. (An earlier version used temporary tables across several statements, which
-- fails with "demo_students does not exist" when a client runs each statement separately.)
-- Stop the backend first, or rely on the advisory lock below, which is the same one the
-- application takes for account changes.

DO $$
DECLARE
    demo_students bigint[];
    demo_groups bigint[];
    demo_requests bigint[];
    demo_connections bigint[];
    demo_sessions bigint[];
BEGIN
    PERFORM pg_advisory_xact_lock(4422026);

    demo_students := ARRAY(
        SELECT s.id FROM students s JOIN users u ON u.id = s.id
        WHERE u.email LIKE '%@demo.example.test');
    demo_groups := ARRAY(
        SELECT id FROM study_groups WHERE leader_id = ANY (demo_students));
    demo_requests := ARRAY(
        SELECT id FROM match_requests
        WHERE sender_id = ANY (demo_students) OR receiver_id = ANY (demo_students));
    demo_connections := ARRAY(
        SELECT id FROM connections
        WHERE student_a_id = ANY (demo_students) OR student_b_id = ANY (demo_students));
    -- Keep the session IDs before deleting their parents cascades to the sessions.
    demo_sessions := ARRAY(
        SELECT id FROM study_sessions
        WHERE study_group_id = ANY (demo_groups) OR connection_id = ANY (demo_connections));

    -- Notifications sent to demo students, or pointing at something about to be removed.
    DELETE FROM notifications
    WHERE recipient_id = ANY (demo_students)
       OR (resource_type = 'STUDENT' AND resource_id = ANY (demo_students))
       OR (resource_type = 'GROUP' AND resource_id = ANY (demo_groups))
       OR (resource_type = 'MATCH_REQUEST' AND resource_id = ANY (demo_requests))
       OR (resource_type = 'SESSION' AND resource_id = ANY (demo_sessions));

    -- Groups led by demo students, with everything inside them; then demo students' places in other groups.
    DELETE FROM group_join_requests
    WHERE study_group_id = ANY (demo_groups) OR student_id = ANY (demo_students);
    DELETE FROM group_memberships
    WHERE study_group_id = ANY (demo_groups) OR student_id = ANY (demo_students);
    DELETE FROM group_availability_slots WHERE study_group_id = ANY (demo_groups);
    DELETE FROM study_group_goals WHERE study_group_id = ANY (demo_groups);
    DELETE FROM study_groups WHERE id = ANY (demo_groups);

    -- Requests and connections involving a demo student.
    DELETE FROM match_requests WHERE id = ANY (demo_requests);
    DELETE FROM connections WHERE id = ANY (demo_connections);

    -- The demo profiles themselves, then every demo login (students and the demo admin).
    DELETE FROM availability_slots WHERE student_id = ANY (demo_students);
    DELETE FROM student_courses WHERE student_id = ANY (demo_students);
    DELETE FROM student_study_goals WHERE student_id = ANY (demo_students);
    DELETE FROM students WHERE id = ANY (demo_students);
    DELETE FROM users WHERE email LIKE '%@demo.example.test';
END
$$;

SELECT count(*) AS demo_accounts_remaining FROM users WHERE email LIKE '%@demo.example.test';
