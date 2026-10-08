const {PGlite}=require('@electric-sql/pglite');const fs=require('fs'),assert=require('assert');
const A='11111111-1111-4111-8111-111111111111',B='22222222-2222-4222-8222-222222222222',C='33333333-3333-4333-8333-333333333333';
(async()=>{
const db=new PGlite();await db.exec(`create role authenticated;create role anon;create role service_role bypassrls;create schema auth;create table auth.users(id uuid primary key);insert into auth.users values('${A}'),('${B}'),('${C}');create function auth.uid() returns uuid language sql stable as $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;grant usage on schema public,auth to authenticated;grant execute on function auth.uid() to authenticated;`);
// Start from the actual Beta schema, then upgrade: this is the reported failure case.
await db.exec(fs.readFileSync('SUPABASE_WATCH_PARTY.sql','utf8'));
await db.exec(`insert into public.miflix_watch_parties(room_code,host_user_id,cloud_id,media_type)values('123456','${A}','tmdb:movie:99','movie');`);
await db.exec(fs.readFileSync('supabase_watch_party_repair.sql','utf8'));
await db.exec(fs.readFileSync('supabase_social_setup.sql','utf8'));
// Repeat migration to confirm safe reruns.
await db.exec(fs.readFileSync('supabase_social_setup.sql','utf8'));
await db.exec(fs.readFileSync('supabase_final_upgrade.sql','utf8'));
await db.exec(fs.readFileSync('supabase_final_upgrade.sql','utf8'));
const legacy=(await db.query('select state from miflix_watch_parties')).rows[0].state;assert.equal(legacy.cloudId,'tmdb:movie:99');
async function as(uid){await db.exec(`reset role;set role authenticated;select set_config('request.jwt.claim.sub','${uid}',false);`)}
async function denied(sql){let bad=false;try{await db.exec(sql)}catch{bad=true}assert(bad,'Expected RLS/constraint rejection: '+sql)}
await as(A);await db.exec(`insert into miflix_social_profiles values('${A}','randy',now());select miflix_join_room('123456');insert into miflix_watch_parties(room_code,host_user_id,state)values('654321','${A}','{}') on conflict(room_code) do update set state=excluded.state returning *;`);
await as(B);await db.exec(`insert into miflix_social_profiles values('${B}','friend_b',now());select miflix_join_room('123456');insert into miflix_party_events(room_code,user_id,kind,body)values('123456','${B}','emoji','🔥');`);
await db.exec(`select miflix_request_pause('123456','tmdb:movie:99',0,0);`);
await denied(`select miflix_take_pause('123456');`);
await denied(`select miflix_request_pause('123456','tmdb:movie:999',0,0);`);
await denied(`select * from miflix_party_pause_requests;`);
await as(A);assert.equal((await db.query("select miflix_take_pause('123456') as paused")).rows[0].paused,true);
assert.equal((await db.query("select miflix_take_pause('123456') as paused")).rows[0].paused,false);
await as(B);
await denied(`insert into miflix_party_events(room_code,user_id,kind,body)values('123456','${A}','chat','spoof');`);
await as(C);await denied(`select miflix_request_pause('123456','tmdb:movie:99',0,0);`);assert.equal((await db.query('select * from miflix_party_events')).rows.length,0);assert.equal((await db.query('select * from miflix_party_members')).rows.length,0);await denied(`insert into miflix_party_events(room_code,user_id,kind,body)values('123456','${C}','chat','stranger');`);await denied(`select miflix_join_room('000000');`);
await as(A);assert.equal((await db.query('select * from miflix_party_events')).rows.length,1);await db.exec(`insert into miflix_friendships(sender_id,receiver_id)values('${A}','${B}');`);
await db.exec(`update miflix_friendships set status='accepted' where sender_id='${A}';`);
assert.equal((await db.query('select status from miflix_friendships')).rows[0].status,'pending');
await as(B);await denied(`update miflix_friendships set sender_id='${C}',status='accepted' where receiver_id='${B}';`);
await db.exec(`update miflix_friendships set status='accepted' where receiver_id='${B}';`);
assert.equal((await db.query('select status from miflix_friendships')).rows[0].status,'accepted');
await denied(`insert into miflix_social_profiles(user_id,nickname)values('${C}','randy');`);
await denied(`select * from miflix_trakt_tokens;`);
await as(C);assert.equal((await db.query('select * from miflix_friendships')).rows.length,0);
await db.exec('reset role');await db.exec("update miflix_watch_parties set expires_at=now()-interval '1 second' where room_code='123456';");
await as(B);assert.equal((await db.query('select * from miflix_party_events')).rows.length,0);
console.log('PASS: guest pause authorization, matching media, host-only consumption, RLS upsert, real PostgreSQL WASM: Beta migration/backfill, repeatable setup, host room insert, membership, event ownership, stranger isolation, friendship consent/identity guard, token isolation, room expiration');
await db.close();
})().catch(e=>{console.error(e);process.exitCode=1});
