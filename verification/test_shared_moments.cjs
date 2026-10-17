'use strict';
const {PGlite}=require('@electric-sql/pglite');
const fs=require('fs'),assert=require('assert');
const A='11111111-1111-4111-8111-111111111111',B='22222222-2222-4222-8222-222222222222',C='33333333-3333-4333-8333-333333333333';
(async()=>{
 const db=new PGlite();
 await db.exec(`create role authenticated;create role anon;create role service_role bypassrls;create schema auth;create table auth.users(id uuid primary key);insert into auth.users values('${A}'),('${B}'),('${C}');create function auth.uid() returns uuid language sql stable as $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;grant usage on schema public,auth to authenticated,anon;grant execute on function auth.uid() to authenticated;`);
 for(const name of ['SUPABASE_WATCH_PARTY.sql','supabase_watch_party_repair.sql','supabase_social_setup.sql','supabase_final_upgrade.sql','supabase_friends_party_upgrade.sql','supabase_rc5_upgrade.sql','supabase_rc63_party_presentation.sql','supabase_rc30_upgrade.sql','supabase_rc62_presence_window.sql','supabase_rc70_event_presence.sql','supabase_rc73_shared_moments.sql','supabase_rc73_shared_moments.sql']) await db.exec(fs.readFileSync(name,'utf8'));
 async function as(uid){await db.exec(`reset role;set role authenticated;select set_config('request.jwt.claim.sub','${uid}',false);`);}
 async function denied(sql){let rejected=false;try{await db.exec(sql)}catch{rejected=true}assert(rejected,'Expected rejection');}


 await as(A);await db.exec(`insert into miflix_social_profiles(user_id,nickname) values('${A}','randy');insert into miflix_friendships(sender_id,receiver_id) values('${A}','${B}');`);
 await as(B);await db.exec(`insert into miflix_social_profiles(user_id,nickname) values('${B}','friend_b');update miflix_friendships set status='accepted' where receiver_id='${B}';`);
 const id='aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';
 const insert=(recipient=B,start=10000,end=20000,cloud='tmdb:movie:1',season=0,episode=0,sender=A)=>`insert into brunio_shared_moments(id,sender_id,recipient_id,cloud_id,title,season,episode,start_ms,end_ms) values('${id}','${sender}','${recipient}','${cloud}','Scene',${season},${episode},${start},${end}) on conflict(id) do nothing`;
 await as(A);await denied(insert(C));await denied(insert(B,20000,10000));await denied(insert(B,-1,10000));await denied(insert(B,0,86400001));await denied(insert(B,0,10000,'tmdb:series:1'));await denied(insert(B,0,10000,'live:1'));
 await db.exec(insert());await db.exec(insert());
 assert.equal((await db.query('select * from brunio_received_moments()')).rows.length,0);
 await denied(`update brunio_shared_moments set title='changed' where id='${id}'`);
 await as(B);let rows=(await db.query('select * from brunio_received_moments()')).rows;
 assert.equal(rows.length,1);assert.equal(rows[0].nickname,'randy');assert.equal(Number(rows[0].start_ms),10000);assert.equal(Number(rows[0].end_ms),20000);
 await denied(insert(A,10000,20000,'tmdb:movie:1',0,0,A));
 await as(C);assert.equal((await db.query('select * from brunio_received_moments()')).rows.length,0);assert.equal((await db.query('select * from brunio_shared_moments')).rows.length,0);
 await db.exec(`delete from brunio_shared_moments where id='${id}'`);
 await as(B);assert.equal((await db.query('select * from brunio_received_moments()')).rows.length,1);
 await db.exec(`delete from brunio_shared_moments where id='${id}'`);assert.equal((await db.query('select * from brunio_received_moments()')).rows.length,0);
 await as(A);await db.exec(insert(B,30000,50000,'tmdb:series:2',1,3));
 await as(B);rows=(await db.query('select * from brunio_received_moments()')).rows;assert.equal(rows[0].season,1);assert.equal(rows[0].episode,3);
 await db.exec(`delete from miflix_friendships where receiver_id='${B}'`);assert.equal((await db.query('select * from brunio_received_moments()')).rows.length,0);
 await db.exec("reset role;set role anon;select set_config('request.jwt.claim.sub','',false);");await denied('select * from brunio_received_moments()');
 await db.close();console.log('PASS: repeatable moments migration, accepted-friend-only send/read, caller spoof rejection, private inbox, idempotent sends, valid ranges/media/episodes, recipient deletion, former-friend isolation, anonymous denial.');
})().catch(e=>{console.error(e);process.exitCode=1});
