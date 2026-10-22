const fs=require('fs'),vm=require('vm'),assert=require('assert');
const source=fs.readFileSync(process.argv[2],'utf8');const start=source.indexOf('const setView39=setView;'),end=source.indexOf('// Profile disclosure',start);assert(start>=0&&end>start);
let destination,seen=[];const ctx={continueShelf44:{hidden:false},home39:{key:'home',shelves:[{}]},$:()=>({}),t:x=>x,setView:next=>{destination=next;seen.push(ctx.continueShelf44.hidden);}};vm.createContext(ctx);vm.runInContext(source.slice(start,end),ctx);
for(const next of ['friends','calendar','live','mylist','recents','upcoming','explore']){ctx.continueShelf44.hidden=false;ctx.setView(next);assert.equal(destination,next);assert.equal(ctx.continueShelf44.hidden,true);assert.equal(seen.at(-1),true,'must hide before starting destination loading');}
assert.equal(ctx.home39.shelves.length,0);console.log('PASS: Continue Watching hidden immediately on all tab transitions, before async destination loading');
