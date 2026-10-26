'use strict';
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const code=fs.readFileSync(process.argv[2]||'web/source-bridge43.js','utf8');
async function run(result,{live=false,stale=false}={}){
 const replies=[],calls=[];
 const current={owner:'user',profile:'profile',channel:'channel',target:live?{live:true,liveChannel:{streams:[{url:'https://example.com/live.m3u8',name:'Live'}]}}:{cloudId:'movie:123'}};
 const ctx={activeWebPlayer:current,session:{user:{id:'user'}},selected:()=>stale?'other':'profile',downloadMaxGb:10,language:'en',location:{origin:'https://example.com'},setTimeout,clearTimeout,
 api:async(...args)=>{calls.push(args);return result;},publicLiveURL:x=>new URL(x),liveCastSource:s=>s.url};
 current.frame={contentWindow:{postMessage:m=>replies.push(m)}};
 vm.createContext(ctx);vm.runInContext(code,ctx);
 await ctx.openPlayerSources43(current,{requestId:'request'});
 return {replies,calls};
}
(async()=>{
 const data={sourcePolicy:'torrentio-comet-v53',sources:[{providerId:'torrentio',name:'D'},{providerId:'other',name:'X'},{providerId:'comet',name:'M'}]};
 let r=await run(data);
 assert.equal(r.calls[0][2].sourcePolicy,data.sourcePolicy);
 assert.deepEqual(Array.from(r.replies[0].sources,s=>s.name),['M','D']);
 assert.equal(r.replies[0].requestId,'request');
 r=await run({sourcePolicy:'old',sources:[]});assert.match(r.replies[0].error,/Update the brunio-downloads/);
 r=await run({...data,sources:[]});assert.equal(r.replies[0].sources.length,0);
 r=await run(data,{live:true});assert.equal(r.calls.length,0);assert.equal(r.replies[0].sources[0].name,'Live');
 r=await run(data,{stale:true});assert.equal(r.replies.length,0);
 console.log('PASS: picker without features46, provider policy/order/filter, empty results, Live TV, stale profile.');
})().catch(e=>{console.error(e);process.exitCode=1;});
