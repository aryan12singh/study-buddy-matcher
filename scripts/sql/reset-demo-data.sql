-- Removes every @demo.example.test account and everything that depends on it, so the opt-in
-- demo seed can rebuild the demo story from scratch. Courses, matching configuration and every
-- non-demo account are kept. A non-demo account loses only its own links to demo accounts:
-- requests and connections with them, and memberships or applications in groups they led.
--
-- Run it once, as a reviewed operation, in the Supabase SQL editor or with
--   psql --set=ON_ERROR_STOP=1 --file=scripts/sql/reset-demo-data.sql
-- It runs as one transaction: any error rolls everything back. Stop the backend first, or rely
-- on the advisory lock below, which is the same one the application takes for account changes.

BEGIN;

SELECT pg_advisory_xact_lock(4422026);

CREATE TEMP TABLE demo_students ON COMMIT DROP AS
    SELECT s.id FROM students s JOIN users u ON u.id = s.id
    WHERE u.email LIKE '%@demo.example.test';

CREATE TEMP TABLE demo_groups ON COMMIT DROP AS
    SELECT id FROM study_groups WHERE leader_id IN (SELECT id FROM demo_students);

CREATE TEMP TABLE demo_requests ON COMMIT DROP AS
    SELECT id FROM match_requests
    WHERE sender_id IN (SELECT id FROM demo_students) OR receiver_id IN (SELECT id FROM demo_students);

-- Notifications sent to demo students, or pointing at something about to be removed.
DELETE FROM notifications
WHERE recipient_id IN (SELECT id FROM demo_students)
   OR (resource_type = 'STUDENT' AND resource_id IN (SELECT id FROM demo_students))
   OR (resource_type = 'GROUP' AND resource_id IN (SELECT id FROM demo_groups))
   OR (resource_type = 'MATCH_REQUEST' AND resource_id IN (SELECT id FROM demo_requests));

-- Groups led by demo students, with everything inside them; then demo students' places in other groups.
DELETE FROM group_join_requests
WHERE study_group_id IN (SELECT id FROM demo_groups) OR student_id IN (SELECT id FROM demo_students);
DELETE FROM group_memberships
WHERE study_group_id IN (SELECT id FROM demo_groups) OR student_id IN (SELECT id FROM demo_students);
DELETE FROM group_availability_slots WHERE study_group_id IN (SELECT id FROM demo_groups);
DELETE FROM study_group_goals WHERE study_group_id IN (SELECT id FROM demo_groups);
DELETE FROM study_groups WHERE id IN (SELECT id FROM demo_groups);

-- Requests and connections involving a demo student.
DELETE FROM match_requests WHERE id IN (SELECT id FROM demo_requests);
DELETE FROM connections
WHERE student_a_id IN (SELECT id FROM demo_students) OR student_b_id IN (SELECT id FROM demo_students);

-- The demo profiles themselves, then every demo login (students and the demo admin).
DELETE FROM availability_slots WHERE student_id IN (SELECT id FROM demo_students);
DELETE FROM student_courses WHERE student_id IN (SELECT id FROM demo_students);
DELETE FROM student_study_goals WHERE student_id IN (SELECT id FROM demo_students);
DELETE FROM students WHERE id IN (SELECT id FROM demo_students);
DELETE FROM users WHERE email LIKE '%@demo.example.test';

SELECT count(*) AS demo_accounts_remaining FROM users WHERE email LIKE '%@demo.example.test';

COMMIT;
