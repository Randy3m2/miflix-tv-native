const {PGlite}=require('@electric-sql/pglite');const fs=require('fs'),assert=require('assert');
(async()=>{
const db=new PGlite(); const A='11111111-1111-4111-8111-111111111111',B='22222222-2222-4222-8222-222222222222';
await db.exec(`create role authenticated; create role anon; create schema auth; create function auth.uid() returns uuid language sql stable as $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;
create table miflix_user_state(user_id uuid,profile_id text,state jsonb,updated_at timestamptz default now(),primary key(user_id,profile_id));create table miflix_ratings(user_id uuid,profile_id text,score int);
insert into miflix_user_state values('${A}','__account__','{"profiles":[{"id":"main","name":"Main","primary":true},{"id":"second","name":"Second"}],"privateSetup":{"addonManifests":["private-url"]}}',now()),('${A}','main','{"favorites":["a"]}',now()),('${A}','second','{}',now()),('${B}','__account__','{"profiles":[{"id":"other","primary":true},{"id":"other2"}]}',now());insert into miflix_ratings values('${A}','main',10),('${A}','second',9),('${B}','main',8);grant usage on schema public,auth to authenticated;grant execute on function auth.uid() to authenticated;`);
const sql=fs.readFileSync(require('path').join(__dirname,'../supabase_rc6_upgrade.sql'),'utf8');await db.exec(sql);await db.exec(sql);
async function as(id){await db.exec(`reset role;set role authenticated;select set_config('request.jwt.claim.sub','${id}',false);`)}
async function denied(q){let e=false;try{await db.query(q)}catch{e=true}assert(e,q)}
await as(A);await denied("select miflix_delete_profile('__account__')");await denied("select miflix_delete_profile('other')");
let rows=(await db.query("select miflix_delete_profile('main') as profiles")).rows[0].profiles;assert.equal(rows.length,1);assert.equal(rows[0].id,'second');assert.equal(rows[0].primary,true);
await denied("select miflix_delete_profile('second')");await denied("select miflix_delete_profile('main')");await db.exec('reset role');
assert.equal((await db.query(`select count(*)::int as n from miflix_user_state where user_id='${A}' and profile_id='main'`)).rows[0].n,0);
assert.equal((await db.query(`select state->'privateSetup' as setup from miflix_user_state where user_id='${A}' and profile_id='__account__'`)).rows[0].setup.addonManifests[0],'private-url');
assert.equal((await db.query(`select count(*)::int as n from miflix_ratings where user_id='${A}' and profile_id='main'`)).rows[0].n,0);
assert.equal((await db.query(`select count(*)::int as n from miflix_ratings where user_id='${B}'`)).rows[0].n,1);
await as('');await denied("select miflix_delete_profile('other')");await db.exec('reset role;set role anon');await denied("select miflix_delete_profile('other')");
await db.close();console.log('RC6: atomic deletion, last-profile guard, primary reassignment, progress/rating cleanup, account isolation, private setup retention and rerun passed');
})().catch(e=>{console.error(e);process.exit(1)});
