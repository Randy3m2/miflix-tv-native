'use strict';
const fs=require('fs'),assert=require('assert'),vm=require('vm');
const base='app/src/main/java/com/miflix/native2/',read=f=>fs.readFileSync(base+f,'utf8');
const seed=JSON.parse(fs.readFileSync('app/src/main/assets/lidom_teams.json','utf8'));
assert.equal(seed.teams.length,6);assert.equal(new Set(seed.teams.map(x=>x.id)).size,6);
assert.deepEqual(seed.teams.map(x=>x.mlb).sort(),[667,668,669,670,671,672]);
for(const t of seed.teams){assert(t.sources.length>=2);assert.equal(t.sources[0].kind,'YOUTUBE');assert(t.sources.every(s=>s.team===t.id));assert.equal(new Set(t.sources.map(s=>s.id)).size,t.sources.length);assert(fs.statSync('app/src/main/res/drawable-nodpi/'+t.logo+'.png').size>500);for(const s of t.sources)assert(!s.url||/^https:\/\/www.youtube.com\//.test(s.url),'no guessed stream URLs');}
const fixture=JSON.parse(fs.readFileSync('verification/lidom_schedule_fixture.json','utf8'));
assert.equal(fixture.dates[0].games.length,3);
for(const game of fixture.dates[0].games){for(const side of ['home','away'])assert(seed.teams.some(t=>t.mlb===game.teams[side].team.id));assert.equal(new Intl.DateTimeFormat('en-US',{timeZone:'America/Santo_Domingo',day:'2-digit'}).format(new Date(game.gameDate)),'16');}
const repo=read('data/LidomRepository.kt'),ui=read('ui/LidomScreen.kt'),yt=read('ui/LidomYoutubePlayer.kt'),screen=read('ui/Screens.kt'),state=read('ui/AppState.kt');
assert(repo.includes('statsapi.mlb.com/api/v1/schedule?sportId=17&leagueId=131'));
assert(!repo.includes('SUPABASE'));assert(!repo.includes('/rest/v1'));assert(repo.includes('SHA-256'));assert(repo.includes('prefix+"favorites"'));assert(repo.includes('prefix+"last:"'));
assert(ui.includes('LaunchedEffect(date,refresh)'));assert(!ui.includes('while(true)'));assert(ui.includes('team=game.home'));assert(ui.includes('sources by team')||ui.includes('Sources by team'));
assert(ui.includes('key(owner)'));assert(yt.includes('value=="playing" && !recorded'));
assert(screen.includes('if(!firstFrame) state.markLidomSourceWorking(request)'));
assert(state.includes('req.liveProvider!="lidom" && liveProviderTab'));
assert(state.includes('if(req.liveProvider=="lidom") {showLidomSources(req);return}'));
assert(screen.includes('if(request.liveMime.isNotBlank()) builder.setMimeType(request.liveMime)'));
assert(screen.includes('if(request.liveProvider=="lidom") return@LaunchedEffect'));
assert(yt.includes('loadDataWithBaseURL(origin+"/"'));assert(yt.includes('settings.allowFileAccess=false'));assert(yt.includes('MIXED_CONTENT_NEVER_ALLOW'));assert(yt.includes('it.destroy()'));assert(yt.includes('Event.ON_STOP'));
const html=fs.readFileSync('app/src/main/assets/lidom_youtube.html','utf8'),script=html.match(/<script>([\s\S]*?)<\/script>/)[1];
const messages=[],actions=[];let config;
const sandbox={window:{BruniO:true},BruniO:{status:m=>messages.push(m)},YT:{Player:function(id,options){config=options;return {getPlayerState:()=>1,pauseVideo:()=>actions.push('pause'),playVideo:()=>actions.push('play'),getCurrentTime:()=>5,seekTo:n=>actions.push(n)};}}};
vm.createContext(sandbox);vm.runInContext(script.replace('__VIDEO_ID__','abcdefghI_1').replace('__ORIGIN__','https://com.miflix.native2'),sandbox);sandbox.onYouTubeIframeAPIReady();
assert.equal(config.playerVars.controls,1);assert.equal(config.playerVars.loop,0);config.events.onReady();assert.deepEqual(messages,['ready']);config.events.onStateChange({data:1});assert.equal(messages.at(-1),'playing');config.events.onError({data:153});assert.equal(messages.at(-1),'error:153');config.events.onAutoplayBlocked();assert.equal(messages.at(-1),'blocked');sandbox.brunioPause();sandbox.brunioSeek(-10);assert.deepEqual(actions,['pause',0]);
for(const f of ['data/LidomRepository.kt','data/LidomUrlPolicy.java','ui/LidomScreen.kt','ui/LidomYoutubePlayer.kt','ui/AppState.kt','ui/Screens.kt','ui/LiveScreen.kt','ui/PlayerControls.kt','ui/MiFlixNativeApp.kt','ui/UpdateNotes.kt'])assert.equal(read(f),fs.readFileSync('main/java/com/miflix/native2/'+f,'utf8'),f);
console.log('PASS: six team/source/logo mappings; no invented stream URL; direct schedule; owner-scoped favorites; no added polling/Supabase; game home-team routing; success-only source memory; unchanged TV tab; HLS/DASH and retry routing; official YouTube API events, blocked/embed errors, controls, lifecycle and mirrors.');
