"""Source-level regression checks; no Android runtime or performance benchmark."""
from pathlib import Path
import re
from types import SimpleNamespace as NS
root=Path(__file__).resolve().parents[1]
ui=root/'app/src/main/java/com/miflix/native2/ui'
s=(ui/'Screens.kt').read_text();b=(ui/'LiveChannelBrowserRP34.kt').read_text()
# Evaluate the actual Kotlin navigation guard with equivalent Python booleans.
guard=re.search(r'else if\((request.live[^\n]*\(firstFrame \|\| hadPlaybackError\)[\s\S]*?)\) \{\n            if\(event.type',s).group(1)
assert 'KEYCODE_DPAD_UP' in guard and 'playbackError==null' not in guard
expr=guard.replace('&&',' and ').replace('||',' or ')
expr=re.sub(r'!(?!=)','not ',expr).replace('null','None').replace('\n',' ')
key=NS(KEYCODE_DPAD_UP=19,KEYCODE_DPAD_DOWN=20)
def allowed(**overrides):
 e=dict(request=NS(live=True,sportsRecordingRP72=False),firstFrame=False,hadPlaybackError=True,
        controlsVisible=False,surfaceFocusedRP34=True,showEpisodes=False,showQueue=False,
        showPartyActions=False,showAccessRequests=False,playerMenu=None,state=NS(sourceSelection=None),
        code=19,android=NS(view=NS(KeyEvent=key)),listOf=lambda *x:x)
 e.update(overrides)
 return bool(eval(expr,{'__builtins__':{}},e))
assert allowed() and allowed(code=20)  # Failed before first frame: next and previous.
assert allowed(firstFrame=True)      # Failure after picture also remains navigable.
assert not allowed(firstFrame=False,hadPlaybackError=False) # Loading behavior unchanged.
assert not allowed(playerMenu='channels')                  # Never hijack the panel's keys.
assert not allowed(state=NS(sourceSelection=object()))
assert not allowed(request=NS(live=True,sportsRecordingRP72=True)) # Recap keeps VOD transport.
assert not allowed(request=NS(live=False,sportsRecordingRP72=False))
assert not allowed(controlsVisible=True,surfaceFocusedRP34=False) # Focused action keeps its key.
notice=s.split('if(playbackError!=null && request.live && !request.sportsRecordingRP72) {')[1].split('} else if(playbackError!=null) {')[0]
assert 'delay(5000)' in notice and 'PlayerMenuDialog(' not in notice
assert '.focusable' not in notice and 'requestFocus' not in notice
assert '!firstFrame && !hadPlaybackError' in s
assert b.count('CustomChannelsRP45.list(')==1
assert 'withContext(Dispatchers.IO)' in b
assert 'val displayed=remember(' in b and 'val searchNamesRP74=remember(' in b
assert 'itemsIndexed(displayed' in b and 'displayed.indexOf(entry)' not in b
assert 'entry.key in favoriteKeysRP74' in b
assert 'rememberUpdatedState<()->LiveBrowseSnapshotRP40?>({' in b
assert 'lastBrowseRP40()?.takeIf' in b
assert 'loadedSelectionRP74==selected' in b
assert 'elapsed>=1750L -> optionsNow()' in b and 'elapsed>=750L -> favoriteNow()' in b
print('PASS: failed-channel key routes, non-modal notice, recap/VOD guards, panel cache/lookup paths, delayed snapshot, long-press thresholds')
