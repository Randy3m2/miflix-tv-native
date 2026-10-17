'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('assert');
class Node {
 constructor(){this.children=[];this.handlers={};this.hidden=false;this.value='';this.paused=false;this.loads=0;}
 addEventListener(name,fn){this.handlers[name]=fn;} replaceChildren(...nodes){this.children=[...nodes];} remove(){} append(...nodes){this.children.push(...nodes);} setAttribute(){} removeAttribute(name){delete this[name];} querySelector(){return this.children[0]||null;} focus(){} select(){}
 play(){this.paused=false;return Promise.resolve();} pause(){this.paused=true;} load(){this.loads++;}
}
function load(html,src,agent='Mozilla Android Samsung',extra={},hlsType=null){
 const nodes=new Map(),get=id=>{if(!nodes.has(id)){const node=new Node();node.hidden=['vlc','android'].includes(id);if(id==='video'&&agent.includes('AppleTest')){node.webkitShowPlaybackTargetPicker=()=>{node.pickerCalls=(node.pickerCalls||0)+1;};node.canPlayType=type=>type==='application/vnd.apple.mpegurl'?'probably':'';}nodes.set(id,node);}return nodes.get(id);},events={},history=[];
 const context=vm.createContext({URL,URLSearchParams,Blob,Error,Number,String,document:{getElementById:get,documentElement:{},head:new Node(),createElement:()=>new Node()},location:{hash:'#'+new URLSearchParams({src,title:'My movie',lang:'en',autoplay:'1',poster:'https://evil.example/poster.jpg',...extra}),pathname:'/player.html',search:''},history:{replaceState:(...args)=>history.push(args)},navigator:{userAgent:agent,platform:'Linux',maxTouchPoints:1},window:{Hls:hlsType,addEventListener:(name,fn)=>events[name]=fn},setTimeout:()=>1,clearTimeout(){},setInterval:()=>1,clearInterval(){},fetch(){throw Error('No API/file proxy expected');}});
 vm.runInContext(html.match(/<script>([\s\S]*?)<\/script>/)[1],context);return{context,get,events,history};
}
(async()=>{
 const html=fs.readFileSync(process.argv[2],'utf8'),src='https://store-079.wnam.tb-cdn.io/movie.mp4?token=private';
 const apple=load(html,src,'Mozilla iPad Safari AppleTest'),v=apple.get('video');
 assert.equal(v.src,src);assert.equal(v.autoplay,false);assert.equal(v.playsInline,true);assert.equal(v.controls,true);assert.equal(v.disableRemotePlayback,false);
 assert.equal(apple.get('castLabel').textContent,'AirPlay');assert(apple.get('castIcon').innerHTML.includes('path'));
 v.handlers.webkitplaybacktargetavailabilitychanged({availability:'not-available'});assert(apple.get('cast').title.includes('No AirPlay'));
 v.handlers.webkitplaybacktargetavailabilitychanged({availability:'available'});await apple.get('cast').onclick();assert.equal(v.pickerCalls,1);
 v.webkitCurrentPlaybackTargetIsWireless=true;v.handlers.webkitcurrentplaybacktargetiswirelesschanged();assert(apple.get('castLabel').textContent.includes('AirPlay · Connected'));
 let full=0;v.requestFullscreen=()=>{throw Error('Prefer native iOS fullscreen');};v.webkitEnterFullscreen=()=>{full++;};apple.get('fullscreen').onclick();assert.equal(full,1);
 v.error={code:4};v.handlers.error();assert(apple.get('status').textContent.includes('H.264'));assert(apple.get('status').textContent.includes('MEDIA_4'));
 const hls=load(html,'https://store-079.wnam.tb-cdn.io/movie.m3u8','Mozilla iPad Safari AppleTest');assert.equal(hls.get('video').src,'https://store-079.wnam.tb-cdn.io/movie.m3u8');assert.equal(vm.runInContext('hlsLibrary',hls.context),null,'Safari native HLS avoids JS media engine');
 apple.events.pagehide();assert(v.paused);assert.equal(v.src,undefined);
 console.log('PASS: Apple manual play, native inline/fullscreen, AirPlay icon/picker/availability/connection, codec errors, native VOD HLS and cleanup.');
})().catch(e=>{console.error(e);process.exitCode=1});
