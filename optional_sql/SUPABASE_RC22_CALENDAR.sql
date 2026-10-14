-- Run once as administrator in the Supabase SQL Editor. Safe to run again.
CREATE TABLE IF NOT EXISTS public.miflix_calendar_events (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 owner_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
 title text NOT NULL CHECK (length(btrim(title)) BETWEEN 1 AND 160),
 kind text NOT NULL CHECK (kind IN ('movie','series','game')),
 starts_at timestamptz NOT NULL,
 cloud_id text,
 season integer NOT NULL DEFAULT 0 CHECK (season>=0),
 episode integer NOT NULL DEFAULT 0 CHECK (episode>=0),
 guests uuid[] NOT NULL DEFAULT '{}',
 created_at timestamptz NOT NULL DEFAULT now(),
 CHECK (cardinality(guests)<=20),
 CHECK ((kind='game' AND cloud_id IS NULL) OR (kind='movie' AND cloud_id IS NOT NULL AND cloud_id ~ '^tmdb:movie:[0-9]+$' AND season=0 AND episode=0) OR (kind='series' AND cloud_id IS NOT NULL AND cloud_id ~ '^tmdb:series:[0-9]+$' AND season>0 AND episode>0))
);
CREATE INDEX IF NOT EXISTS miflix_calendar_owner_date ON public.miflix_calendar_events(owner_id,starts_at);
CREATE INDEX IF NOT EXISTS miflix_calendar_guests ON public.miflix_calendar_events USING gin(guests);
CREATE OR REPLACE FUNCTION public.miflix_calendar_invites_valid(invitees uuid[]) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=public,pg_temp AS $$
 SELECT auth.uid() IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM unnest(invitees) AS g(id)
  WHERE g.id IS NULL OR g.id=auth.uid() OR NOT EXISTS (
   SELECT 1 FROM public.miflix_friendships f WHERE f.status='accepted'
   AND ((f.sender_id=auth.uid() AND f.receiver_id=g.id) OR (f.receiver_id=auth.uid() AND f.sender_id=g.id))
  )
 );
$$;
REVOKE ALL ON FUNCTION public.miflix_calendar_invites_valid(uuid[]) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.miflix_calendar_invites_valid(uuid[]) TO authenticated;
ALTER TABLE public.miflix_calendar_events ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS calendar_read ON public.miflix_calendar_events;
DROP POLICY IF EXISTS calendar_create ON public.miflix_calendar_events;
DROP POLICY IF EXISTS calendar_delete ON public.miflix_calendar_events;
CREATE POLICY calendar_read ON public.miflix_calendar_events FOR SELECT TO authenticated USING (owner_id=auth.uid() OR auth.uid()=ANY(guests));
CREATE POLICY calendar_create ON public.miflix_calendar_events FOR INSERT TO authenticated WITH CHECK (owner_id=auth.uid() AND public.miflix_calendar_invites_valid(guests));
CREATE POLICY calendar_delete ON public.miflix_calendar_events FOR DELETE TO authenticated USING (owner_id=auth.uid());
REVOKE ALL ON public.miflix_calendar_events FROM anon,authenticated;
GRANT SELECT,INSERT,DELETE ON public.miflix_calendar_events TO authenticated;
NOTIFY pgrst,'reload schema';
