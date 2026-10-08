-- Synthetic fixtures in an empty studybuddy_migration_test database only.
INSERT INTO users(id,active,created_at,email,password_hash,role) VALUES
  (10,true,'2026-10-01 09:15:00',' Legacy@Example.Test ','fixture-only-placeholder','STUDENT'),
  (11,true,'2026-10-01 09:15:00','peer@example.test','fixture-only-placeholder','STUDENT');
INSERT INTO courses(id,code,name) VALUES (30,'IS442','Object Oriented Programming');
INSERT INTO students(id,contact_number,name,programme,school,year_of_study,target_course_id) VALUES
  (10,'Synthetic legacy contact','Legacy Student','IS','SCIS',3,30),
  (11,'Synthetic peer contact','Peer Student','IS','SCIS',3,30);
INSERT INTO student_courses(student_id,course_id) VALUES (10,30),(11,30);
INSERT INTO student_study_goals(student_id,study_goal) VALUES (10,'CONCEPT_REVIEW');
INSERT INTO availability_slots(student_id,day_of_week,start_time,end_time) VALUES (10,'TUESDAY','09:00','11:00');
INSERT INTO match_requests(id,created_at,responded_at,status,sender_id,receiver_id,message) VALUES
  (40,'2026-10-01 09:15:00','2026-10-01 09:15:00','ACCEPTED',10,11,'Legacy request');
INSERT INTO connections(id,created_at,ended_at,student_a_id,student_b_id) VALUES
  (50,'2026-10-01 09:15:00','2026-10-01 09:15:00',10,11);
INSERT INTO study_groups(id,active,created_at,description,max_group_size,name,course_id,leader_id) VALUES
  (20,true,'2026-10-01 09:15:00','Legacy group',2,'Legacy revision',30,10);
INSERT INTO study_group_goals(study_group_id,study_goal) VALUES (20,'CONCEPT_REVIEW');
INSERT INTO group_availability_slots(study_group_id,day_of_week,start_time,end_time) VALUES (20,'TUESDAY','09:00','11:00');
INSERT INTO group_memberships(joined_at,student_id,study_group_id) VALUES ('2026-10-01 09:15:00',10,20);
INSERT INTO group_join_requests(created_at,responded_at,status,student_id,study_group_id) VALUES
  ('2026-10-01 09:15:00','2026-10-01 09:15:00','REJECTED',11,20);
INSERT INTO notifications(created_at,message,read,type,recipient_id) VALUES
  ('2026-10-01 09:15:00','Legacy notification',false,'MATCH_REQUEST_ACCEPTED',10);
