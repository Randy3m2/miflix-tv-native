-- Run once in Supabase SQL Editor. Avatars are public profile pictures, not private documents.
INSERT INTO storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
VALUES ('miflix-avatars','miflix-avatars',true,512000,ARRAY['image/jpeg'])
ON CONFLICT(id) DO UPDATE SET public=true,file_size_limit=512000,allowed_mime_types=ARRAY['image/jpeg'];
DROP POLICY IF EXISTS miflix_avatar_insert ON storage.objects;
CREATE POLICY miflix_avatar_insert ON storage.objects FOR INSERT TO authenticated
WITH CHECK(bucket_id='miflix-avatars' AND (storage.foldername(name))[1]=auth.uid()::text);
DROP POLICY IF EXISTS miflix_avatar_delete ON storage.objects;
CREATE POLICY miflix_avatar_delete ON storage.objects FOR DELETE TO authenticated
USING(bucket_id='miflix-avatars' AND (storage.foldername(name))[1]=auth.uid()::text);
DROP POLICY IF EXISTS miflix_avatar_select ON storage.objects;
CREATE POLICY miflix_avatar_select ON storage.objects FOR SELECT TO authenticated
USING(bucket_id='miflix-avatars' AND (storage.foldername(name))[1]=auth.uid()::text);
