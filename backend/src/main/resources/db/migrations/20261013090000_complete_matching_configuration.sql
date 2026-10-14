-- One settings row serializes all administrators' saves and versions scoring snapshots.
-- Existing overrides retain their strategy/weights; fresh databases use external startup defaults.
DO $$
BEGIN
    IF (SELECT count(*) FROM public.matching_strategy_settings) > 1 THEN
        RAISE EXCEPTION 'Matching settings contain multiple rows; resolve them before migration';
    END IF;
END $$;

ALTER TABLE public.matching_strategy_settings ADD COLUMN IF NOT EXISTS min_score double precision NOT NULL DEFAULT 0;
ALTER TABLE public.matching_strategy_settings ADD COLUMN IF NOT EXISTS config_version bigint NOT NULL DEFAULT 1;
ALTER TABLE public.matching_strategy_settings ADD COLUMN IF NOT EXISTS has_override boolean NOT NULL DEFAULT true;
UPDATE public.matching_strategy_settings SET id=1 WHERE id<>1;
INSERT INTO public.matching_strategy_settings(id,active_strategy,updated_at,min_score,config_version,has_override)
SELECT 1,'BALANCED',now(),0,
       CASE WHEN EXISTS(SELECT 1 FROM public.matching_configs) THEN 1 ELSE 0 END,
       EXISTS(SELECT 1 FROM public.matching_configs)
WHERE NOT EXISTS(SELECT 1 FROM public.matching_strategy_settings);

ALTER TABLE public.matching_strategy_settings DROP CONSTRAINT IF EXISTS matching_settings_singleton_check;
ALTER TABLE public.matching_strategy_settings ADD CONSTRAINT matching_settings_singleton_check CHECK(id=1);
ALTER TABLE public.matching_strategy_settings DROP CONSTRAINT IF EXISTS matching_settings_min_score_check;
ALTER TABLE public.matching_strategy_settings ADD CONSTRAINT matching_settings_min_score_check CHECK(min_score>=0 AND min_score<=1);
ALTER TABLE public.matching_strategy_settings DROP CONSTRAINT IF EXISTS matching_settings_version_check;
ALTER TABLE public.matching_strategy_settings ADD CONSTRAINT matching_settings_version_check CHECK(config_version>=0);

CREATE TABLE IF NOT EXISTS public.matching_enabled_criteria (
    setting_id bigint NOT NULL REFERENCES public.matching_strategy_settings(id) ON DELETE CASCADE,
    criterion varchar(20) NOT NULL CHECK(criterion IN ('COURSE','AVAILABILITY','STUDY_MODE','STUDY_GOAL','GROUP_SIZE')),
    PRIMARY KEY(setting_id,criterion)
);
-- Legacy settings had all five criteria enabled. A repeat run leaves explicit disabled choices alone.
INSERT INTO public.matching_enabled_criteria(setting_id,criterion)
SELECT settings.id,criteria.criterion
FROM public.matching_strategy_settings settings
CROSS JOIN (VALUES('COURSE'),('AVAILABILITY'),('STUDY_MODE'),('STUDY_GOAL'),('GROUP_SIZE')) criteria(criterion)
WHERE settings.has_override AND NOT EXISTS(
    SELECT 1 FROM public.matching_enabled_criteria enabled WHERE enabled.setting_id=settings.id
)
ON CONFLICT DO NOTHING;
ALTER TABLE public.matching_enabled_criteria ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.matching_enabled_criteria FROM PUBLIC,anon,authenticated;
