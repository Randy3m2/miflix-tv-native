'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('assert'),{stripTypeScriptTypes}=require('node:module');
(async()=>{
 let handler,authCalls=0,listCalls=0,fileCalls=0;const calls=[];
 const text='1\n00:00:01,000 --> 00:00:03,000\nDialogue centre\n';
 const context=vm.createContext({URL,Request,Response,AbortSignal,Map,Set,Date,JSON,Number,String,Uint8Array,TextDecoder,Blob,DecompressionStream,Deno:{env:{get:k=>({TMDB_TOKEN:'test',SUPABASE_URL:'https://project.supabase.co',SUPABASE_ANON_KEY:'public'})[k]},serve:fn=>handler=fn},fetch:async(raw)=>{const u=new URL(raw);calls.push(u.href);
  if(u.pathname==='/auth/v1/user'){authCalls++;return Response.json({id:'owner'});}
  if(u.hostname==='api.themoviedb.org'&&u.pathname.endsWith('/external_ids'))return Response.json({imdb_id:'tt0133093'});
  if(u.hostname==='opensubtitles-v3.strem.io'){listCalls++;return Response.json({subtitles:[{lang:'eng',url:'https://dl.opensubtitles.org/en.srt'},{lang:'spa',url:'https://dl.opensubtitles.org/es.srt'},{lang:'es-419',url:'https://dl.opensubtitles.org/latino.srt'},{lang:'spa',url:'http://127.0.0.1/private.srt'},{lang:'eng',url:'https://dl.opensubtitles.org/redirect.srt'}]});}
  if(u.hostname==='dl.opensubtitles.org'){fileCalls++;return new Response(text);}
  throw Error('Unexpected upstream '+u.href);
 }});
 vm.runInContext(stripTypeScriptTypes(fs.readFileSync(process.argv[2],'utf8')),context);
 const call=async(body,auth=true)=>{const r=await handler(new Request('https://project.supabase.co/functions/v1/brunio-catalog',{method:'POST',headers:{...(auth?{Authorization:'Bearer test'}:{}),'Content-Type':'application/json'},body:JSON.stringify(body)}));return {status:r.status,data:await r.json()};};
 const first=await call({action:'subtitles',cloud:'tmdb:series:99',season:2,episode:7,language:'es'});assert.equal(first.status,200);assert.deepEqual(Array.from(first.data.tracks,t=>t.lang),['es-419','es','en']);assert(first.data.tracks.every(t=>t.vtt.includes('00:00:01.000 --> 00:00:03.000')));assert(calls.some(u=>u.includes('tt0133093:2:7')));assert.equal(fileCalls,3);
 await call({action:'subtitles',cloud:'tmdb:series:99',season:2,episode:7,language:'es'});assert.equal(listCalls,1);assert.equal(fileCalls,3);assert.equal(authCalls,2,'auth still checked for cached results');
 const english=await call({action:'subtitles',cloud:'tmdb:movie:550',language:'en'});assert.equal(english.data.tracks[0].lang,'en');
 assert.equal((await call({action:'subtitles',cloud:'tmdb:movie:550'},false)).status,401);
 assert.throws(()=>vm.runInContext("subtitleURL('https://dl.opensubtitles.org.evil.example/a')",context));assert.throws(()=>vm.runInContext("subtitleURL('https://user:pass@dl.opensubtitles.org/a')",context));
 assert.equal(calls.filter(u=>u.includes('127.0.0.1')).length,0);
 console.log('PASS: authenticated subtitle preloading, exact series episode, Spanish/English priority, 3-track bound, VTT conversion, warm cache and unsafe-host rejection');
})().catch(e=>{console.error(e);process.exitCode=1;});
