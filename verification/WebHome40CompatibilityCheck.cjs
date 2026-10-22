'use strict';
const vm=require('node:vm'),fs=require('node:fs'),assert=require('node:assert/strict');
const source=fs.readFileSync(process.argv[2]||'web/home39.js','utf8');
const begin=source.indexOf('const homeLoad39=load;'),end=source.indexOf('const homeRender39=render;');
async function check(message){
 let calls=0,legacy=0,status='',disabled=false;
 const ctx={generation:0,loading:false,items:[],hasMore:false,activeProvider:'',filter:'all',home39:{key:'test',shelves:[],more:true,next:0,busy:false,unsupported:false},
 isHome39:()=>!ctx.home39.unsupported,homeKey39:()=> 'test',homeSeed39:()=>'',cinemaWords:(a,b)=>b,t:x=>x,
 $:id=>id==='more'?{set textContent(x){},set disabled(x){disabled=x}}:id==='region'?{value:'US'}:{set textContent(x){status=x}},
 render:()=>{},load:async()=>{legacy++},catalog:async()=>{calls++;throw Error(message)},Set,Map,Error};
 vm.createContext(ctx);vm.runInContext(source.slice(begin,end),ctx);
 await ctx.load(true);
 if(message==='Invalid action'){assert.equal(legacy,1);assert.equal(ctx.home39.unsupported,true);assert.equal(disabled,false);await ctx.load(true);assert.equal(calls,1);assert.equal(legacy,2);}
 else {assert.equal(legacy,0);assert.equal(ctx.home39.unsupported,false);assert.equal(status,message);}
}
(async()=>{await check('Invalid action');await check('Session expired');await check('Network error');console.log('PASS: legacy Explore fallback once, no repeated unsupported calls, auth/network errors preserved.');})();
