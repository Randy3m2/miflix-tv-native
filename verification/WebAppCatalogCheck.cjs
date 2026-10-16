'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('assert'),{stripTypeScriptTypes}=require('node:module');
(async()=>{
 let handler;const calls=[];
 const episode=n=>({id:n,season_number:1,episode_number:n,name:'Episode '+n,overview:'Text',air_date:n===52?'2999-01-01':'2020-01-01',still_path:'/still.jpg'});
 const context=vm.createContext({URL,Request,Response,AbortSignal,Date,Math,Deno:{env:{get:key=>({SUPABASE_URL:'https://project.supabase.co',SUPABASE_ANON_KEY:'anon',TMDB_TOKEN:'test'})[key]},serve:fn=>handler=fn},fetch:async raw=>{
  const u=new URL(String(raw));calls.push(u.pathname);if(u.pathname==='/auth/v1/user')return Response.json({id:'owner'});
  if(u.pathname==='/3/person/1/combined_credits')return Response.json({cast:Array.from({length:52},(_,i)=>({id:i+1,media_type:'movie',title:'Movie '+i,popularity:i,poster_path:'/p.jpg'}))});
  if(u.pathname==='/3/tv/99/season/1')return Response.json({episodes:Array.from({length:52},(_,i)=>episode(i+1))});
  if(u.pathname==='/3/movie/550'||u.pathname==='/3/tv/99')return Response.json({id:u.pathname.endsWith('99')?99:550,title:'Movie',name:'Show',genres:[{id:18}],seasons:[{season_number:0,episode_count:1,air_date:'2020-01-01'},{season_number:1,name:'Season 1',episode_count:52,air_date:'2020-01-01'},{season_number:2,name:'Future',episode_count:20,air_date:'2999-01-01'}],credits:{cast:[{id:1,name:'Actor',character:'Role',profile_path:'/actor.jpg'}]},images:{logos:[{file_path:'/logo.png'}]},recommendations:{results:[{id:2,title:'Related',poster_path:'/r.jpg'}]}});
  throw Error('Unexpected upstream '+u.pathname);
 }});
 vm.runInContext(stripTypeScriptTypes(fs.readFileSync(process.argv[2],'utf8')),context);
 const call=async body=>{const r=await handler(new Request('https://project.supabase.co/functions/v1/brunio-catalog',{method:'POST',headers:{Authorization:'Bearer test','Content-Type':'application/json'},body:JSON.stringify(body)}));return {status:r.status,data:await r.json()};};
 const full=await call({action:'details',cloud:'tmdb:series:99'});assert.equal(full.status,200);assert.equal(full.data.item.full,true);assert.equal(full.data.item.cast[0].name,'Actor');assert(full.data.item.logo.endsWith('/logo.png'));assert.equal(full.data.item.seasons.length,2);
 const first=await call({action:'episodes',cloud:'tmdb:series:99',season:1,page:1}),third=await call({action:'episodes',cloud:'tmdb:series:99',season:1,page:3});assert.equal(first.data.episodes.length,25);assert.equal(third.data.episodes.length,2);assert.equal(third.data.hasMore,false);assert.equal(calls.filter(x=>x==='/3/tv/99/season/1').length,1,'season payload cached');
 const credits=await call({action:'filmography',person:1,page:2});assert.equal(credits.data.items.length,25);assert.equal(credits.data.hasMore,true);
 const random=await call({action:'randomEpisode',cloud:'tmdb:series:99'});assert.equal(random.data.episode.season,1);assert(random.data.episode.episode<=51,'future episode excluded');
 assert.equal((await call({action:'episodes',cloud:'tmdb:movie:550',season:1})).status,400);assert.equal((await call({action:'details',cloud:'https://evil.example'})).status,400);
 console.log('PASS: rich metadata/cast/logo, 25-episode and filmography pages, season cache, random aired-only episode and invalid input rejection');
})().catch(e=>{console.error(e);process.exitCode=1;});
