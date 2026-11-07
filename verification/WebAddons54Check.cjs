'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict');
class Element {append(){}addEventListener(){}}
const snapshot={state:{profiles:[{id:'p',name:'Test'}],lastProfileRP83:'p',privateSetup:{tmdbToken:'keep',torrentioManifest:'https://torrentio.strem.fun/private/manifest.json',addonManifests:[]}},updated_at:'2026-11-07T00:00:00Z'};
let calls=[],response=[snapshot],writeResult=[{profile_id:'__account__'}];
const c=vm.createContext({URL,Date,JSON,Set,Error,document:{querySelector:()=>new Element()},session:{user:{id:'owner'}},$:()=>new Element(),button:()=>new Element(),logout(){},words:en=>en,t:s=>s,api:async(path,method,body,retry,headers)=>{calls.push({path,method,body,headers});return method==='PATCH'?writeResult:structuredClone(response);}});
vm.runInContext(fs.readFileSync('web/addons.js','utf8'),c);
(async()=>{
 for(const value of ['http://example.com/manifest.json','https://127.0.0.1/manifest.json','https://user:pass@example.com/manifest.json','https://router.local/manifest.json','https://example.com/configure'])assert.throws(()=>c.addonURL(value));
 assert.equal(c.addonURL('stremio://torrentio.strem.fun/key/manifest.json'),'https://torrentio.strem.fun/key/manifest.json');
 await c.saveWebAddons('owner',['https://comet.feels.legal/key/manifest.json']);
 let write=calls.find(x=>x.method==='PATCH');assert(write.path.includes('updated_at=eq.'));assert.equal(write.headers.Prefer,'return=representation');assert.equal(write.body.state.privateSetup.tmdbToken,'keep');assert.equal(write.body.state.profiles[0].name,'Test');assert.equal(write.body.state.lastProfileRP83,'p');assert.equal(write.body.state.privateSetup.torrentioManifest,'https://comet.feels.legal/key/manifest.json');
 calls=[];await c.saveWebAddons('owner',[]);write=calls.find(x=>x.method==='PATCH');assert.equal(write.body.state.privateSetup.torrentioManifest,'');assert.equal(write.body.state.privateSetup.addonManifests.length,0);
 calls=[];await assert.rejects(c.saveWebAddons('owner',[],['https://changed.example.com/manifest.json']));assert(!calls.some(x=>x.method==='PATCH'));
 writeResult=[];await assert.rejects(c.saveWebAddons('owner',[]));
 calls=[];await assert.rejects(c.saveWebAddons('another-user',[]));assert.equal(calls.length,0);
 console.log('Web add-ons: URL policy, account isolation, preserved settings, removal and concurrent-write checks passed.');
})().catch(e=>{console.error(e);process.exitCode=1;});
