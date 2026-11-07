'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict');
class Element {append(){}addEventListener(){}}
const snapshot={state:{profiles:[{id:'p',name:'Test'}],lastProfileRP83:'p',privateSetup:{tmdbToken:'keep',torrentioManifest:'https://torrentio.strem.fun/private/manifest.json',addonManifests:[]}},updated_at:'2026-11-07T00:00:00Z'};
let calls=[],response=[snapshot],writeResult=[{profile_id:'__account__'}];
const c=vm.createContext({URL,Date,JSON,Set,Error,profiles:[{id:'legacy',name:'Existing profile'}],document:{querySelector:()=>new Element()},session:{user:{id:'owner'}},$:()=>new Element(),button:()=>new Element(),logout(){},words:en=>en,t:s=>s,api:async(path,method,body,retry,headers)=>{calls.push({path,method,body,headers});return method==='PATCH'||method==='POST'?writeResult:structuredClone(response);}});
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
 // Fresh accounts can save without first configuring anything on TV/mobile.
 response=[];writeResult=[{profile_id:'__account__'}];calls=[];
 await c.saveWebAddons('owner',['https://torrentio.strem.fun/key/manifest.json'],[]);
 write=calls.find(x=>x.method==='POST');assert(write);assert.equal(write.body.user_id,'owner');assert.equal(write.body.profile_id,'__account__');assert.equal(write.body.state.profiles[0].id,'legacy');assert.equal(write.headers.Prefer,'resolution=ignore-duplicates,return=representation');assert.equal(write.body.state.privateSetup.addonManifests.length,1);
 // A concurrent account creation must never be overwritten by an upsert.
 writeResult=[];await assert.rejects(c.saveWebAddons('owner',[],[]));
 // Existing rows with no timestamp use SQL IS NULL, not an invalid eq.null.
 response=[{state:{profiles:[{id:'preserve'}]},updated_at:null}];writeResult=[{}];calls=[];
 await c.saveWebAddons('owner',[],[]);write=calls.find(x=>x.method==='PATCH');assert(write.path.endsWith('&updated_at=is.null'));assert.equal(write.body.state.profiles[0].id,'preserve');
 // Existing nullable state also initializes safely.
 response=[{state:null,updated_at:'2026-11-07T00:00:00Z'}];await c.saveWebAddons('owner',[],[]);
 response=[];c.profiles=[];calls=[];await c.saveWebAddons('owner',['https://comet.feels.legal/key/manifest.json'],[]);write=calls.find(x=>x.method==='POST');assert.equal(write.body.state.profiles.length,0);
 console.log('Web add-ons: URL policy, account isolation, preserved settings, removal and concurrent-write checks passed.');
})().catch(e=>{console.error(e);process.exitCode=1;});
