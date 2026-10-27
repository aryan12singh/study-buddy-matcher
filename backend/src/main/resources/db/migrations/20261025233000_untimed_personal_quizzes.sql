-- A personal practice quiz has no time limit: it stays open until its student submits it, so it
-- has no closing time. A group quiz still always closes after its open window. Existing personal
-- quizzes lose their closing time, which reopens any that ran out of time unsubmitted.
-- Repeatable: constraints are dropped before being re-added.
ALTER TABLE public.quizzes ALTER COLUMN closes_at DROP NOT NULL;
ALTER TABLE public.quizzes DROP CONSTRAINT IF EXISTS quizzes_closes_after_created_check;
ALTER TABLE public.quizzes ADD CONSTRAINT quizzes_closes_after_created_check
    CHECK (closes_at IS NULL OR closes_at > created_at);
ALTER TABLE public.quizzes DROP CONSTRAINT IF EXISTS quizzes_group_closing_time_check;
ALTER TABLE public.quizzes ADD CONSTRAINT quizzes_group_closing_time_check
    CHECK (scope = 'PERSONAL' OR closes_at IS NOT NULL);
UPDATE public.quizzes SET closes_at = NULL WHERE scope = 'PERSONAL' AND closes_at IS NOT NULL;
