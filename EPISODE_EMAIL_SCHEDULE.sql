-- AFTER deploying the function and configuring its secrets.
-- Enable Cron, pg_net, and Vault in your Supabase project first.
-- Replace the two placeholders, run once, and keep this secret out of the app.
select vault.create_secret('https://YOUR_PROJECT.supabase.co', 'brunio_project_url');
select vault.create_secret('REPLACE_WITH_EPISODE_CRON_SECRET', 'brunio_episode_cron_secret');
select cron.schedule('brunio-episode-reminders', '15 * * * *', $$
 select net.http_post(
  url := (select decrypted_secret from vault.decrypted_secrets where name='brunio_project_url') || '/functions/v1/brunio-episode-reminders',
  headers := jsonb_build_object('Content-Type','application/json','x-cron-secret',
   (select decrypted_secret from vault.decrypted_secrets where name='brunio_episode_cron_secret')),
  body := '{}'::jsonb,
  timeout_milliseconds := 120000
 );
$$);
