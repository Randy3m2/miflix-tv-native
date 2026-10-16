'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('assert');
class Element {
 constructor(tag='div'){this.tagName=tag;this.children=[];this.value='';this.dataset={};this.style={};this.classList={toggle(){}};this.open=false;this.hidden=false;this.selectedOptions=[{textContent:'USA'}];}
 append(...nodes){this.children.push(...nodes);} replaceChildren(...nodes){this.children=nodes;}
 setAttribute(){} showModal(){this.open=true;} close(){this.open=false;} focus(){} select(){} reportValidity(){return true;}
}
(async()=>{
 const nodes=new Map(),get=id=>{if(!nodes.has(id))nodes.set(id,new Element());return nodes.get(id);};
 const requests=[],listeners={},intervals=[];
 const context=vm.createContext({console,URL,URLSearchParams,AbortController,Date,Intl,Math,Map,Set,Promise,
  location:{hash:'#m'+(550).toString(36),pathname:'/',search:''},history:{replaceState(){}},navigator:{},
  localStorage:{getItem(){return null},setItem(){}},sessionStorage:{getItem(){return null},setItem(){},removeItem(){}},
  document:{hidden:false,documentElement:{dataset:{}},getElementById:get,createElement:tag=>new Element(tag),querySelector:()=>new Element(),querySelectorAll:()=>[],addEventListener:(key,fn)=>listeners[key]=fn},
  window:{parent:null,addEventListener(){}},setTimeout:()=>1,clearTimeout(){},setInterval:fn=>intervals.push(fn),
  fetch:async(url,options)=>{requests.push({url,options});const input=options.body?JSON.parse(options.body):{};return {ok:true,status:200,json:async()=>({items:(input.ids||[]).map(cloudId=>({cloudId,id:Number(cloudId.split(':')[2]),type:cloudId.split(':')[1],title:cloudId}))})};}
 });context.window.parent=context.window;
 vm.runInContext(fs.readFileSync(process.argv[2],'utf8'),context);
 const run=code=>vm.runInContext(code,context);
 assert.equal(run('pendingShared'),'tmdb:movie:550');
 assert.equal(run("shortTitleLink({type:'movie',id:550})"),'https://miflix-tv-native-web.vercel.app/#mfa');
 assert(!run("shortTitleLink({type:'series',id:1399})").includes('tv='));
 assert.equal(requests.length,0,'no logged-out API requests');
 run("session={user:{id:'owner'},expires_at:Date.now()+3600000,access_token:'test'}; $('profile').value='p1';");
 await run('openSharedTitle()');assert.equal(get('detail').open,true);assert.equal(run('currentDetail.cloudId'),'tmdb:movie:550');
 requests.length=0;await run("titleBatch(Array.from({length:25},(_,i)=>'tmdb:movie:'+(i+1)))");
 assert.deepEqual(requests.map(r=>JSON.parse(r.options.body).ids.length),[20,5]);
 requests.length=0;
 run("recentProgress=Object.fromEntries(Array.from({length:51},(_,i)=>['tmdb:movie:'+(i+1),{position:100,updatedAt:i+1}]));view='recents';page=1;");
 await run('loadRecents(false)');assert.equal(run('items.length'),25);assert.equal(run('items[0].cloudId'),'tmdb:movie:51');assert.equal(run('hasMore'),true);
 run('page=2');await run('loadRecents(false)');assert.equal(run('items.length'),50);run('page=3');await run('loadRecents(false)');assert.equal(run('items.length'),51);assert.equal(run('hasMore'),false);
 requests.length=0;await run("recommendTitle({cloudId:'tmdb:movie:550',title:'Fight Club',poster:null})");
 assert.equal(requests[0].options.headers.Prefer,'resolution=merge-duplicates,return=minimal');
 // Inject the local bridge response: Now Playing must never use the cloud API.
 run("castToken='local-parent-bridge';castPost=async(path,payload)=>{if(path!=='/status'||payload.action!=='status')throw Error('wrong local endpoint');return {status:{active:true,cloud:'tmdb:movie:550',title:'Fight Club',position:65000,duration:120000,playing:true}};};");
 requests.length=0;await run('pollNowPlaying()');assert.equal(requests.length,0);assert.equal(get('nowPlaying').hidden,false);
 run('document.hidden=true');get('nowPlaying').hidden=true;await run('pollNowPlaying()');assert.equal(get('nowPlaying').hidden,true,'hidden tab does not poll');
 run("document.hidden=false;castToken='';");await run('pollNowPlaying()');assert.equal(get('nowPlaying').hidden,true,'disconnect hides stale activity');
 assert.equal(intervals.length,1);console.log('PASS: short links/deep-link login continuation, 25-title batches, recents sorting/pagination, recommendation upsert, local-only Now Playing and hidden/disconnected polling');
})().catch(e=>{console.error(e);process.exitCode=1;});
