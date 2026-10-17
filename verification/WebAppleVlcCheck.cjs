'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('assert');
const code=fs.readFileSync(process.argv[2],'utf8');
class Node{constructor(tag,text){this.tag=tag;this.textContent=text;this.children=[];}append(...n){this.children.push(...n);}setAttribute(k,v){this[k]=v;}}
function setup(userAgent,platform='',maxTouchPoints=0){const calls=[];const c=vm.createContext({URL,URLSearchParams,Number,String,navigator:{userAgent,platform,maxTouchPoints},language:'en',safeImage:()=>false,el:(tag,text)=>new Node(tag,text),openBrowserPlayer:(...args)=>calls.push(args)});for(const name of ['isAppleMobile','isTorBoxMediaHost','vlcStreamLink','webPlayerAddress','appendBrowserPlayAction'])vm.runInContext(code.split('\n').find(l=>l.startsWith('function '+name+'(')),c);return {c,calls};}
const raw='https://store.tb-cdn.io/media/movie.mkv?token=private&name=A%20B';
for(const config of [['Mozilla iPhone'],['Mozilla iPad'],['Mozilla Macintosh','MacIntel',5]]){
 const {c,calls}=setup(...config),row=new Node('div');c.row=row;c.raw=raw;vm.runInContext("appendBrowserPlayAction(row,raw,'Movie','',null)",c);const primary=row.children[0].children[0];assert.equal(primary.tag,'a');assert.equal(primary.textContent,'▶ Open in VLC');assert.equal(primary.target,'_top');assert.equal(new URL(primary.href).searchParams.get('url'),raw);assert.equal(primary.onclick,undefined);assert.equal(calls.length,0);assert(row.children[0].children[1].href.startsWith('player.html?v=35#'));assert.throws(()=>vm.runInContext("vlcStreamLink('https://evil.example/file')",c));}
const android=setup('Android Chrome'),row=new Node('div');android.c.row=row;android.c.raw=raw;vm.runInContext("appendBrowserPlayAction(row,raw,'Movie','',null)",android.c);const play=row.children[0].children[0];assert.equal(play.tag,'button');play.onclick();assert.equal(android.calls.length,1);
assert(!code.includes('appleSourceRank'));assert(!code.includes('Safari candidate'));assert(code.includes('for(const source of result.sources)'));
console.log('PASS: primary VLC link on iPhone/iPad/desktop-mode iPad, exact signed URL encoding, trusted hosts, preserved Android player and original source order.');
