"""Static integration guards, not an Android compile or UI test. Run from patch/repository root."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
u=root/'app/src/main/java/com/miflix/native2/ui'
s=(u/'AppState.kt').read_text();app=(u/'MiFlixNativeApp.kt').read_text()
controls=(u/'PlayerControls.kt').read_text();screens=(u/'Screens.kt').read_text();ui=(u/'EasyModeRP79.kt').read_text()
def require(value,message):
    if not value: raise AssertionError(message)
require('if(kidsMode || easyModeRP79) null' in s,'social identity must be disabled')
require('if(easyModeRP79 || checkingNotices' in s,'notifications must be disabled')
require('session?.takeUnless {easyModeRP79}?.let { account ->' in s,'bootstrap Trakt/party restore must be disabled')
require('if(easyModeRP79) return\n' in s.split('suspend fun traktMarkWatched',1)[1][:200],'Trakt watched must be disabled')
require('if(!state.easyModeRP79) PresenceNetworkRecovery(state)' in app,'outbox retry callback must not register')
require('if(!state.easyModeRP79) LocalCastHost(state)' in app,'companion host must not open')
require('if(bootstrapped && !state.easyModeRP79 && state.session!=null && state.appForeground) while(true)' in app,'social loop must not start')
require('if (!state.easyModeRP79 && state.watchParty != null && state.appForeground)' in app,'party polling must not start')
require('state.pendingPartyCode,state.easyModeRP79)' in app,'mode change must cancel social loop')
require('state.watchParty?.roomCode,state.appForeground,state.easyModeRP79)' in app,'mode change must cancel party loop')
require('if(state.easyModeRP79 && destination!=Screen.PLAYER) EasyScreenRP79' in app,'simple route switch missing')
require('allowsScreen(value.name)' in s,'state-level route protection missing')
require('val secondaryAudio: String,val easy:Boolean' in s,'mode missing from source cache key')
require('org.json.JSONArray(listOf(easyModeRP79,audioLanguage' in s,'mode missing from previous-source identity')
branch=s.split('if(easyModeRP79) return@supervisorScope StreamOrdering.sorted(',1)[1].split('val native=',1)[0]
require('easyStreamsRP79.resolve' in branch and 'streamRepo.resolve' not in branch,'easy resolver must be separate')
require('setManifests(com.miflix.native2.data.EasyModePolicyRP79.sourceManifests())' in s,'provider policy not connected')
require('easyModeRP79=state.easyModeRP79' in screens,'basic controls not connected')
basic=controls.split('if(easyModeRP79) {',1)[1].split('val seekFocus=remember',1)[0]
require('onMenu("audio")' in basic and 'onMenu("channels")' in basic,'audio/channels controls missing')
for forbidden in ('onParty()','onLinks()','onInfo()','onQueue()','onPip()','onMultiView()','onEpisodes()'):
    require(forbidden not in basic,'advanced basic-player action: '+forbidden)
require('Screen.FRIENDS' not in ui and 'Screen.SETTINGS' not in ui,'advanced navigation in simple UI')
require('saveEasySnapshotRP79()' in s.split('progress[req.item.cloudId] = row',1)[1][:100],'local progress snapshot missing')
require('cloud.' not in s.split('private fun saveEasySnapshotRP79()',1)[1].split('suspend fun setEasyModeRP79',1)[0],'local snapshot must not write cloud')
require('if(saved.updatedAt>(progress[key]?.updatedAt ?: 0L))' in s,'older local progress must not replace newer cloud progress')
require('versionCode 22183' in (root/'app/build.gradle').read_text(),'wrong build version')
print('PASS: static wiring for routing, social/background gates, cache isolation, basic controls and local progress')
