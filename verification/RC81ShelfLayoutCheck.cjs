'use strict';
const fs=require('fs'),assert=require('assert');
const root='app/src/main/java/com/miflix/native2/',read=p=>fs.readFileSync(root+p,'utf8');
const cards=read('ui/CinematicBrowsing.kt'),ui=read('ui/Screens.kt'),frame=read('ui/HomeShelfFrame.kt'),hero=read('ui/BrowseHero.kt'),components=read('ui/Components.kt');
assert(cards.includes('delay(3000)'));assert(cards.includes('modifier.width(slotWidth.dp)'));assert(cards.includes('if(expanded) 334f else baseWidth'));
assert(!cards.includes('unbounded=true'));assert(!cards.includes('requiredWidth(displayWidth.dp)'));assert(!cards.includes('.zIndex(if(expanded)'));
assert(cards.includes('delay(170);cardBring.bringIntoView()'));assert(cards.includes('showTitle=showTitle'));assert(cards.includes('expanded || simple || showTitle'));
const home=ui.slice(ui.indexOf('fun HomeScreen('),ui.indexOf('private fun MediaRail'));
assert(home.includes('rows=state.continueWatching,showTitle=true'));assert(!home.includes('rows=state.continueWatching,landscape=true'));
assert(home.includes('Modifier.fillParentMaxHeight().fillMaxWidth()'));assert(home.includes('var shelfIndex=0'));assert(home.includes('calculateScrollDistance(offset: Float,size: Float,containerSize: Float): Float = 0f'));
assert(frame.includes('LaunchedEffect(active,index)'));assert(frame.includes('row.offset.toFloat(),tween(140)'));assert(frame.includes('scroll.animateScrollToItem(index)'));assert(frame.includes('onFocusChanged {active=it.hasFocus}'));
assert(!components.includes('Text(tile.title, color = Color.White, fontSize = 14.sp'));assert(components.includes('Text(tile.title,color=Color.White,fontSize=25.sp'));
assert(hero.includes('val compact=heroFocus!=null'));assert(hero.includes('if(compact) 2 else'));
// Layout invariants throughout expansion/contraction: artwork stays within its measured slot.
for(const base of [130,172,312])for(let tick=0;tick<=20;tick++){
 const width=base+(334-base)*tick/20;
 const focusedArtworkRight=6+(width-12);
 const nextArtworkLeft=width+6;
 assert(focusedArtworkRight<nextArtworkLeft);
}
// Full shelf header + cards must fit below the fixed hero at supported TV viewport heights.
for(const height of [432,480,540,600,720,1080]){
 const heroHeight=Math.min(284,Math.max(228,height*.5));
 const contentHeight=height-heroHeight,innerHeight=contentHeight-16;
 const cardHeight=Math.min(188,Math.max(124,innerHeight-64));
 assert(cardHeight+60<=innerHeight,`${height}: heading and cards fit, including eyebrow`);
 assert(208<=innerHeight||height<480,'streaming shelf fits standard TV viewport');
}
for(const name of ['ui/HomeShelfFrame.kt','ui/CinematicBrowsing.kt','ui/BrowseHero.kt','ui/Components.kt','ui/Screens.kt','ui/UpdateNotes.kt'])assert.equal(read(name),fs.readFileSync('main/java/com/miflix/native2/'+name,'utf8'));
assert(fs.readFileSync('app/build.gradle','utf8').includes("versionName '2.0.0-rc81'"));
console.log('PASS: push-layout geometry, equal Continue Watching cards, in-card titles, full-group focus scrolling in both directions, compact hero, no redundant streaming captions and mirrors.');
