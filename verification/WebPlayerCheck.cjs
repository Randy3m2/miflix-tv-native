'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('assert');
class Node {
 constructor(){this.children=[];this.handlers={};this.hidden=false;this.value='';this.paused=false;this.loads=0;}
 addEventListener(name,fn){this.handlers[name]=fn;} replaceChildren(...nodes){this.children=[...nodes];} remove(){} append(...nodes){this.children.push(...nodes);} setAttribute(){} removeAttribute(name){delete this[name];} querySelector(){return this.children[0]||null;} focus(){} select(){}
 play(){this.paused=false;return Promise.resolve();} pause(){this.paused=true;} load(){this.loads++;}
}
function load(html,src,agent='Mozilla Android Samsung',extra={},hlsType=null){
 const nodes=new Map(),get=id=>{if(!nodes.has(id)){const node=new Node();node.hidden=['vlc','android'].includes(id);nodes.set(id,node);}return nodes.get(id);},events={},history=[];
 const context=vm.createContext({URL,URLSearchParams,Blob,Error,Number,String,document:{getElementById:get,documentElement:{},head:new Node(),createElement:()=>new Node()},location:{hash:'#'+new URLSearchParams({src,title:'My movie',lang:'en',autoplay:'1',poster:'https://evil.example/poster.jpg',...extra}),pathname:'/player.html',search:''},history:{replaceState:(...args)=>history.push(args)},navigator:{userAgent:agent,platform:'Linux',maxTouchPoints:1},window:{Hls:hlsType,addEventListener:(name,fn)=>events[name]=fn},setTimeout:()=>1,clearTimeout(){},setInterval:()=>1,clearInterval(){},fetch(){throw Error('No API/file proxy expected');}});
 vm.runInContext(html.match(/<script>([\s\S]*?)<\/script>/)[1],context);return{context,get,events,history};
}
(async()=>{
 const html=fs.readFileSync(process.argv[2],'utf8'),src='https://store-079.wnam.tb-cdn.io/dld/movie?token=private&x=1';
 const player=load(html,src),v=player.get('video');assert.equal(v.src,src);// Native controls are declared in the page markup.
 assert(html.includes('controls playsinline'));assert.equal(v.poster,undefined,'untrusted poster blocked');assert.equal(player.history[0][2],'/player.html','private source removed from history fragment');
 assert.equal(player.get('browser').href,src);assert.equal(player.get('android').hidden,false);assert(player.get('android').href.startsWith('intent://'));assert.equal(player.get('vlc').hidden,true);
 v.error={code:4};v.handlers.error();assert(player.get('status').textContent.includes('MEDIA_4'));
 v.play=()=>{const e=Error('tap required');e.name='NotAllowedError';return Promise.reject(e);};await player.get('play').onclick();assert(player.get('status').textContent.includes('Tap the video'));
 let full=0;v.requestFullscreen=()=>{full++;return Promise.resolve();};player.get('fullscreen').onclick();assert.equal(full,1);delete v.requestFullscreen;v.webkitEnterFullscreen=()=>{full++;};player.get('fullscreen').onclick();assert.equal(full,2);
 player.events.pagehide();assert.equal(v.src,undefined);assert.equal(v.paused,true);assert.equal(v.loads,1);
 const invalid=load(html,'https://torbox.app.evil.example/media');assert.equal(invalid.get('video').src,undefined);assert.equal(invalid.get('play').disabled,true);
 const ios=load(html,src,'Mozilla iPad');assert.equal(ios.get('vlc').hidden,false);assert.equal(new URL(ios.get('vlc').href).searchParams.get('url'),src);
 const converted=vm.runInContext("subtitleVtt('1\\n00:00:01,000 --> 00:00:03,000\\nHello')",player.context);
 assert(converted.startsWith('WEBVTT'));assert(converted.includes('00:00:01.000 --> 00:00:03.000'));
 assert.throws(()=>vm.runInContext("subtitleVtt('no timings')",player.context));
 const controls=load(html,src);controls.get('subtitlePanel').hidden=true;controls.get('subtitles').onclick();assert.equal(controls.get('subtitlePanel').hidden,false);controls.get('subtitles').onclick();assert.equal(controls.get('subtitlePanel').hidden,true);
 let prompts=0;controls.get('video').remote={prompt:async()=>{prompts++;}};await controls.get('cast').onclick();assert.equal(prompts,1);
 let airplay=0;controls.get('video').webkitShowPlaybackTargetPicker=()=>{airplay++;};await controls.get('cast').onclick();assert.equal(airplay,1);
 vm.runInContext("installOnlineSubtitles([{lang:'es-419',label:'Latino',vtt:'WEBVTT\\n\\n00:00:01.000 --> 00:00:02.000\\nHello'}])",controls.context);assert.equal(vm.runInContext('onlineSubtitleResources.length',controls.context),1);assert(controls.get('subtitleStatus').textContent.includes('preloaded'));
 controls.events.pagehide();assert.equal(vm.runInContext('onlineSubtitleResources.length',controls.context),0);
 assert(html.includes('<svg width="22"'));assert(html.includes('id="castLabel"'));
 const direct=load(html,'https://channels.example.com/live.mp4','Samsung',{mode:'live'});assert.equal(direct.get('video').src,'https://channels.example.com/live.mp4');assert.equal(direct.get('play').disabled,undefined);
 const privateLive=load(html,'https://127.0.0.1/private.mp4','Samsung',{mode:'live'});assert.equal(privateLive.get('play').disabled,true);
 const engines=[];class FakeHls{static isSupported(){return true;}static Events={ERROR:'hls-error'};constructor(config){this.config=config;engines.push(this);}on(name,fn){this.error=fn;}loadSource(source){this.source=source;}attachMedia(video){this.video=video;}destroy(){this.destroyed=true;}}
 const hls=load(html,'https://channels.example.com/live.m3u8','Samsung',{mode:'live'},FakeHls);await Promise.resolve();await Promise.resolve();assert.equal(engines.length,1);assert.equal(engines[0].source,'https://channels.example.com/live.m3u8');assert.equal(engines[0].config.fragLoadingMaxRetry,1);
 let castRequest;hls.context.window.chrome={cast:{media:{MediaInfo:class{constructor(url,mime){this.contentId=url;this.contentType=mime;}},GenericMediaMetadata:class{},LoadRequest:class{constructor(info){this.media=info;}},StreamType:{LIVE:'LIVE'}}}};
 hls.context.window.cast={framework:{CastContext:{getInstance:()=>({requestSession:async()=>{},getCurrentSession:()=>({loadMedia:async request=>{castRequest=request;}})})}}};vm.runInContext('googleCastReady=true',hls.context);await hls.get('cast').onclick();assert.equal(castRequest.media.contentId,'https://channels.example.com/live.m3u8');assert.equal(castRequest.media.streamType,'LIVE');assert(hls.get('video').paused);
 hls.events.pagehide();assert(engines[0].destroyed);
 console.log('PASS: standalone player source validation, private fragment cleanup, native controls, Android/VLC fallbacks, codec/autoplay errors, subtitle conversion/controls, Cast/AirPlay dispatch/icon, automatic subtitle Blob installation, direct live HTTPS validation, HLS lifecycle and Google Cast LIVE dispatch');
})().catch(e=>{console.error(e);process.exitCode=1;});
