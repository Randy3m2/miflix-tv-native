'use strict';
const {PGlite}=require('@electric-sql/pglite');
const fs=require('fs'),assert=require('assert');
const A='11111111-1111-4111-8111-111111111111',B='22222222-2222-4222-8222-222222222222',C='33333333-3333-4333-8333-333333333333';
(async()=>{
 const db=new PGlite();
 await db.exec(`create role authenticated;create role anon;create role service_role bypassrls;create schema auth;create table auth.users(id uuid primary key);insert into auth.users values('${A}'),('${B}'),('${C}');create function auth.uid() returns uuid language sql stable as $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;grant usage on schema public,auth to authenticated,anon;grant execute on function auth.uid() to authenticated;`);
 for(const name of ['SUPABASE_WATCH_PARTY.sql','supabase_watch_party_repair.sql','supabase_social_setup.sql','supabase_final_upgrade.sql','supabase_friends_party_upgrade.sql','supabase_rc5_upgrade.sql','supabase_rc63_party_presentation.sql','supabase_rc30_upgrade.sql','supabase_rc62_presence_window.sql','supabase_rc70_event_presence.sql','supabase_rc70_event_presence.sql']) await db.exec(fs.readFileSync(name,'utf8'));
 async function as(uid){await db.exec(`reset role;set role authenticated;select set_config('request.jwt.claim.sub','${uid}',false);`);}
 async function denied(sql){let rejected=false;try{await db.exec(sql)}catch{rejected=true}assert(rejected,'Expected rejection');}

 await as(A);await db.exec(`insert into miflix_social_profiles(user_id,nickname) values('${A}','randy');insert into miflix_friendships(sender_id,receiver_id) values('${A}','${B}');`);
 await as(B);await db.exec(`insert into miflix_social_profiles(user_id,nickname) values('${B}','friend_b');update miflix_friendships set status='accepted' where receiver_id='${B}';`);
 async function event(seq,active=true,device='fire-tv',title='Movie',expiry="now()+interval '3 hours'",room='null') {
  await db.exec(`select brunio_presence_event(${active},'${title}','tmdb:movie:1',0,0,${room},'${device}',${seq},${expiry});`);
 }
 await as(A);await event(1);
 // A 2-hour playback remains visible without a single subsequent presence write.
 await db.exec("reset role;update miflix_presence set updated_at=now()-interval '2 hours';");
 await as(B);let rows=(await db.query('select * from miflix_friend_activity_v30()')).rows;
 assert.equal(rows[0].title,'Movie');assert.equal(rows[0].online,true);
 await as(C);assert.equal((await db.query('select * from miflix_friend_activity_v30()')).rows.length,0);
 await denied('select * from brunio_presence_cursors');await denied('select * from miflix_presence');
 await as(A);await event(2,true,'fire-tv','Next movie');await event(1,true,'fire-tv','Old movie');
 await as(B);assert.equal((await db.query('select * from miflix_friend_activity_v30()')).rows[0].title,'Next movie');
 await as(A);await event(3,false);await event(2);
 await as(B);rows=(await db.query('select * from miflix_friend_activity_v30()')).rows;
 assert.equal(rows[0].title,'');assert.equal(rows[0].online,false);assert.equal(rows[0].cloud_id,'');
 await as(A);await event(4);await event(1,true,'other-tv','Other TV');await event(5,false);
 await as(B);assert.equal((await db.query('select * from miflix_friend_activity_v30()')).rows[0].title,'Other TV');
 // Natural expiration is checked on read: no cleanup job, heartbeat or writes required.
 await db.exec("reset role;update miflix_presence set expires_at=now()-interval '1 second';");
 await as(B);assert.equal((await db.query('select * from miflix_friend_activity_v30()')).rows[0].title,'');
 await as(A);await event(6,true,'fire-tv','Expired pending',"now()-interval '1 second'");
 await as(B);assert.equal((await db.query('select * from miflix_friend_activity_v30()')).rows[0].online,false);
 // Older APKs retain their short lease; unauthorized party room cannot be attached.
 await as(A);await db.exec("select miflix_set_presence('Legacy','tmdb:movie:2',0,0,null);");
 await denied("select brunio_presence_event(true,'Spoof','tmdb:movie:3',0,0,'123456','fire-tv',7,now()+interval '1 hour')");
 await db.exec("reset role;update miflix_presence set expires_at=now()-interval '1 second';");
 await as(B);assert.equal((await db.query('select * from miflix_friend_activity()')).rows[0].online,false);
 await db.exec("reset role;set role anon;select set_config('request.jwt.claim.sub','',false);");
 await denied("select brunio_presence_event(true,'x','x',0,0,null,'x',1,now()+interval '1 hour')");
 await db.close();console.log('PASS: repeatable migration, preserved RPC returns, 2h without heartbeat, immediate changes/stops, dedup/order, other-device stop guard, natural expiry, offline expiry, legacy clients, privacy and room authorization.');
})().catch(e=>{console.error(e);process.exitCode=1});
