// Server only: never bundle service-role or email credentials in the Android app.
const env = (key: string) => {
  const value = Deno.env.get(key);
  if (!value) throw new Error(`Missing configuration: ${key}`);
  return value;
};
async function json(url: string, init: RequestInit = {}) {
  const response = await fetch(url, { ...init, signal: AbortSignal.timeout(15000) });
  if (!response.ok) throw new Error(`Upstream HTTP ${response.status}`);
  return response.status === 204 ? null : response.json();
}
type Recipient = { user_id: string; email: string; name: string; show_id: number };
type Episode = { id: number; air_date: string; season_number: number; episode_number: number };
Deno.serve(async (request: Request) => {
  if (request.method !== 'POST') return new Response('Method not allowed', { status: 405 });
  const secret = Deno.env.get('EPISODE_CRON_SECRET');
  if (!secret || request.headers.get('x-cron-secret') !== secret)
    return new Response('Unauthorized', { status: 401 });
  try {
    const base = env('SUPABASE_URL');
    const service = env('SUPABASE_SERVICE_ROLE_KEY');
    const token = env('TMDB_TOKEN');
    const mailKey = env('RESEND_API_KEY');
    const from = env('EPISODE_EMAIL_FROM');
    const timezone = Deno.env.get('EPISODE_TIMEZONE') || 'America/Santo_Domingo';
    const parts = new Intl.DateTimeFormat('en-US', { timeZone: timezone, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date());
    const part = (name: string) => parts.find(p => p.type === name)!.value;
    const today = `${part('year')}-${part('month')}-${part('day')}`;
    const headers = { apikey: service, Authorization: `Bearer ${service}`, 'Content-Type': 'application/json' };
    const rest = (path: string, init: RequestInit = {}) => json(`${base}/rest/v1/${path}`, { ...init, headers: { ...headers, ...init.headers } });
    const tmdb = (path: string) => {
      const url = new URL(`https://api.themoviedb.org/3${path}`);
      url.searchParams.set('language', 'es-ES');
      if (!token.startsWith('eyJ')) url.searchParams.set('api_key', token);
      return json(url.href, { headers: token.startsWith('eyJ') ? { Authorization: `Bearer ${token}` } : {} });
    };
    const recipients: Recipient[] = await rest('rpc/brunio_episode_recipients', { method: 'POST', body: '{}' });
    const shows = new Map<number, Recipient[]>();
    for (const recipient of recipients) shows.set(recipient.show_id, [...(shows.get(recipient.show_id) || []), recipient]);
    let sent = 0, failures = 0;
    for (const [showId, users] of shows) {
      try {
        const show = await tmdb(`/tv/${showId}`);
        // Inspect seasons airing today, including multiple episodes released together.
        const seasons = new Set<number>();
        for (const episode of [show.last_episode_to_air, show.next_episode_to_air])
          if (episode?.air_date === today) seasons.add(episode.season_number);
        for (const season of show.seasons || [])
          if (season.air_date === today) seasons.add(season.season_number);
        for (const season of seasons) {
          const data = await tmdb(`/tv/${showId}/season/${season}`);
          for (const episode of (data.episodes || []) as Episode[]) {
            if (episode.air_date !== today) continue;
            for (const user of users) {
              try {
                const exists = await rest(`brunio_episode_mail?user_id=eq.${user.user_id}&episode_id=eq.${episode.id}&select=episode_id`);
                if (exists.length) continue;
                // Deterministic payload + provider idempotency covers concurrent calls/retries.
                const key = `brunio-episode/${user.user_id}/${episode.id}`;
                await json('https://api.resend.com/emails', {
                  method: 'POST',
                  headers: { Authorization: `Bearer ${mailKey}`, 'Content-Type': 'application/json', 'Idempotency-Key': key },
                  body: JSON.stringify({ from, to: [user.email],
                    subject: `BruniO: ${show.name} · S${episode.season_number} E${episode.episode_number}`,
                    text: `Hello ${user.name},\n\nReminder de que hoy se estrena el episodio ${episode.episode_number}, temporada ${episode.season_number}, de tu serie ${show.name}.\n\nPasa por BruniO para disfrutar sin precedentes.\n\nLa disponibilidad puede variar según tu fuente de streaming. Para dejar de recibir avisos de esta serie, quítala de favoritos en todos tus perfiles y sincroniza tu cuenta.` })
                });
                await rest('brunio_episode_mail?on_conflict=user_id,episode_id', {
                  method: 'POST', headers: { Prefer: 'resolution=ignore-duplicates,return=minimal' },
                  body: JSON.stringify({ user_id: user.user_id, episode_id: episode.id })
                });
                sent++;
              } catch { failures++; }
            }
          }
        }
      } catch { failures++; }
    }
    return Response.json({ sent, failures, releaseDate: today }, { status: failures ? 503 : 200 });
  } catch {
    return Response.json({ error: 'Reminder job failed; check server configuration and upstream services.' }, { status: 500 });
  }
});
