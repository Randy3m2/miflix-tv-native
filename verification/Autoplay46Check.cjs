const fs=require('fs'),vm=require('vm'),assert=require('assert');
let currentScope='u:p',opened=[],resolutions=0,notices=[],listeners={},nodes={friendsRefresh:{},detail:{open:true}};
const ctx={console,URL,Map,Promise,Date,Math,Number,String,Error,crypto:require('crypto').webcrypto,language:'en',session:{user:{id:'u'}},selected:()=>currentScope.split(':')[1],$:()=>({}),el:()=>({}),sourceDeadline44:p=>p,publicLiveURL:raw=>new URL(raw),downloadMaxGb:5,loadFriends:()=>{},closeBrowserPlayer:()=>{},window:{addEventListener:(name,f)=>listeners[name]=f},location:{origin:'https://app.test'},toast:x=>notices.push(x),currentDetail:{cloudId:'tmdb:movie:1'},activeWebPlayer:null,api:async()=>({})};
ctx.$=id=>nodes[id]||{};ctx.openBrowserPlayer=(url,title,poster,target)=>{opened.push(url);ctx.activeWebPlayer={target,owner:'u',profile:'p',frame:{contentWindow:{}},channel:'c'};};vm.createContext(ctx);vm.runInContext(fs.readFileSync(process.argv[2],'utf8'),ctx);
(async()=>{
vm.runInContext("resolve46=async(source)=>{if(source.fail)throw Error('failed');return {url:'https://cdn.test/'+source.id};};manualSources46=async()=>{}",ctx);
const q=()=>vm.runInContext("({sources:[{id:1,fail:true},{id:2,fail:true},{id:3},{id:4},{id:5},{id:6}],index:0,target:{cloudId:'tmdb:movie:1'},scope:scope46(),run:sourceRun46,busy:false})",ctx);
let queue=q();await ctx.advance46(queue);assert.equal(queue.index,3);assert.equal(opened.length,1);assert(opened[0].endsWith('/3'));
let current=ctx.activeWebPlayer;await ctx.advance46(queue,current);assert.equal(queue.index,4);assert.equal(opened.length,2);
// AutoPlay trims the queue to five; no sixth media failure attempt.
queue=q();queue.sources=queue.sources.slice(0,5);for(let i=0;i<5;i++)await ctx.advance46(queue,ctx.activeWebPlayer);assert.equal(queue.index,5);assert(notices.length>0);
// Stop fallback on the first actual playing event, regardless of audio evidence.
current=ctx.activeWebPlayer;current.auto46={};listeners.message({origin:ctx.location.origin,source:current.frame.contentWindow,data:{channel:'c',brunio:'player-started46'}});assert.equal(current.auto46,null);
// Ignore stale frames and another profile.
current.auto46={};listeners.message({origin:ctx.location.origin,source:{},data:{channel:'c',brunio:'player-started46'}});assert(current.auto46);const stale=q();currentScope='u:other';const n=opened.length;await ctx.advance46(stale);assert.equal(opened.length,n);
console.log('PASS: failed link resolution, media fallback, five-source cap, playing stops fallback, stale frame/profile guards');
})().catch(e=>{console.error(e);process.exitCode=1;});
