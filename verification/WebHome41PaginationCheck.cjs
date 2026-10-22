'use strict';
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const root=process.argv[2]||'web41-patch/web';
let homeCalls=0,legacyCalls=0,active=0,peak=0,legacyFallbacks=0;
const nodes={more:{disabled:false,textContent:''},status:{textContent:''},region:{value:'US'}};
const ctx={home39:{key:'',shelves:[],next:0,more:true,busy:false,seen:new Map(),unsupported:false},language:'en',generation:0,loading:false,items:[],hasMore:false,activeProvider:'netflix',filter:'all',Map,Set,Date,Promise,Error,
 $:id=>nodes[id],isHome39:()=>true,homeKey39:()=> 'owner|profile|en|netflix',homeSeed39:()=>'',homeFresh39:()=>{},cinemaWords:(a,b)=>b,t:x=>x,render:()=>{},load:async()=>{legacyFallbacks++},
 catalog:async body=>{
  if(body.action==='home'){homeCalls++;throw Error('Invalid action');}
  legacyCalls++;active++;peak=Math.max(peak,active);await new Promise(r=>setTimeout(r,1));active--;
  assert.equal(body.provider,'netflix');assert.equal(body.region,'US');
  return {items:Array.from({length:20},(_,i)=>({cloudId:`tmdb:${body.type}:${(Number(body.genre)||100)+i}`,id:i,type:body.type,title:`${body.query||body.genre||body.sort} ${i}`,date:i===0?'2999-01-01':'2024-01-01'}))};
 }};
vm.createContext(ctx);vm.runInContext(fs.readFileSync(root+'/home-compat41.js','utf8'),ctx);
const source=fs.readFileSync(root+'/home39.js','utf8');vm.runInContext(source.slice(source.indexOf('const homeLoad39=load;'),source.indexOf('const homeRender39=render;')),ctx);
(async()=>{
 await ctx.load(true);assert.equal(ctx.home39.shelves.length,15);const original=ctx.home39.shelves.map(s=>JSON.stringify(s));
 await ctx.load(false);assert.equal(ctx.home39.shelves.length,21);
 await ctx.load(false);assert.equal(ctx.home39.shelves.length,27);
 assert.equal(ctx.home39.shelves.slice(0,15).map(s=>JSON.stringify(s)).join('|'),original.join('|'),'previous rows unchanged');
 assert.equal(new Set(ctx.home39.shelves.map(s=>s.id)).size,27,'distinct rows');assert.equal(homeCalls,1);assert.equal(legacyFallbacks,0);assert(peak<=3);assert(ctx.home39.shelves.every(s=>s.items.every(x=>x.date<='2026-12-31')));
 assert.equal(ctx.home39.shelves[0].items.length,10);assert.equal(ctx.home39.shelves[1].items.length,10);
 while(ctx.home39.more)await ctx.load(false);
 assert(ctx.home39.shelves.length>27);assert.equal(ctx.home39.more,false);assert.equal(nodes.more.disabled,false);
 console.log('PASS: 15 → 21 → 27 shelves, previous rows preserved, unique IDs, subsequent pages to exhaustion, one unsupported probe, provider/region maintained, concurrency ≤3 and future releases excluded.');
 const previous=ctx.catalog;ctx.home39.unsupported=false;ctx.catalog=async()=>{throw Error('Session expired')};await assert.rejects(()=>ctx.requestHomeShelves41({action:'home'}),/Session expired/);ctx.catalog=previous;
 console.log('PASS: authentication errors do not fall back.');
})().catch(e=>{console.error(e);process.exitCode=1;});
