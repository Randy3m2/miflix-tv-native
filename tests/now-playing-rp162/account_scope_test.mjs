import {PGlite} from '@electric-sql/pglite';
import fs from 'node:fs';import assert from 'node:assert/strict';
const db=new PGlite();
await db.exec(`create role anon;create role authenticated;
create schema auth;create function auth.uid() returns uuid language sql as $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;
create table public.brunio_rp1_actors(id uuid primary key,account_id uuid,profile_id text);
create table public.rp1_miflix_presence(user_id uuid,title text,cloud_id text,season integer,episode integer,event_device text,expires_at timestamptz,updated_at timestamptz);
insert into brunio_rp1_actors values
 ('11111111-1111-4111-8111-111111111111','11111111-1111-4111-8111-111111111111','main'),
 ('22222222-2222-4222-8222-222222222222','11111111-1111-4111-8111-111111111111','other-profile'),
 ('33333333-3333-4333-8333-333333333333','33333333-3333-4333-8333-333333333333','foreign-profile'),
 ('44444444-4444-4444-8444-444444444444','11111111-1111-4111-8111-111111111111','expired'),
 ('55555555-5555-4555-8555-555555555555','11111111-1111-4111-8111-111111111111','phone');
insert into rp1_miflix_presence values
 ('11111111-1111-4111-8111-111111111111','A','tmdb:movie:1',0,0,'tv:a',now()+interval '1 hour',now()),
 ('22222222-2222-4222-8222-222222222222','B','tmdb:series:2',1,2,'tv:b',now()+interval '1 hour',now()),
 ('33333333-3333-4333-8333-333333333333','FOREIGN','tmdb:movie:3',0,0,'tv:c',now()+interval '1 hour',now()),
 ('44444444-4444-4444-8444-444444444444','OLD','tmdb:movie:4',0,0,'tv:d',now()-interval '1 hour',now()),
 ('55555555-5555-4555-8555-555555555555','PHONE','tmdb:movie:5',0,0,'mobile:e',now()+interval '1 hour',now());`);
const sql=fs.readFileSync(new URL('../../supabase_now_playing_rp162.sql',import.meta.url),'utf8');
await db.exec(sql);await db.exec(sql);
await db.exec("set request.jwt.claim.sub='11111111-1111-4111-8111-111111111111';set role authenticated");
let rows=(await db.query('select * from public.brunio_now_playing_rp162()')).rows;
assert.deepEqual(rows.map(x=>x.title).sort(),['A','B']);assert(rows.every(x=>Number(x.valid_for_ms)>0));
await db.exec("reset role;set request.jwt.claim.sub='33333333-3333-4333-8333-333333333333';set role authenticated");
assert.deepEqual((await db.query('select title from public.brunio_now_playing_rp162()')).rows.map(x=>x.title),['FOREIGN']);
await db.exec("reset role;set request.jwt.claim.sub='';set role authenticated");
assert.deepEqual((await db.query('select * from public.brunio_now_playing_rp162()')).rows,[]);
await db.exec('reset role;set role anon');
await assert.rejects(db.query('select * from public.brunio_now_playing_rp162()'),/permission denied/);
console.log('PASS: own account only, multiple profiles, no phones/expired rows, signed-out empty, anonymous denied, rerunnable migration.');
await db.close();
