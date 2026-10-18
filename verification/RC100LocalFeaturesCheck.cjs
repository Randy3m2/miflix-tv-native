const fs=require('fs'),assert=require('assert');
const root='app/src/main/java/com/miflix/native2/ui/';
const read=n=>fs.readFileSync(root+n,'utf8');
const state=read('AppState.kt'),screen=read('Screens.kt'),store=read('LocalExtrasRC100.kt'),ui=read('LocalExtrasUiRC100.kt');
// Check feature wiring across the state, screens and device-only data store.
for(const flag of ['sleep','still','lists','hide','filters','tracks','technical','backup']) {
 assert(store.includes('"'+flag+'"'));assert(ui.includes('"'+flag+'"'));
}
assert(screen.includes('!state.localExtras.sleepStopped && !state.localExtras.stillPrompt'));
assert(screen.includes('state.localExtras.episodeEnded(request.playbackId'));
assert(screen.includes('state.localExtras.touch()'));
assert(screen.includes('LocalPlayerToolsRC100(state,request,player'));
assert(store.includes('SystemClock.elapsedRealtime()'));
assert(store.includes('lastEpisode==id'));
assert(store.includes('"profile:$scope"'));
assert(store.includes('body.getString("scope")==scope'));
assert(ui.includes('state.validateLocalBackupRC100(body);pendingImport=body'));
assert(ui.includes('ActivityResultContracts.CreateDocument'));
assert(ui.includes('output.size()+n<=2_000_000'));
assert(screen.includes('gridItems(visibleCatalog'));
assert(state.includes('filterOwner!=ratingsKey() || filterLanguage!=interfaceLanguage'));
assert(state.includes('"interface:${ratingsKey()}"'));
assert(state.includes('putString(interfaceLanguageKey(),code)'));
assert(!state.includes('putString("interface",code)'));
assert(state.includes('activeProfile = p\n        applyProfileInterfaceLanguage()'));
assert(state.includes('if(current()) trending.replaceWith(it)'));
assert(screen.includes('savedSeriesTracks'));
assert(screen.includes('state.localExtras.rememberTracks'));
// Feature source contains no cloud/presence/add-on or HTTP access.
for(const code of [store,ui]) for(const forbidden of ['supabase','pushCloud(','Http.','OkHttp','hostPartyUpdate(','streamRepo.']) assert(!code.includes(forbidden),forbidden);
const backup=state.slice(state.indexOf('internal fun exportLocalBackupRC100'),state.indexOf('internal fun validateLocalBackupRC100'));
for(const forbidden of ['accessToken','refreshToken','password','stream.url','session']) assert(!backup.includes(forbidden),forbidden);
for(const name of ['AppState.kt','Screens.kt','UpdateNotes.kt','HomeRowVisibilityRC99.kt','LocalExtrasRC100.kt','LocalExtrasUiRC100.kt']) assert(fs.readFileSync(root+name).equals(fs.readFileSync('main/java/com/miflix/native2/ui/'+name)));
assert(state.includes('val automaticFallback=autoplayNextLink && watchParty==null'));
assert(screen.includes('homeSections.filter {it.key !in state.hiddenHomeRows'));
console.log('PASS: feature wiring, automatic playback guards, profile language scope, bounded backup validation, local-only access and RC99 preservation. Static checks only; no APK compilation.');
