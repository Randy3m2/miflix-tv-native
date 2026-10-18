'use strict';
const fs=require('fs'),assert=require('assert');
const root='app/src/main/java/com/miflix/native2/',read=p=>fs.readFileSync(root+p,'utf8');
const data=JSON.parse(fs.readFileSync('app/src/main/assets/awards_catalog.json','utf8'));
assert.equal(data.oscarCategory,'Best Picture');assert.deepEqual(Object.keys(data.oscars),['2020','2021','2022','2023','2024','2025','2026']);
for(const [year,rows] of Object.entries(data.oscars)){
 assert(rows.length>=8 && rows.length<=10);assert.equal(rows.filter(x=>x.winner).length,1);assert(rows.every(x=>x.year===Number(year)&&x.type==='movie'&&x.title));assert.equal(new Set(rows.map(x=>x.title)).size,rows.length);
}
assert.equal(data.oscars['2026'].find(x=>x.winner).title,'One Battle after Another');assert(data.oscars['2025'].some(x=>x.winner&&x.title==='Anora'));assert(data.emmys['2026'].some(x=>x.title==="Widow's Bay"));
const state=read('ui/AppState.kt'),screen=read('ui/Screens.kt'),repo=read('data/TmdbRepository.kt');
assert(!state.includes('catalog_badges:'));assert(state.includes('item.releaseDate'));assert(state.includes('private val cardDateSlots=Semaphore(3)'));assert(state.includes('key in cardDateInflight'));assert(state.includes('3*3600000L'));assert(!screen.includes('loadCardFreshness(badgeRows)'));
assert(read('ui/CinematicBrowsing.kt').includes('observeCardFreshness(item)'));assert(read('ui/Components.kt').includes('observeCardFreshness(item)'));assert(read('ui/CardFreshness.kt').includes('LaunchedEffect(item.cloudId,item.releaseDate)'));
assert(!read('ui/CinematicBrowsing.kt').includes('freshness!=null && awardLabel==null'));
assert(repo.includes('cardDatesCache'));assert(repo.includes('last_episode_to_air'));assert(repo.includes('releaseDate = date'));
assert(screen.indexOf('shelf(key="horror_movies")')<screen.indexOf('shelf(key="christmas_movies")'));assert(screen.includes('state.loadHolidayHome()'));assert(!screen.includes('!simple && state.familiarHomeMatches && state.christmasPicks'));
assert(repo.includes('listOf(771,10719,508965,5255,360920,1581,5825,508)'));assert(repo.includes('if(themed.isNotEmpty()) themed else'));
assert(read('ui/MiFlixNativeApp.kt').includes('Screen.OSCARS -> OscarsScreen(state)'));assert(read('ui/OscarsScreen.kt').includes('items(state.oscarYears'));assert(read('ui/OscarsScreen.kt').includes('awardYear=state.oscarYear'));
assert(repo.includes('awards:v2:$year:$locale:$kind'));assert(read('data/AwardSelections.kt').includes('fun latestYear'));
for(const rel of ['model/Models.kt','data/AwardSelections.kt','data/CardFreshnessPolicy.java','data/TmdbRepository.kt','ui/AppState.kt','ui/CardFreshness.kt','ui/CinematicBrowsing.kt','ui/Components.kt','ui/Screens.kt','ui/MiFlixNativeApp.kt','ui/OscarsScreen.kt','ui/UpdateNotes.kt']) assert.equal(read(rel),fs.readFileSync('main/java/com/miflix/native2/'+rel,'utf8'),rel);
assert(fs.readFileSync('app/build.gradle','utf8').includes("versionName '2.0.0-rc90'"));
console.log('PASS: verified award years/winners, global date metadata, visible-card dedup/cache limits, non-overlapping labels, Christmas fallback/retry/Simple Home, Oscar year navigation and mirrors.');
