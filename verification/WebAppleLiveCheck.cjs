'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('assert');
const source=fs.readFileSync(process.argv[2],'utf8');
class N {constructor(text=''){this.textContent=text;this.children=[];this.open=true;}append(...x){this.children.push(...x);}replaceChildren(...x){this.children=x;}close(){this.open=false;}}
async function run(airplay,url){
 const dialog=new N(),opened=[],state={prepares:0,casts:0};
 const c=vm.createContext({URL,Error,String,Object,Promise,isAppleWebDevice:()=>true,document:{createElement:()=>airplay?{webkitShowPlaybackTargetPicker(){}}:{}},words:en=>en,t:x=>x,experienceDialog:()=>dialog,liveManifest:()=> 'https://provider.example/manifest.json',prepareLiveCast:async()=>{state.prepares++;return true;},sendLiveCast:async()=>{state.casts++;},el:(_tag,text)=>new N(text),button:(text,fn)=>{const n=new N(text);n.onclick=fn;return n;},addLiveCastIcon(){},addLiveAirplayIcon(){},openBrowserPlayer:(...args)=>opened.push(args),liveMessage:e=>e.message});
 vm.runInContext('let liveSourceRevision=0;'+['publicLiveURL','liveCastSource','livePrefersAirPlay'].map(name=>source.split('\n').find(l=>l.startsWith('function '+name+'('))).join('\n')+'\n'+source.slice(source.indexOf('async function openLiveSources('),source.indexOf("$('live').onclick")),c);
 await vm.runInContext('openLiveSources('+JSON.stringify({name:'Channel',streams:[{name:'Source',url}]})+')',c);
 const row=dialog.children.find(n=>n.children.some(x=>typeof x.onclick==='function')),button=row.children.find(n=>typeof n.onclick==='function');
 await button.onclick();return {dialog,opened,state,row,button};
}
(async()=>{
 const apple=await run(true,'https://channel.example/live.m3u8');assert.equal(apple.button.textContent,'AirPlay');assert.equal(apple.state.prepares,0);assert.equal(apple.state.casts,0);assert.equal(apple.opened[0][0],'https://channel.example/live.m3u8');assert(apple.opened[0][3].live);assert(!apple.dialog.open);
 const http=await run(true,'http://channel.example/live.m3u8');assert.equal(http.opened.length,0);assert(http.row.children.some(x=>String(x.textContent).includes('AirPlay needs an HTTPS')));
 const chromeMac=await run(false,'https://channel.example/live.m3u8');assert.equal(chromeMac.opened.length,0);assert.equal(chromeMac.state.casts,1,'Mac Chrome retains Google Cast without WebKit AirPlay API');
 console.log('PASS: Live AirPlay controls, no Google SDK on AirPlay, HTTPS enforcement and Chrome Mac Cast fallback.');
})().catch(e=>{console.error(e);process.exitCode=1});
