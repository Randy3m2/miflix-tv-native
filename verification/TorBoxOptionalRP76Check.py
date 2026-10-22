"""Integration checks; no Android runtime or live TorBox credentials required."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
u=root/'app/src/main/java/com/miflix/native2/ui';d=root/'app/src/main/java/com/miflix/native2/data'
s=(u/'Screens.kt').read_text();settings=(u/'NativeSourcesSettings.kt').read_text();qr=(u/'TorBoxKeyQrRP76.kt').read_text();store=(d/'NativeSourceStore.kt').read_text();server=(d/'TorBoxKeyServerRP76.java').read_text()
assert 'TorBoxSetupRP75(state)' not in s and 'offerTorBoxRP75' not in s
assert 'if(showTasteSetup && !state.kidsMode) TasteOnboardingRC102' in s
assert 'prefs.getBoolean("enabled_$owner",false)' in store
assert 'prefs.getBoolean("only_native_rp75_$owner",false)' in store
assert 'state.toggleNativeSources()' in settings and 'state.setNativeOnlyRP75' in settings
assert 'state.saveNativeSourceKey(draft)' in settings and 'activateRP75=true' not in settings
assert 'received={draft=it}' in settings
assert 'saveNativeSourceKey' not in qr and 'toggleNativeSources' not in qr
assert 'onDispose {alive.set(false);server?.close()}' in qr
assert 'CustomChannelsRP45.owner(state)==owner' in qr
assert 'state.screen==Screen.SETTINGS' in qr and '!state.sessionNeedsSignIn' in qr
assert 'type=\'password\'' in server and "method='post'" in server
assert 'name=\'apiKey\'' in server and 'initialUrl' not in server
assert 'if(received)' in server and 'received=true' in server
assert 'Cache-Control: no-store' in server
assert 'Inline notice: no Dialog/window' in s  # RP74 remains.
print('PASS optional defaults, no startup prompt, taste flow, explicit activation, draft-only QR, account/screen guards, receiver disposal and secret handling')
