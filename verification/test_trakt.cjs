const fs=require('fs'),vm=require('vm'),{stripTypeScriptTypes}=require('node:module'),assert=require('assert');
const source=fs.readFileSync('supabase/functions/miflix-trakt/index.ts','utf8');
const js=stripTypeScriptTypes(source);fs.writeFileSync('verification/trakt.js',js);
let handler;const store={};let oauthPending=true;let historyAdds=0;let expiry=Date.now()/1000;
const json=(data,status=200)=>new Response(JSON.stringify(data),{status});
const fetchMock=async(url,options={})=>{
 if(url.endsWith('/auth/v1/user'))return options.headers.Authorization==='Bearer good'?json({id:'user-1'}):json({},401);
 if(url.includes('/rest/v1/')){
  const table=url.split('/rest/v1/')[1].split('?')[0];const method=options.method||'GET';
  if(method==='POST'){store[table]=JSON.parse(options.body);return json([store[table]])}
  if(method==='DELETE'){delete store[table];return new Response('',{status:200})}
  return json(store[table]?[store[table]]:[]);
 }
 if(url.endsWith('/oauth/token')) {
  const payload=JSON.parse(options.body);
  assert.equal(payload.code_verifier,store.miflix_trakt_pending.state.verifier);
  assert(!('client_secret' in payload));
  assert.equal(payload.redirect_uri,'https://db.test/functions/v1/miflix-trakt');
  return json({access_token:'SECRET_TOKEN',refresh_token:'SECRET_REFRESH',created_at:expiry,expires_in:600});
 }
 if(url.endsWith('/sync/history')&&options.method==='POST'){historyAdds++;const payload=JSON.parse(options.body);assert(payload.shows[0].seasons[0].episodes[0].number===3);return json({added:{episodes:1}},201)}
 throw new Error('Unexpected mock URL '+url);
};
vm.runInNewContext(js,{Deno:{env:{get:name=>({SUPABASE_URL:'https://db.test',SUPABASE_SERVICE_ROLE_KEY:'service',TRAKT_CLIENT_ID:'client'})[name]},serve:fn=>handler=fn},fetch:fetchMock,Response,Request,URL,TextEncoder,Uint8Array,crypto:globalThis.crypto,btoa,Date,JSON,Number,String,Error});
async function call(data,auth='good'){const r=await handler(new Request('https://function.test',{method:'POST',headers:{Authorization:'Bearer '+auth,'Content-Type':'application/json'},body:JSON.stringify(data)}));return [r.status,await r.json()]}
(async()=>{
 assert.equal((await call({action:'status'},'bad'))[0],401);
 assert.equal((await call({action:'status'}))[1].connected,false);
 const start=(await call({action:'start'}))[1];
 const authorize=new URL(start.verification_url);
 assert.equal(authorize.origin,'https://auth.trakt.tv');
 assert.equal(authorize.searchParams.get('code_challenge_method'),'S256');
 assert(!JSON.stringify(start).includes(store.miflix_trakt_pending.state.verifier));
 assert.equal((await call({action:'poll'}))[1].pending,true);
 const state=authorize.searchParams.get('state');
 assert.equal((await handler(new Request('https://function.test?state=bad&code=a'))).status,400);
 assert.equal((await handler(new Request('https://function.test?state='+state+'&code=a'))).status,200);
 assert.equal((await handler(new Request('https://function.test?state='+state+'&code=a'))).status,410);
 const connected=(await call({action:'poll'}))[1];assert.equal(connected.connected,true);assert(!JSON.stringify(connected).includes('SECRET'));
 assert.equal((await call({action:'status'}))[1].connected,true);
 const body={action:'watched',tmdb:123,type:'series',season:2,episode:3,event:'test-1'};
 assert.equal((await call(body))[1].saved,true);assert.equal((await call(body))[1].duplicate,true);assert.equal(historyAdds,1);
 assert.equal((await call({...body,type:'live'}))[0],400);
 console.log('PASS: auth rejection, PKCE S256, server-only verifier/tokens, wrong state/replay rejection, pending/connected flow, episode history payload, duplicate protection, live exclusion');
})().catch(e=>{console.error(e);process.exitCode=1});
