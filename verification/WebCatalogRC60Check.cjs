'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('assert');
class Element {
 constructor(tag='div'){this.tagName=tag;this.children=[];this.value='';this.dataset={};this.style={};this.classList={toggle(){}};this.open=false;this.hidden=false;this.selectedOptions=[{textContent:'USA'}];}
 append(...nodes){this.children.push(...nodes);} replaceChildren(...nodes){this.children=nodes;}
 remove(){} scrollIntoView(){} replaceWith(node){this.replacement=node;}
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
 run("api=async(path,method,body)=>body.action==='sources'?{sources:[{name:'Comet 1080p',filename:'Movie.mkv',provider:'Comet',size:1000,ticket:'opaque-ticket'}]}:{url:'https://cdn.torbox.app/download/Movie.mkv',filename:'Movie.mkv'};");
 await run('openDownloads(currentDetail)');const panel=get('detailBody').children.at(-1),row=panel.children.at(-1),generate=row.children[1];await generate.onclick();assert.equal(generate.replacement.tagName,'a');assert.equal(generate.replacement.href,'https://cdn.torbox.app/download/Movie.mkv');assert.equal(generate.replacement.download,'Movie.mkv');assert.equal(row.children[1].className,'source-primary-actions');assert(row.children[1].children.at(-1).className.includes('source-other-players'));assert.equal(row.children[2].className,'source-status-ready');
 run("navigator.platform='MacIntel';navigator.maxTouchPoints=5;navigator.userAgent='Mozilla Macintosh';");assert.equal(run('isAppleMobile()'),true);
 const raw='https://store-079.wnam.tb-cdn.io/dld/Movie?token=temporary&x=1';const encoded=run('vlcStreamLink('+JSON.stringify(raw)+')');assert.equal(new URL(encoded).searchParams.get('url'),raw);assert.throws(()=>run("vlcStreamLink('javascript:alert(1)')"));
 run("const testRow=el('div');appendExternalPlayerActions(testRow,'https://cdn.torbox.app/file');globalThis.playerTestRow=testRow;");assert.equal(context.playerTestRow.children[0].children[0].href.startsWith('vlc-x-callback:'),true);
 assert.equal(run("isTorBoxMediaHost('store.tb-cdn.earth')"),true);assert.equal(run("isTorBoxMediaHost('store.tb-cdn.io.evil.example')"),false);
 assert.equal(run('downloadMaxGb'),5);
 run('showDetail(currentDetail);castRender();');const info=get('detailBody').children[1],actions=info.children.at(-1);assert.equal(actions.children.filter(x=>x.textContent==='Enviar a TV').length,0);assert.equal(actions.children.filter(x=>x.textContent==='Reproducir en TV').length,1);assert.equal(actions.children.length,4);assert(actions.children[0].textContent.includes('Play or Download'));assert.equal(actions.children[3].tagName,'details');assert.equal(actions.children[3].children[1].children.length,3);assert.equal(get('castTools').textContent,'Conectar TV');
 run("const filePanel=el('section');appendLocalFileActions(filePanel);globalThis.localFileTestPanel=filePanel;");const localBox=context.localFileTestPanel.children[0],fileInput=localBox.children[1],openWith=localBox.children[2];assert.equal(openWith.hidden,true);assert.equal(openWith.textContent,'Open in external player');
 const chosen={name:'Downloaded.mkv',size:1000};fileInput.files=[chosen];fileInput.onchange();assert.equal(openWith.hidden,false);
 let shared=null;context.navigator.canShare=data=>data.files[0]===chosen;context.navigator.share=async data=>{shared=data.files[0];};const beforeShare=requests.length;await openWith.onclick();assert.equal(shared,chosen);assert.equal(requests.length,beforeShare,'no cloud request/file upload');
 context.navigator.canShare=()=>false;await openWith.onclick();assert(localBox.children[3].textContent.includes('Archivos'),'unsupported file sharing explains local Files fallback');
 assert(localBox.children[3].textContent.includes('FILE_SHARE_REJECTED'));
 context.navigator.canShare=()=>{throw Error('blocked')};await openWith.onclick();assert(localBox.children[3].textContent.includes('FILE_SHARE_FAILED'));
 context.window.isSecureContext=false;await openWith.onclick();assert(localBox.children[3].textContent.includes('SECURE_CONTEXT_REQUIRED'));context.window.isSecureContext=true;
 context.navigator.canShare=()=>true;context.navigator.share=async()=>{const e=Error('closed');e.name='AbortError';throw e;};await openWith.onclick();assert(localBox.children[3].textContent.includes('SHARE_CANCELLED'));assert.equal(openWith.disabled,false);
 run("navigator.userAgent='Mozilla Android Samsung';navigator.platform='Linux arm';navigator.maxTouchPoints=5;const androidRow=el('div');appendExternalPlayerActions(androidRow,'https://store.tb-cdn.io/dld/movie?token=private');globalThis.androidPlayerTest=androidRow;");const androidLink=context.androidPlayerTest.children[0].children[0];assert(androidLink.href.startsWith('intent://store.tb-cdn.io/'));assert(androidLink.href.includes('action=android.intent.action.VIEW'));assert(androidLink.href.includes('type=video/*'));assert(!androidLink.href.includes('package='));
 assert.throws(()=>run("androidMediaIntent('https://example.com/file')"));
 const media='https://store.tb-cdn.io/dld/movie?token=private&x=1';const playerPage=run('webPlayerAddress('+JSON.stringify(media)+',"Movie",null)');assert(playerPage.startsWith('player.html?v=28#'));assert.equal(new URLSearchParams(playerPage.split('#')[1]).get('src'),media);assert.equal(playerPage.split('#')[0],'player.html?v=28');
 run('openBrowserPlayer('+JSON.stringify(media)+',"Movie",null)');const playerFrame=get('webPlayerDialog').children[1];assert.equal(get('webPlayerDialog').open,true);assert(playerFrame.allow.includes('fullscreen'));run('closeBrowserPlayer()');assert.equal(playerFrame.src,'about:blank');assert.equal(get('webPlayerDialog').open,false);
 assert.equal(intervals.length,1);console.log('PASS: short links/deep-link login continuation, 25-title batches, recents sorting/pagination, recommendation upsert, local-only Now Playing and hidden/disconnected polling');
})().catch(e=>{console.error(e);process.exitCode=1;});
