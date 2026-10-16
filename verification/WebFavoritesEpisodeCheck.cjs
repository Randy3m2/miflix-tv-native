'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('assert');
(async()=>{
 const app=fs.readFileSync(process.argv[2],'utf8'),experience=fs.readFileSync(process.argv[3],'utf8');
 const controls=[{dataset:{favoriteId:'tmdb:series:1'},setAttribute(){},disabled:false}],state={favorites:new Set(),session:{user:{id:'owner'}},selected:()=> 'profile',language:'en',document:{querySelectorAll:()=>controls},t:x=>x,render(){},updateCount(){},toast(){},showDetail(){throw Error('Must not rebuild the open title');}};
 let writes=0;state.api=async(_path,_method,body)=>{writes++;assert.equal(body.p_profile,'profile');return {favorites:body.p_adds.length?body.p_adds:[]};};
 const context=vm.createContext(state);vm.runInContext(app.slice(app.indexOf('const favoriteUpdates='),app.indexOf('function detailImage(')),context);
 await vm.runInContext("toggle({cloudId:'tmdb:series:1'},document.querySelectorAll()[0])",context);
 assert.equal(writes,1);assert(state.favorites.has('tmdb:series:1')||vm.runInContext("favorites.has('tmdb:series:1')",context));assert(controls[0].textContent.startsWith('✓'));assert(!controls[0].disabled);
 await vm.runInContext("toggle({cloudId:'tmdb:series:1'},document.querySelectorAll()[0])",context);assert(!vm.runInContext("favorites.has('tmdb:series:1')",context));assert.equal(writes,2);
 const episodes=experience.slice(experience.indexOf('async function showEpisodes('),experience.indexOf('async function randomWebEpisode('));
 assert(episodes.includes('save.dataset.favoriteId=item.cloudId'));assert(!episodes.includes('if(validDetail(item,key,lang))showEpisodes(item,season,parent)'));
 assert(episodes.includes("control.textContent=nowSeen?"));
 console.log('PASS: favorite RPC add/remove updates controls without rebuilding details; episode actions update in place');
})().catch(e=>{console.error(e);process.exitCode=1});
