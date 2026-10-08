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
 if(url.endsWith('/oauth/device/code'))return json({device_code:'PRIVATE',user_code:'PUBLIC',verification_url:'https://trakt.tv/activate',expires_in:600,interval:5});
 if(url.endsWith('/oauth/device/token'))return oauthPending?json({},400):json({access_token:'SECRET_TOKEN',refresh_token:'SECRET_REFRESH',created_at:expiry,expires_in:600});
 if(url.endsWith('/sync/history')&&options.method==='POST'){historyAdds++;const payload=JSON.parse(options.body);assert(payload.shows[0].seasons[0].episodes[0].number===3);return json({added:{episodes:1}},201)}
 throw new Error('Unexpected mock URL '+url);
};
vm.runInNewContext(js,{Deno:{env:{get:name=>({SUPABASE_URL:'https://db.test',SUPABASE_SERVICE_ROLE_KEY:'service',TRAKT_CLIENT_ID:'client',TRAKT_CLIENT_SECRET:'secret'})[name]},serve:fn=>handler=fn},fetch:fetchMock,Response,Date,JSON,Number,String,Error});
async function call(data,auth='good'){const r=await handler(new Request('https://function.test',{method:'POST',headers:{Authorization:'Bearer '+auth,'Content-Type':'application/json'},body:JSON.stringify(data)}));return [r.status,await r.json()]}
(async()=>{
 assert.equal((await call({action:'status'},'bad'))[0],401);
 assert.equal((await call({action:'status'}))[1].connected,false);
 const start=(await call({action:'start'}))[1];assert.equal(start.user_code,'PUBLIC');assert(!JSON.stringify(start).includes('PRIVATE'));
 assert.equal((await call({action:'poll'}))[1].pending,true);oauthPending=false;
 const connected=(await call({action:'poll'}))[1];assert.equal(connected.connected,true);assert(!JSON.stringify(connected).includes('SECRET'));
 assert.equal((await call({action:'status'}))[1].connected,true);
 const body={action:'watched',tmdb:123,type:'series',season:2,episode:3,event:'test-1'};
 assert.equal((await call(body))[1].saved,true);assert.equal((await call(body))[1].duplicate,true);assert.equal(historyAdds,1);
 assert.equal((await call({...body,type:'live'}))[0],400);
 console.log('PASS: auth rejection, server-only OAuth code/tokens, pending/connected flow, episode history payload, duplicate protection, live exclusion');
})().catch(e=>{console.error(e);process.exitCode=1});
