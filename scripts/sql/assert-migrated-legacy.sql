DO $$
DECLARE task_timestamp timestamptz;
        task_constraint text;
BEGIN
  IF (SELECT email FROM users WHERE id=10) <> 'legacy@example.test'
     OR (SELECT token_version FROM users WHERE id=10) <> 0
     OR (SELECT last_login_at FROM users WHERE id=10) IS NOT NULL
     OR (SELECT origin FROM match_requests WHERE id=40) <> 'PROFILE'
     OR (SELECT contact_number FROM students WHERE id=10) <> 'Synthetic legacy contact' THEN
    RAISE EXCEPTION 'Legacy upgrade did not preserve/default the expected account fields';
  END IF;
  FOR task_timestamp IN
    SELECT created_at FROM users UNION ALL
    SELECT created_at FROM match_requests UNION ALL SELECT responded_at FROM match_requests UNION ALL
    SELECT created_at FROM connections UNION ALL SELECT ended_at FROM connections UNION ALL
    SELECT created_at FROM study_groups UNION ALL SELECT joined_at FROM group_memberships UNION ALL
    SELECT created_at FROM group_join_requests UNION ALL SELECT responded_at FROM group_join_requests UNION ALL
    SELECT created_at FROM notifications
  LOOP
    IF task_timestamp <> timestamptz '2026-10-01 01:15:00+00' THEN
      RAISE EXCEPTION 'Legacy Singapore timestamp conversion changed the instant';
    END IF;
  END LOOP;
  IF NOT EXISTS(SELECT 1 FROM group_availability_slots WHERE day_of_week='TUESDAY' AND start_time='09:00' AND end_time='11:00')
     OR NOT EXISTS(SELECT 1 FROM availability_slots WHERE day_of_week='TUESDAY' AND start_time='09:00' AND end_time='11:00') THEN
    RAISE EXCEPTION 'Upgrade changed recurring campus-local availability';
  END IF;
  IF EXISTS(SELECT 1 FROM pg_tables WHERE schemaname='public' AND NOT rowsecurity)
     OR EXISTS(SELECT 1 FROM information_schema.role_table_grants WHERE table_schema='public' AND grantee IN ('anon','authenticated','PUBLIC')) THEN
    RAISE EXCEPTION 'Backend-only access did not survive the upgrade';
  END IF;
  IF (SELECT count(*) FROM pg_constraint WHERE contype='p' AND conrelid IN ('student_study_goals'::regclass,'study_group_goals'::regclass)) <> 2 THEN
    RAISE EXCEPTION 'Goal collection primary keys were not installed';
  END IF;
  IF (SELECT count(*) FROM pg_constraint WHERE conrelid='match_explanations'::regclass AND contype='u') <> 1
     OR NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='match_explanations'::regclass
       AND conname='match_explanations_evidence_hash_key' AND pg_get_constraintdef(oid)='UNIQUE (evidence_hash)')
     OR (SELECT count(*) FROM pg_index WHERE indrelid='match_explanations'::regclass AND indisunique AND NOT indisprimary) <> 1 THEN
    RAISE EXCEPTION 'Cache uniqueness did not converge to one canonical constraint and index';
  END IF;
  IF (SELECT count(*) FROM match_explanations) <> 1
     OR NOT EXISTS(SELECT 1 FROM match_explanations WHERE id=90 AND evidence_hash=repeat('a',64)
       AND prompt_version='legacy-v1' AND model='fixture-model' AND explanation_text='Retained cached wording.'
       AND created_at=timestamptz '2026-10-01 01:15+00') THEN
    RAISE EXCEPTION 'Cache migration changed the retained explanation';
  END IF;
  BEGIN
    INSERT INTO match_explanations(evidence_hash,prompt_version,model,explanation_text,created_at)
      VALUES(repeat('a',64),'legacy-v1','fixture-model','Duplicate must fail.',now());
    RAISE EXCEPTION 'Cache migration lost evidence-hash uniqueness';
  EXCEPTION WHEN unique_violation THEN
    GET STACKED DIAGNOSTICS task_constraint = CONSTRAINT_NAME;
    IF task_constraint <> 'match_explanations_evidence_hash_key' THEN
      RAISE EXCEPTION 'Duplicate cache hash rejected by an unexpected constraint: %', task_constraint;
    END IF;
  END;
END $$;
