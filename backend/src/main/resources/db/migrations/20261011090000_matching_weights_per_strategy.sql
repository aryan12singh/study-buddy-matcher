-- Each matching strategy keeps its own criterion weights, so a weight row now belongs to a
-- strategy as well as a criterion. Rows saved before this belonged to the single shared set
-- and become the Balanced strategy's weights. Repeatable: safe to run more than once.
ALTER TABLE public.matching_configs ADD COLUMN IF NOT EXISTS strategy varchar(20);
UPDATE public.matching_configs SET strategy = 'BALANCED' WHERE strategy IS NULL;
ALTER TABLE public.matching_configs ALTER COLUMN strategy SET NOT NULL;

ALTER TABLE public.matching_configs DROP CONSTRAINT IF EXISTS matching_configs_strategy_check;
ALTER TABLE public.matching_configs ADD CONSTRAINT matching_configs_strategy_check CHECK (strategy IN (
    'BALANCED','AVAILABILITY_FIRST','COURSE_FIRST'));

-- One weight per criterion per strategy, replacing one weight per criterion overall.
ALTER TABLE public.matching_configs DROP CONSTRAINT IF EXISTS uk2qjp2v8hbvpiltp7w32a7o9b1;
ALTER TABLE public.matching_configs DROP CONSTRAINT IF EXISTS matching_configs_strategy_criterion_key;
ALTER TABLE public.matching_configs ADD CONSTRAINT matching_configs_strategy_criterion_key UNIQUE (strategy, criterion);
