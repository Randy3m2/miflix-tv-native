'use strict';
// Public app shell only. Never cache Supabase, TorBox media, authenticated data or private links.
const CACHE='brunio-shell-41',FILES=['./','index.html','app.js?v=41','experience.js?v=41','live.js?v=41','style.css?v=41','cinema.css?v=41','cinema.js?v=41','player-ui.css?v=41','player-ui.js?v=41','audio-state.js?v=41','search38.js?v=41','home39.js?v=41','home-compat41.js?v=41','manifest.webmanifest','brunio-logo.png','icon-192.png','icon-512.png'];
self.addEventListener('install',event=>event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(FILES)).then(()=>self.skipWaiting())));
self.addEventListener('activate',event=>event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(key=>key.startsWith('brunio-shell-')&&key!==CACHE).map(key=>caches.delete(key)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',event=>{
 const request=event.request,url=new URL(request.url);
 if(request.method!=='GET'||url.origin!==self.location.origin||request.headers.has('Authorization'))return;
 const path=url.pathname.split('/').pop()||'index.html';if(!['index.html','app.js','experience.js','live.js','live-relay-config.js','hls.min.js','style.css','cinema.css','cinema.js','player-ui.css','player-ui.js','audio-state.js','search38.js','home39.js','home-compat41.js','manifest.webmanifest','brunio-logo.png','icon-192.png','icon-512.png'].includes(path)||[...url.searchParams.keys()].some(key=>key!=='v'))return;
 event.respondWith(fetch(request).then(async response=>{if(response.ok&&response.type==='basic'){const cache=await caches.open(CACHE);await cache.put(request,response.clone());}return response;}).catch(()=>caches.match(request).then(cached=>cached||((path==='index.html')?caches.match('./'):Response.error()))));
});
