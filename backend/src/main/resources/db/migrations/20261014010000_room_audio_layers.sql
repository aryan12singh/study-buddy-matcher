-- Keep legacy columns for repeatable upgrades; new room state uses two independent layers.
ALTER TABLE study_rooms ADD COLUMN IF NOT EXISTS music_preset varchar(20);
ALTER TABLE study_rooms ADD COLUMN IF NOT EXISTS music_playing boolean;
ALTER TABLE study_rooms ADD COLUMN IF NOT EXISTS ambient_preset varchar(20);
ALTER TABLE study_rooms ADD COLUMN IF NOT EXISTS ambient_playing boolean;

-- A NULL playing flag identifies only rows awaiting their first audio-layer backfill.
-- Re-running migrations never overwrites current room selections or resurrects playback.
UPDATE study_rooms
SET music_preset = CASE WHEN audio_preset = 'CALM_MUSIC' THEN audio_preset ELSE NULL END,
    music_playing = audio_preset = 'CALM_MUSIC' AND audio_playing,
    ambient_preset = CASE WHEN audio_preset <> 'CALM_MUSIC' THEN audio_preset ELSE NULL END,
    ambient_playing = audio_preset <> 'CALM_MUSIC' AND audio_playing
WHERE music_playing IS NULL OR ambient_playing IS NULL;

ALTER TABLE study_rooms ALTER COLUMN music_playing SET DEFAULT false;
ALTER TABLE study_rooms ALTER COLUMN music_playing SET NOT NULL;
ALTER TABLE study_rooms ALTER COLUMN ambient_playing SET DEFAULT false;
ALTER TABLE study_rooms ALTER COLUMN ambient_playing SET NOT NULL;
ALTER TABLE study_rooms ALTER COLUMN audio_preset SET DEFAULT 'CALM_MUSIC';

ALTER TABLE study_rooms DROP CONSTRAINT IF EXISTS room_music_layer_check;
ALTER TABLE study_rooms ADD CONSTRAINT room_music_layer_check CHECK (
    (music_preset IS NULL OR music_preset = 'CALM_MUSIC') AND (NOT music_playing OR music_preset IS NOT NULL)
);
ALTER TABLE study_rooms DROP CONSTRAINT IF EXISTS room_ambient_layer_check;
ALTER TABLE study_rooms ADD CONSTRAINT room_ambient_layer_check CHECK (
    (ambient_preset IS NULL OR ambient_preset IN ('RAIN','WHITE_NOISE','CAFE'))
    AND (NOT ambient_playing OR ambient_preset IS NOT NULL)
);
