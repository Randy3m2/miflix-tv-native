const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict'),{stripTypeScriptTypes}=require('node:module');
const source=stripTypeScriptTypes(fs.readFileSync('supabase/functions/miflix-trakt/index.ts','utf8'));
let handler,mode='normal',paths=[];
const json=(data,status=200)=>new Response(JSON.stringify(data),{status});
const fetch=async(url,options={})=>{
 if(url.endsWith('/auth/v1/user'))return options.headers.Authorization==='Bearer good'?json({id:'user'}):json({},401);
 if(!url.startsWith('https://api.trakt.tv'))throw Error('Unexpected OAuth table access');
 assert.equal(options.headers['trakt-api-key'],'client');assert(!options.headers.Authorization);paths.push(url);
 if(url.includes('/search/tmdb/')) {
  if(mode==='error')return json({},429);
  if(mode==='none')return json([]);
  return json([{type:'movie',movie:{ids:{tmdb:123,trakt:22}}},{type:'show',show:{ids:{tmdb:123,trakt:33}}}]);
 }
 if(url.includes('/comments/likes?'))return json([{id:1,comment:'Good movie!',likes:5,spoiler:false,user:{username:'reviewer'},created_at:'2026-10-13T01:00:00Z'},{id:2,comment:'[SpOiLeR]Ending![/spoiler]',spoiler:false,user:{name:'Person'},likes:2},{id:3,comment:'Secret ending',spoiler:true},{id:4,comment:'   '}]);
 throw Error('Unexpected endpoint');
};
vm.runInNewContext(source,{Deno:{env:{get:k=>({SUPABASE_URL:'https://db.test',SUPABASE_SERVICE_ROLE_KEY:'service',TRAKT_CLIENT_ID:'client'})[k]},serve:fn=>handler=fn},fetch,Response,Request,URL,Date,JSON,Number,String,Error});
async function call(type='movie',auth='good',tmdb=123){const r=await handler(new Request('https://function',{method:'POST',headers:{Authorization:'Bearer '+auth},body:JSON.stringify({action:'comments',type,tmdb})}));return {status:r.status,data:await r.json()};}
(async()=>{
 assert.equal((await call('movie','bad')).status,401);assert.equal((await call('live')).status,400);assert.equal((await call('movie','good',-1)).status,400);
 let r=await call();assert.equal(r.status,200);assert.equal(r.data.items.length,3);assert.equal(r.data.items[0].author,'reviewer');assert.equal(r.data.items[1].spoiler,true);assert.equal(r.data.items[2].spoiler,true);assert(paths.some(p=>p.includes('/movies/22/comments/likes')));
 r=await call('series');assert.equal(r.status,200);assert(paths.some(p=>p.includes('/shows/33/comments/likes')));
 mode='none';r=await call();assert.deepEqual(r.data.items,[]);mode='error';assert.equal((await call()).status,502);
 console.log('PASS: app auth, public comments without OAuth, title/type validation, movie/show mapping, no comments, spoiler protection, upstream failure');
})().catch(e=>{console.error(e);process.exitCode=1;});
