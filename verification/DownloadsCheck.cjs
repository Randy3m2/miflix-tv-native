'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('assert'),{webcrypto}=require('crypto'),{stripTypeScriptTypes}=require('node:module');
(async()=>{
 let handler;const calls=[];const secret='PRIVATE_TORBOX_KEY',manifest='https://comet.feels.legal/private-config/manifest.json';
 const env={SUPABASE_URL:'https://project.supabase.co',SUPABASE_ANON_KEY:'anon',SUPABASE_SERVICE_ROLE_KEY:'private-runtime-key',TMDB_TOKEN:'tmdb-key'};
 const context=vm.createContext({URL,URLSearchParams,Request,Response,TextEncoder,TextDecoder,Uint8Array,Map,Set,Promise,Date,Number,String,Array,JSON,Error,AbortSignal,btoa,atob,crypto:webcrypto,Deno:{env:{get:key=>env[key]},serve:fn=>handler=fn},fetch:async(raw,options={})=>{
  const url=new URL(String(raw));calls.push({url:url.href,options});
  if(url.pathname==='/auth/v1/user')return Response.json({id:options.headers.Authorization==='Bearer other'?'other':'owner'});
  if(url.pathname==='/rest/v1/miflix_user_state')return Response.json([{state:{profiles:[{id:'adult'},{id:'brunio-kids',isKids:true}],privateSetup:{addonManifests:[manifest]}}}]);
  if(url.hostname==='api.themoviedb.org')return Response.json({imdb_id:'tt0137523'});
  if(url.pathname.endsWith('/stream/movie/tt0137523.json'))return Response.json({streams:[{name:'[TB⚡] Comet 1080p',title:'Movie',url:'https://comet.feels.legal/resolve/private',behaviorHints:{filename:'Movie.mkv',videoSize:5000000000}},{name:'[TB] Bad',url:'http://127.0.0.1/private'},{name:'[TB Download] waiting',url:'https://comet.feels.legal/waiting'},{name:'[TB] Large',url:'https://comet.feels.legal/large',behaviorHints:{videoSize:7000000000}},{name:'[TB] Unknown',url:'https://comet.feels.legal/unknown'},{name:'[RD] Other service',url:'https://comet.feels.legal/rd'}]});
  if(url.hostname==='comet.feels.legal'&&url.pathname==='/resolve/private')return new Response(null,{status:302,headers:{Location:'https://api.torbox.app/v1/api/torrents/requestdl?token='+secret+'&torrent_id=1&file_id=2&redirect=true'}});
  if(url.hostname==='api.torbox.app'){assert.equal(url.searchParams.get('redirect'),'false');return Response.json({success:true,data:'https://store-079.wnam.tb-cdn.io/dld/short-lived-signature?token=private-cdn-token'});}
  throw Error('Unexpected fetch: '+url.hostname);
 }});
 const code=stripTypeScriptTypes(fs.readFileSync(process.argv[2],'utf8'));vm.runInContext(code,context);
 const call=async(body,token='owner')=>{const response=await handler(new Request('https://project.supabase.co/functions/v1/brunio-downloads',{method:'POST',headers:{Authorization:'Bearer '+token,'Content-Type':'application/json'},body:JSON.stringify(body)}));return{status:response.status,body:await response.json()};};
 assert.equal((await handler(new Request('https://project.supabase.co',{method:'POST'}))).status,401);
 assert.equal((await call({profile:'brunio-kids',action:'sources',cloud:'tmdb:movie:550'})).status,403);
 const input={profile:'adult',action:'sources',cloud:'tmdb:movie:550',language:'en'};
 const sources=await call(input);assert.equal(sources.status,200);assert.equal(sources.body.sources.length,1);
 const serialized=JSON.stringify(sources.body);assert(!serialized.includes(secret));assert(!serialized.includes('private-config'));assert(!serialized.includes('resolve/private'));assert(!('url' in sources.body.sources[0]));
 assert.equal((await call({...input,maxGb:2})).body.sources.length,0);assert.equal((await call({...input,maxGb:10})).body.sources.length,2);assert.equal((await call({...input,maxGb:0})).body.sources.length,3);
 assert.equal(vm.runInContext("sourceSize({title:'Size: 1.5 GiB'})",context),1.5*1024**3);assert.equal(vm.runInContext("withinLimit(0,sizeLimit(undefined))",context),false);
 const count=calls.filter(x=>x.url.includes('/stream/')).length;await call(input);assert.equal(calls.filter(x=>x.url.includes('/stream/')).length,count,'source list cached');
 const ticket=sources.body.sources[0].ticket;
 assert.equal((await call({profile:'adult',action:'link',ticket},'other')).status,410,'ticket owner bound');
 const link=await call({profile:'adult',action:'link',ticket});assert.equal(link.status,200);assert.equal(link.body.url,'https://store-079.wnam.tb-cdn.io/dld/short-lived-signature?token=private-cdn-token');assert(!JSON.stringify(link.body).includes(secret));
 assert(!calls.some(x=>x.url.includes('.tb-cdn.')),'file bytes not fetched/proxied');
 assert.equal((await call({profile:'adult',action:'link',ticket:'bad.ticket'})).status,502);
 await assert.rejects(()=>vm.runInContext("finalLink('https://127.0.0.1/file',new Set())",context));
 await assert.rejects(()=>vm.runInContext("finalLink('https://torbox.app.evil.example/file',new Set())",context));
 await assert.rejects(()=>vm.runInContext("finalLink('https://cdn.torbox.app/file?apikey=private',new Set())",context));
 await assert.rejects(()=>vm.runInContext("finalLink('https://store.tb-cdn.io.evil.example/file',new Set())",context));
 console.log('PASS: authentication, Kids block, cached TorBox-only sources, encrypted owner-bound tickets, private key isolation, safe redirects, API-to-CDN resolution, unsafe host rejection and no file proxy');
})().catch(e=>{console.error(e);process.exitCode=1;});
