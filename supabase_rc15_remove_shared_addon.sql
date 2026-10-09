-- Run in Supabase SQL Editor as project administrator, after editing the TWO values below.
-- Removes ONLY the exact leaked manifest from other accounts. Keeps the owner's setup,
-- unrelated add-ons, profiles, favorites, playback history, ratings and TMDB token.
-- Find your owner UUID in Authentication > Users. Copy the old manifest from your setup.
DO $$
DECLARE
    owner_user_id uuid := NULL; -- Replace NULL with 'your-owner-user-uuid'::uuid
    shared_manifest text := ''; -- Paste the EXACT old /manifest.json URL between the quotes
    changed integer;
BEGIN
    shared_manifest := btrim(shared_manifest);
    IF owner_user_id IS NULL OR shared_manifest = '' OR shared_manifest NOT LIKE 'http%' THEN
        RAISE EXCEPTION 'Set owner_user_id and the exact shared_manifest before running this cleanup';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM auth.users WHERE id=owner_user_id) THEN
        RAISE EXCEPTION 'Owner UUID does not exist in auth.users';
    END IF;
    WITH candidates AS (
        SELECT user_id,profile_id,state,state->'privateSetup' AS setup,
            COALESCE((SELECT jsonb_agg(value ORDER BY ordinal)
                FROM jsonb_array_elements(CASE WHEN jsonb_typeof(state#>'{privateSetup,addonManifests}')='array'
                    THEN state#>'{privateSetup,addonManifests}' ELSE '[]'::jsonb END) WITH ORDINALITY AS a(value,ordinal)
                WHERE value #>> '{}' <> shared_manifest),'[]'::jsonb) AS kept
        FROM public.miflix_user_state
        WHERE profile_id='__account__' AND user_id<>owner_user_id
            AND (state#>>'{privateSetup,torrentioManifest}'=shared_manifest
                OR state#>'{privateSetup,addonManifests}' @> jsonb_build_array(shared_manifest))
    )
    UPDATE public.miflix_user_state AS target
        SET state=jsonb_set(candidates.state,'{privateSetup}',candidates.setup || jsonb_build_object(
            'addonManifests',kept,
            'torrentioManifest',CASE WHEN candidates.setup->>'torrentioManifest'=shared_manifest
                THEN COALESCE(kept->>0,'') ELSE COALESCE(candidates.setup->>'torrentioManifest','') END)),
            updated_at=now()
        FROM candidates WHERE target.user_id=candidates.user_id AND target.profile_id=candidates.profile_id;
    GET DIAGNOSTICS changed=ROW_COUNT;
    RAISE NOTICE 'Cleaned % other account(s). Owner and unrelated data preserved.',changed;
END $$;
