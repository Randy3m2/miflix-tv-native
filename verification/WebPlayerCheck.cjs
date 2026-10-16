'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('assert');
class Node {
 constructor(){this.children=[];this.handlers={};this.hidden=false;this.value='';this.paused=false;this.loads=0;}
 addEventListener(name,fn){this.handlers[name]=fn;} append(...nodes){this.children.push(...nodes);} setAttribute(){} removeAttribute(name){delete this[name];} querySelector(){return this.children[0]||null;} focus(){} select(){}
 play(){this.paused=false;return Promise.resolve();} pause(){this.paused=true;} load(){this.loads++;}
}
function load(html,src,agent='Mozilla Android Samsung'){
 const nodes=new Map(),get=id=>{if(!nodes.has(id)){const node=new Node();node.hidden=['vlc','android'].includes(id);nodes.set(id,node);}return nodes.get(id);},events={},history=[];
 const context=vm.createContext({URL,URLSearchParams,Error,Number,String,document:{getElementById:get,documentElement:{},createElement:()=>new Node()},location:{hash:'#'+new URLSearchParams({src,title:'My movie',lang:'en',autoplay:'1',poster:'https://evil.example/poster.jpg'}),pathname:'/player.html',search:''},history:{replaceState:(...args)=>history.push(args)},navigator:{userAgent:agent,platform:'Linux',maxTouchPoints:1},window:{addEventListener:(name,fn)=>events[name]=fn},setTimeout:()=>1,clearTimeout(){},fetch(){throw Error('No API/file proxy expected');}});
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
 console.log('PASS: standalone player source validation, private fragment cleanup, native controls, Android/VLC fallbacks, codec/autoplay errors and playback cleanup');
})().catch(e=>{console.error(e);process.exitCode=1;});
