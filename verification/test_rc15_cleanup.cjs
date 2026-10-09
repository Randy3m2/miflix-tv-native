const {PGlite}=require('@electric-sql/pglite');
const fs=require('fs'),assert=require('assert'),path=require('path');
(async()=>{
 const db=new PGlite();
 const owner='11111111-1111-4111-8111-111111111111',other='22222222-2222-4222-8222-222222222222',legacy='33333333-3333-4333-8333-333333333333',untouched='44444444-4444-4444-8444-444444444444';
 const leaked='https://torrentio.example.test/private/manifest.json',personal='https://comet.example.test/own/manifest.json';
 await db.exec('create schema auth; create table auth.users(id uuid primary key);create table miflix_user_state(user_id uuid,profile_id text,state jsonb,updated_at timestamptz default now(),primary key(user_id,profile_id));');
 for(const id of [owner,other,legacy,untouched]) await db.query('insert into auth.users values($1)',[id]);
 const states={
  [owner]:{profiles:[{id:'owner'}],privateSetup:{tmdbToken:'owner-token',torrentioManifest:leaked,addonManifests:[leaked]}},
  [other]:{profiles:[{id:'friend'}],privateSetup:{tmdbToken:'friend-token',torrentioManifest:leaked,addonManifests:[leaked,personal]}},
  [legacy]:{profiles:[{id:'legacy'}],privateSetup:{tmdbToken:'legacy-token',torrentioManifest:leaked}},
  [untouched]:{profiles:[{id:'unrelated'}],privateSetup:{torrentioManifest:personal,addonManifests:[personal]}}
 };
 for(const [id,state] of Object.entries(states)) await db.query("insert into miflix_user_state(user_id,profile_id,state) values($1,'__account__',$2)",[id,JSON.stringify(state)]);
 await db.query("insert into miflix_user_state(user_id,profile_id,state) values($1,'main',$2)",[other,JSON.stringify({favorites:['movie'],privateSetup:{torrentioManifest:leaked}})]);
 const original=fs.readFileSync(process.argv[2]||path.join(__dirname,'..','supabase_rc15_remove_shared_addon.sql'),'utf8');
 let rejected=false;try{await db.exec(original)}catch{rejected=true}assert(rejected,'unconfigured cleanup must fail');
 const sql=original.replace('owner_user_id uuid := NULL',`owner_user_id uuid := '${owner}'::uuid`).replace("shared_manifest text := ''",`shared_manifest text := '${leaked}'`);
 await db.exec(sql);await db.exec(sql);
 async function state(id,profile='__account__'){return (await db.query('select state from miflix_user_state where user_id=$1 and profile_id=$2',[id,profile])).rows[0].state;}
 assert.deepStrictEqual(await state(owner),states[owner]);
 const b=await state(other);assert.deepStrictEqual(b.profiles,states[other].profiles);assert.equal(b.privateSetup.tmdbToken,'friend-token');assert.deepStrictEqual(b.privateSetup.addonManifests,[personal]);assert.equal(b.privateSetup.torrentioManifest,personal);
 const c=await state(legacy);assert.deepStrictEqual(c.privateSetup.addonManifests,[]);assert.equal(c.privateSetup.torrentioManifest,'');assert.equal(c.privateSetup.tmdbToken,'legacy-token');
 assert.deepStrictEqual(await state(untouched),states[untouched]);assert.equal((await state(other,'main')).favorites[0],'movie');
 await db.close();console.log('PASS RC15 cleanup: guarded parameters; exact leaked link removed; owner, unrelated add-ons, profiles/tokens/progress preserved; legacy and repeat execution handled');
})().catch(e=>{console.error(e);process.exit(1)});
