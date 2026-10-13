import { readFileSync } from 'node:fs';
import { stripTypeScriptTypes } from 'node:module';
import vm from 'node:vm';
import assert from 'node:assert/strict';
const source=stripTypeScriptTypes(readFileSync(new URL('../supabase/functions/brunio-episode-reminders/index.ts',import.meta.url),'utf8'));
let handler, sends=0;
const delivered=new Set();
const today=new Date().toISOString().slice(0,10);
let failMail=false;
const environment={EPISODE_CRON_SECRET:'secret',SUPABASE_URL:'https://test',SUPABASE_SERVICE_ROLE_KEY:'service',TMDB_TOKEN:'token',RESEND_API_KEY:'mail',EPISODE_EMAIL_FROM:'BruniO <test@example.com>',EPISODE_TIMEZONE:'UTC'};
const fetch=async(url, options={})=>{
 const response=(data,status=200)=>new Response(status===204?null:JSON.stringify(data),{status});
 if(url.includes('brunio_episode_recipients')) return response([{user_id:'u1',email:'a@example.com',name:'Randy',show_id:1}]);
 if(url.includes('/tv/1/season/1')) return response({episodes:[{id:10,air_date:today,season_number:1,episode_number:1},{id:11,air_date:today,season_number:1,episode_number:2},{id:12,air_date:'2000-01-01',season_number:1,episode_number:3}]});
 if(url.includes('/tv/1')) return response({name:'Serie',last_episode_to_air:{air_date:today,season_number:1}});
 if(url.includes('api.resend.com')) {if(failMail)return response({},429);sends++;assert.match(JSON.parse(options.body).text,/Hello Randy/);return response({id:'mail'});}
 if(url.includes('brunio_episode_mail')) {
  if(options.method==='POST'){delivered.add(JSON.parse(options.body).episode_id);return response(null,204);}
  const id=Number(url.match(/episode_id=eq\.(\d+)/)[1]);return response(delivered.has(id)?[{episode_id:id}]:[]);
 }
 throw Error('Unexpected request');
};
vm.runInNewContext(source,{Deno:{env:{get:key=>environment[key]},serve:fn=>handler=fn},fetch,Response,URL,AbortSignal,Intl,Date,Map,Set,JSON,Error});
const call=()=>handler(new Request('https://test',{method:'POST',headers:{'x-cron-secret':'secret'}}));
assert.equal((await handler(new Request('https://test',{method:'POST'}))).status,401);
assert.equal((await handler(new Request('https://test'))).status,405);
let result=await call();assert.equal(result.status,200);assert.equal(sends,2);
await call();assert.equal(sends,2,'No duplicates on repeated runs');
delivered.clear();failMail=true;result=await call();assert.equal(result.status,503);assert.equal(delivered.size,0,'Failed deliveries must remain retryable');
failMail=false;await call();assert.equal(sends,4);
console.log('PASS: authentication, multi-episode release, old episode exclusion, deduplication, email failure and retry');
