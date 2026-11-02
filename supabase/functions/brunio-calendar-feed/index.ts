// Deploy with --no-verify-jwt: GET uses an unguessable bearer URL; POST verifies Supabase Auth.
const headers = { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' };
const reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers });
const hash = async (s: string) => Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(s)))).map(x => x.toString(16).padStart(2, '0')).join('');
Deno.serve(async (request: Request) => {
 try {
  const base = Deno.env.get('SUPABASE_URL')!;const service = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
  const admin = { apikey: service, Authorization: `Bearer ${service}`, 'Content-Type': 'application/json' };
  const rest = async (query: string, init: RequestInit = {}) => { const r = await fetch(`${base}/rest/v1/brunio_calendar_feeds${query}`, { ...init, headers: { ...admin, ...init.headers }, signal: AbortSignal.timeout(10000) });if (!r.ok) throw new Error('Storage failed');return r.status === 204 ? null : r.text(); };
  if (request.method === 'GET') {
   const token = new URL(request.url).searchParams.get('token') || '';if (!/^[a-f0-9]{64}$/.test(token)) return reply({ error: 'Not found' }, 404);
   const rows = JSON.parse((await rest(`?token_hash=eq.${await hash(token)}&select=ics&limit=1`)) || '[]');if (!rows.length) return reply({ error: 'Not found' }, 404);
   return new Response(rows[0].ics, { headers: { 'Content-Type': 'text/calendar; charset=utf-8', 'Cache-Control': 'private, max-age=900', 'Referrer-Policy': 'no-referrer', 'X-Content-Type-Options': 'nosniff' } });
  }
  if (request.method !== 'POST') return reply({ error: 'Method not allowed' }, 405);
  const body = await request.text();if (body.length > 400000) return reply({ error: 'Too large' }, 413);
  const input = JSON.parse(body);
  const validIcs = typeof input.ics === 'string' && new TextEncoder().encode(input.ics).length <= 400000 && input.ics.startsWith('BEGIN:VCALENDAR\r\n') && input.ics.endsWith('END:VCALENDAR\r\n');
  if (input.action === 'publish') {
   if (typeof input.publisher !== 'string' || !/^[a-f0-9]{64}$/.test(input.publisher) || !validIcs) return reply({ error: 'Unauthorized' }, 401);
   const filter = `?publish_hash=eq.${await hash(input.publisher)}`;
   const rows = JSON.parse((await rest(filter + '&select=user_id&limit=1')) || '[]');if (!rows.length) return reply({ error: 'Unauthorized' }, 401);
   await rest(filter, { method: 'PATCH', headers: { Prefer: 'return=minimal' }, body: JSON.stringify({ ics: input.ics, updated_at: new Date().toISOString() }) });return reply({ ok: true });
  }
  const bearer = request.headers.get('Authorization') || '';const auth = await fetch(`${base}/auth/v1/user`, { headers: { apikey: service, Authorization: bearer }, signal: AbortSignal.timeout(10000) });if (!auth.ok) return reply({ error: 'Unauthorized' }, 401);
  const user = await auth.json();if (!user.id) return reply({ error: 'Unauthorized' }, 401);
  const profile = input.profile;if (typeof profile !== 'string' || !profile.length || profile.length > 100) return reply({ error: 'Invalid profile' }, 400);
  const filter = `?user_id=eq.${encodeURIComponent(user.id)}&profile_id=eq.${encodeURIComponent(profile)}`;
  if (input.action === 'revoke') {await rest(filter, { method: 'DELETE', headers: { Prefer: 'return=minimal' } });return reply({ ok: true });}
  if (!['create', 'update'].includes(input.action) || !validIcs) return reply({ error: 'Invalid calendar' }, 400);
  if (input.action === 'update') {await rest(filter, { method: 'PATCH', headers: { Prefer: 'return=minimal' }, body: JSON.stringify({ ics: input.ics, updated_at: new Date().toISOString() }) });return reply({ ok: true });}
  const bytes = crypto.getRandomValues(new Uint8Array(32));const token = Array.from(bytes).map(x => x.toString(16).padStart(2, '0')).join('');
  const publisher = Array.from(crypto.getRandomValues(new Uint8Array(32))).map(x => x.toString(16).padStart(2, '0')).join('');
  await rest('?on_conflict=user_id,profile_id', { method: 'POST', headers: { Prefer: 'resolution=merge-duplicates,return=minimal' }, body: JSON.stringify({ user_id: user.id, profile_id: profile, token_hash: await hash(token), publish_hash: await hash(publisher), ics: input.ics, updated_at: new Date().toISOString() }) });
  return reply({ url: `${base}/functions/v1/brunio-calendar-feed?token=${token}`, publisher });
 } catch { return reply({ error: 'Calendar service unavailable' }, 503); }
});
