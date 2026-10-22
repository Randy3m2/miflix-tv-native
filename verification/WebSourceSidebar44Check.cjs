'use strict';
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
let calls=[],posts=[],profile='p1',failLink=false;
const current={channel:'nonce',owner:'u1',profile:'p1',target:{cloudId:'tmdb:series:1',type:'series',title:'Test show',season:2,episode:3},frame:{src:'original-player',contentWindow:{postMessage:msg=>posts.push(msg)}}};
const ctx={setTimeout,clearTimeout,Promise,activeWebPlayer:current,session:{user:{id:'u1'}},selected:()=>profile,downloadMaxGb:5,language:'en',location:{origin:'https://brunio.example'},URL,Error,String,api:async(path,method,body)=>{calls.push(body);if(body.action==='sources')return {sources:[{ticket:'private-ticket',name:'1080p',filename:'test.mkv',provider:'TorBox',size:2e9}]};if(failLink)throw Error('Source unavailable');return {url:'https://cdn.example/movie',filename:'test.mkv'};},webPlayerAddress:()=> 'player.html#private'};
vm.createContext(ctx);vm.runInContext(fs.readFileSync(process.argv[2]||'web/source-bridge43.js','utf8'),ctx);
(async()=>{
 await ctx.openPlayerSources43(current,{requestId:'request-1'});assert.equal(current.frame.src,'original-player');assert.equal(calls.length,1);assert.equal(calls[0].season,2);assert.equal(calls[0].episode,3);assert(!JSON.stringify(posts).includes('private-ticket'));assert.equal(posts[0].sources.length,1);
 await ctx.selectPlayerSource43(current,{requestId:'wrong',id:'0'});assert.equal(calls.length,1);
 failLink=true;await ctx.selectPlayerSource43(current,{requestId:'request-1',id:'0'});assert.equal(current.frame.src,'original-player');assert.equal(posts.at(-1).error,'Source unavailable');
 failLink=false;await ctx.selectPlayerSource43(current,{requestId:'request-1',id:'0'});assert.equal(posts.at(-1).brunio,'player-source-ready43');assert.equal(current.frame.src,'original-player','navigation belongs to child only after user selection');
 const count=posts.length;profile='other';await ctx.selectPlayerSource43(current,{requestId:'request-1',id:'0'});assert.equal(posts.length,count);
 ctx.setTimeout=fn=>{queueMicrotask(fn);return 1};ctx.clearTimeout=()=>{};await assert.rejects(()=>ctx.sourceDeadline44(new Promise(()=>{})),/took too long/);
 console.log('PASS: deadline releases stuck requests; opening sources retains iframe, lookup uses exact episode, tickets stay in parent, selection required, link failures preserve player and profile/request guards reject stale selection.');
})().catch(e=>{console.error(e);process.exitCode=1;});
