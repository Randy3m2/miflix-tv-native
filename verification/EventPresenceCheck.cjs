'use strict';
const fs=require('fs'),assert=require('assert');
const root='app/src/main/java/com/miflix/native2/';
const read=p=>fs.readFileSync(root+p,'utf8');
const ui=read('ui/MiFlixNativeApp.kt'),state=read('ui/AppState.kt');
const eventEffect=ui.slice(ui.indexOf('// Event presence'),ui.indexOf('PresenceNetworkRecovery(state)'));
assert(eventEffect.includes('socialReadyPlaybackId'));
assert(!/while\s*\(|delay\s*\(/.test(eventEffect));
const legacy=state.slice(state.indexOf('suspend fun refreshSocialActivity()'),state.indexOf('suspend fun decideAccess'));
assert(!legacy.includes('social.presence'));
assert(!read('data/SocialRepository.kt').includes('presenceGate'));
assert(read('data/EventPresencePublisher.kt').includes('prefs.getString(key,null)==raw'));
assert(read('data/EventPresencePublisher.kt').includes('.commit()'));
assert(!/delay\s*\(/.test(read('data/EventPresencePublisher.kt')));
assert(ui.includes('state.appForeground=false;state.publishSocialPresenceEvent()'));
assert(state.includes('session?.let { presenceEvents.queue(it,"","",0,0,null'));
assert(read('ui/Screens.kt').includes('state.socialPlaybackEnded(request)'));
for(const rel of ['data/EventPresencePublisher.kt','data/SocialRepository.kt','ui/AppState.kt',
 'ui/MiFlixNativeApp.kt','ui/PresenceNetworkRecovery.kt','ui/Screens.kt','ui/UpdateNotes.kt']) {
 assert.equal(read(rel),fs.readFileSync('main/java/com/miflix/native2/'+rel,'utf8'),rel+' mirror');
}
console.log('PASS: event-only presence wiring, durable latest-intent acknowledgement, lifecycle/sign-out/end cleanup, no timer retry, matching source mirrors.');
