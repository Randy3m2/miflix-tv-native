-- Run after SUPABASE_RC22_CALENDAR.sql. Repeatable; preserves existing events and RLS.
BEGIN;
ALTER TABLE public.miflix_calendar_events ADD COLUMN IF NOT EXISTS channel_id text NOT NULL DEFAULT '';
ALTER TABLE public.miflix_calendar_events ADD COLUMN IF NOT EXISTS channel_type text NOT NULL DEFAULT 'tv';
DO $$ DECLARE c record; BEGIN
 FOR c IN SELECT conname FROM pg_constraint WHERE conrelid='public.miflix_calendar_events'::regclass AND contype='c'
 AND (pg_get_constraintdef(oid) LIKE '%kind%' OR pg_get_constraintdef(oid) LIKE '%cloud_id%') LOOP
  EXECUTE format('ALTER TABLE public.miflix_calendar_events DROP CONSTRAINT %I',c.conname);
 END LOOP;
END $$;
ALTER TABLE public.miflix_calendar_events ADD CONSTRAINT calendar_content_valid CHECK (
 (kind='movie' AND cloud_id IS NOT NULL AND cloud_id ~ '^tmdb:movie:[0-9]+$' AND season=0 AND episode=0 AND channel_id='') OR
 (kind='series' AND cloud_id IS NOT NULL AND cloud_id ~ '^tmdb:series:[0-9]+$' AND season>0 AND episode>0 AND channel_id='') OR
 (kind IN ('live','sports') AND cloud_id IS NULL AND length(btrim(channel_id)) BETWEEN 1 AND 1000 AND length(btrim(channel_type)) BETWEEN 1 AND 80 AND season=0 AND episode=0) OR
 -- Legacy game events remain readable/deletable; creating new games is blocked by RLS below.
 (kind='game' AND cloud_id IS NULL AND channel_id='')
);
DROP POLICY IF EXISTS calendar_create ON public.miflix_calendar_events;
CREATE POLICY calendar_create ON public.miflix_calendar_events FOR INSERT TO authenticated WITH CHECK (
 owner_id=auth.uid() AND kind IN ('movie','series','live','sports') AND public.miflix_calendar_invites_valid(guests)
);
NOTIFY pgrst,'reload schema';
COMMIT;
