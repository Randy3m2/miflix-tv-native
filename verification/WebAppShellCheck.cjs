'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('assert'),path=require('path');
(async()=>{
 const root=process.argv[2],events={},cached=[],self={location:{origin:'https://brunio.example'},clients:{claim:async()=>{}},skipWaiting:async()=>{},addEventListener:(name,fn)=>events[name]=fn};
 const context=vm.createContext({self,URL,Response,caches:{open:async()=>({addAll:async files=>cached.push(...files),put:async()=>{}}),keys:async()=>[],match:async()=>undefined,delete:async()=>true},fetch:async()=>Response.error()});
 vm.runInContext(fs.readFileSync(path.join(root,'sw.js'),'utf8'),context);
 let install;events.install({waitUntil:p=>install=p});await install;assert(cached.includes('experience.js?v=29'));assert(!cached.some(x=>x.includes('supabase')||x.includes('torbox')));
 const ignored=(url,auth=false)=>{let intercepted=false;events.fetch({request:{url,method:'GET',headers:{has:()=>auth}},respondWith:()=>intercepted=true});assert.equal(intercepted,false,url);};
 ignored('https://project.supabase.co/rest/v1/miflix_user_state');ignored('https://store.tb-cdn.io/dld/movie?token=secret');ignored('https://brunio.example/player.html');ignored('https://brunio.example/app.js?token=secret');ignored('https://brunio.example/index.html',true);
 const manifest=JSON.parse(fs.readFileSync(path.join(root,'manifest.webmanifest'),'utf8'));assert.equal(manifest.display,'standalone');for(const icon of manifest.icons)assert(fs.existsSync(path.join(root,icon.src)));
 const html=fs.readFileSync(path.join(root,'index.html'),'utf8');assert(html.includes('experience.js?v=29'));assert(html.includes('manifest.webmanifest'));
 console.log('PASS: installable manifest/icons, shell assets, and exclusion of authenticated data/media/private-query requests from service-worker cache');
})().catch(e=>{console.error(e);process.exitCode=1;});
