import fs from 'node:fs';import vm from 'node:vm';import assert from 'node:assert/strict';import {stripTypeScriptTypes} from 'node:module';import {webcrypto} from 'node:crypto';
let handler,calls=[],providerMode='ok';const response=data=>new Response(JSON.stringify(data));
const ctx={URL,Response,Request,TextEncoder,TextDecoder,Uint8Array,AbortSignal,Set,Map,Promise,Number,String,Error,Date,JSON,Math,crypto:webcrypto,btoa,atob,Deno:{env:{get:k=>({SUPABASE_URL:'https://db.test',SUPABASE_ANON_KEY:'anon',SUPABASE_SERVICE_ROLE_KEY:'test-private',TMDB_TOKEN:'test'})[k]},serve:f=>handler=f},fetch:async(raw,options={})=>{const u=new URL(raw);calls.push(u.hostname);
if(['x.pepo.ar','mg.homelabx.qzz.io'].includes(u.hostname)){
 assert.equal(options.headers.Accept,'application/json');assert.equal(options.headers['User-Agent'],'BruniO/RP12');
 assert.equal(options.headers.Authorization,undefined);assert.equal(options.redirect,'manual');
 if(providerMode==='http')return new Response(null,{status:403});
 if(providerMode==='redirect'&&!u.pathname.startsWith('/moved/'))return new Response(null,{status:302,headers:{Location:'/moved'+u.pathname}});
 if(providerMode==='unsafe')return new Response(null,{status:302,headers:{Location:'https://127.0.0.1/private'}});
 if(providerMode==='network')throw Error('Sensitive upstream URL must never reach client');
}
if(u.pathname==='/auth/v1/user')return response({id:'owner'});if(u.pathname.startsWith('/rest/'))return response([{state:{profiles:[{id:'p'}],privateSetup:{addonManifests:['https://torrentio.strem.fun/manifest.json']}}}]);if(u.hostname==='api.themoviedb.org')return response({imdb_id:'tt0468569'});if(u.hostname==='x.pepo.ar')return response({streams:[{name:'RenTV',url:'https://cdn.example.com/movie.ts'},{url:'https://127.0.0.1/a'},{url:'http://cdn.example.com/insecure'},{url:'https://cdn.example.com/torrent',infoHash:'abc'}]});if(u.hostname==='mg.homelabx.qzz.io')return response({streams:[{name:'Deepflix',url:'https://media.example.com/movie.mp4',behaviorHints:{videoSize:2e9}}]});throw Error('Unexpected network '+u.hostname);}};
vm.createContext(ctx);vm.runInContext(stripTypeScriptTypes(fs.readFileSync(process.argv[2],'utf8'),{mode:'strip'}),ctx);
async function run(body){const r=await handler(new Request('https://function.test',{method:'POST',headers:{Authorization:'Bearer test','Content-Type':'application/json'},body:JSON.stringify({profile:'p',language:'en',...body})}));return {status:r.status,data:await r.json()};}
let result=await run({action:'sources',cloud:'tmdb:movie:155',maxGb:5});assert.equal(result.status,200);assert.deepEqual(result.data.sources.map(s=>s.provider),['Mandanguita TV','Deepflix']);assert(!calls.some(h=>h.includes('torrentio')||h.includes('comet')));assert.equal(result.data.sources[0].size,0);assert(!JSON.stringify(result.data).includes('https://cdn.example.com'));
let source=result.data.sources[0];result=await run({action:'link',ticket:source.ticket,maxGb:5});assert.equal(result.data.url,'https://cdn.example.com/movie.ts');assert(!calls.includes('cdn.example.com'));
result=await run({action:'link',ticket:source.ticket,maxGb:5,profile:'different'});assert.equal(result.status,403);
result=await run({action:'sources',cloud:'tmdb:movie:155',maxGb:1});assert.deepEqual(result.data.sources.map(s=>s.provider),['Mandanguita TV']);
console.log('PASS: factory providers/order, unknown size exception, direct HTTPS, no Torrentio/Comet, sealed tickets, profile isolation, size limit');

providerMode='redirect';result=await run({action:'sources',cloud:'tmdb:series:1399',season:2,episode:3,refresh:true});assert.equal(result.status,200);assert.equal(result.data.sources.length,2);
providerMode='http';result=await run({action:'sources',cloud:'tmdb:movie:155',refresh:true});assert.equal(result.status,502);assert(result.data.failures.every(f=>f.code==='PROVIDER_HTTP_403'));assert.match(result.data.error,/Mandanguita TV/);
providerMode='unsafe';result=await run({action:'sources',cloud:'tmdb:movie:155',refresh:true});assert.equal(result.status,502);assert(result.data.failures.every(f=>f.code==='PROVIDER_REDIRECT_HOST'));assert(!calls.includes('127.0.0.1'));
providerMode='network';result=await run({action:'sources',cloud:'tmdb:movie:155',refresh:true});assert.equal(result.status,502);assert(result.data.failures.every(f=>f.code==='PROVIDER_NETWORK'));assert(!JSON.stringify(result.data).includes('Sensitive upstream'));
console.log('PASS: TV request headers, approved redirects for episodes, HTTP diagnostics, blocked unapproved redirect, no credentials or raw upstream errors exposed.');
