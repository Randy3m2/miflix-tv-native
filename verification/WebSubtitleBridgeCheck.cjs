'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('assert');
(async()=>{
 const src=fs.readFileSync(process.argv[2],'utf8'),listeners=[],posted=[];let calls=0;
 const frameWindow={postMessage:(data,origin)=>posted.push({data,origin})},current={target:{cloudId:'tmdb:series:99',season:2,episode:7},frame:{contentWindow:frameWindow},channel:'channel',owner:'owner',profile:'profile'};
 const c=vm.createContext({window:{addEventListener:(kind,fn)=>listeners.push(fn)},activeWebPlayer:current,session:{user:{id:'owner'}},selected:()=> 'profile',language:'es',location:{origin:'https://brunio.example'},api:async(path,method,body)=>{calls++;assert.equal(body.season,2);assert.equal(body.episode,7);assert.equal(body.action,'subtitles');return {tracks:[{lang:'es',vtt:'WEBVTT'}]};}});
 vm.runInContext(src.slice(src.indexOf('const onlineSubtitleCache=')),c);const event={origin:'https://brunio.example',source:frameWindow,data:{brunio:'subtitle-request',channel:'channel'}};
 await listeners[0]({...event,origin:'https://evil.example'});assert.equal(calls,0);
 await listeners[0]({...event,data:{...event.data,channel:'wrong'}});assert.equal(calls,0);
 await listeners[0](event);await listeners[0](event);assert.equal(calls,1);assert.equal(posted.length,2);assert(posted.every(row=>row.origin==='https://brunio.example'&&row.data.channel==='channel'));
 console.log('PASS: subtitle bridge validates origin/source/channel, uses exact episode and reuses per-profile cached payload');
})().catch(e=>{console.error(e);process.exitCode=1});
