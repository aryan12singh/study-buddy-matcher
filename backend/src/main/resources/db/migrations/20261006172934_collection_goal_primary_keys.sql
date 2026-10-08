-- Element collections are sets: each owner/goal pair is their natural identity.
-- Preserve valid rows and refuse legacy null/duplicate values rather than deleting them.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.student_study_goals WHERE study_goal IS NULL)
       OR EXISTS (SELECT 1 FROM public.study_group_goals WHERE study_goal IS NULL) THEN
        RAISE EXCEPTION 'Null study goals exist; resolve them before migrating';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conrelid='public.student_study_goals'::regclass AND contype='p') THEN
        ALTER TABLE public.student_study_goals ALTER COLUMN study_goal SET NOT NULL;
        ALTER TABLE public.student_study_goals DROP CONSTRAINT IF EXISTS uk85j04c9dphcf0qu7ostunxp86;
        ALTER TABLE public.student_study_goals ADD CONSTRAINT pk_student_study_goals PRIMARY KEY(student_id,study_goal);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conrelid='public.study_group_goals'::regclass AND contype='p') THEN
        ALTER TABLE public.study_group_goals ALTER COLUMN study_goal SET NOT NULL;
        ALTER TABLE public.study_group_goals DROP CONSTRAINT IF EXISTS ukp2ar7gb6wnsbtwxtma9tg2s1f;
        ALTER TABLE public.study_group_goals ADD CONSTRAINT pk_study_group_goals PRIMARY KEY(study_group_id,study_goal);
    END IF;
END $$;
