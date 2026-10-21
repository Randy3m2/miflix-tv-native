'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('assert'),path=require('path');
(async()=>{
 const root=process.argv[2],src=fs.readFileSync(path.join(root,'cinema.js'),'utf8');
 let handler,close=0,opened=0,downloads=0,calls=0,live=0;
 const frame={},current={frame:{contentWindow:frame},channel:'test-channel',owner:'owner',profile:'profile',target:{cloudId:'tmdb:series:42',type:'series',season:1,episode:1}},nodes=new Map();
 const c=vm.createContext({window:{addEventListener:(_name,fn)=>handler=fn},activeWebPlayer:current,session:{user:{id:'owner'}},selected:()=> 'profile',location:{origin:'https://brunio.example'},catalog:async body=>{calls++;return body.action==='details'?{item:{...current.target,seasons:[{number:1},{number:2}]}}:{episodes:[{season:1,episode:2,date:'2020-01-01'}],hasMore:false};},closeBrowserPlayer:()=>close++,openDetail:async item=>{opened++;c.currentDetail=item;},currentDetail:null,openDownloads:()=>downloads++,openLiveSources:()=>live++,todayLocal:()=> '2026-10-21',toast(){},cinemaWords:(_es,en)=>en,$:id=>{if(!nodes.has(id))nodes.set(id,{});return nodes.get(id);}});
 vm.runInContext(src.slice(src.indexOf('let cinemaNextBusy='),src.indexOf('if(items.length)render();')),c);
 const event={origin:'https://brunio.example',source:frame,data:{brunio:'player-next',channel:'test-channel'}};
 await handler({...event,origin:'https://evil.example'});await handler({...event,source:{}});await handler({...event,data:{...event.data,channel:'wrong'}});assert.equal(calls,0);
 await handler(event);assert.equal(close,1);assert.equal(opened,1);assert.equal(downloads,1);assert.equal(nodes.get('castEpisode').value,'2');
 await handler({...event,data:{...event.data,brunio:'player-sources'}});assert.equal(downloads,2);assert.equal(nodes.get('castEpisode').value,'1');
 c.activeWebPlayer={...current,target:{live:true,liveChannel:{name:'test'}}};await handler({...event,data:{...event.data,brunio:'player-sources'}});assert.equal(live,1);
 const sw=fs.readFileSync(path.join(root,'sw.js'),'utf8'),events={},cached=[];
 const swc=vm.createContext({self:{location:{origin:'https://brunio.example'},clients:{claim(){}},skipWaiting(){},addEventListener:(n,fn)=>events[n]=fn},URL,caches:{open:async()=>({addAll:async files=>cached.push(...files)}),keys:async()=>[]}});vm.runInContext(sw,swc);let install;events.install({waitUntil:p=>install=p});await install;assert(cached.includes('cinema.js?v=37'));assert(cached.includes('player-ui.js?v=37'));assert(!cached.includes('player.html'));
 for(const url of ['https://store.tb-cdn.io/dld?token=secret','https://brunio.example/player.html','https://brunio.example/cinema.js?token=secret']){let intercepted=false;events.fetch({request:{url,method:'GET',headers:{has:()=>false}},respondWith:()=>intercepted=true});assert.equal(intercepted,false);}
 console.log('PASS: player message origin/frame/channel isolation; next released episode; source change for VOD/live; public-shell caching excludes player/private source URLs.');
})().catch(e=>{console.error(e);process.exitCode=1;});
