"""Source wiring checks, not an Android runtime test."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]/'app/src/main/java/com/miflix/native2'
s=(r/'ui/AppState.kt').read_text();settings=(r/'ui/NativeSourcesSettings.kt').read_text();repo=(r/'data/NativeSourceRepository.kt').read_text();ordering=(r/'data/StreamOrdering.kt').read_text()
assert 'profileAudioKeyRP77()="audio_rp77:${localExtrasScope()}"' in s
assert 'putString("audio",audio)' not in s
assert 'native_rp77:$name:${localExtrasScope()}' in s
assert 'spanishOnly=nativeSpanishOnlyRP77,sizeAscending=nativeSizeAscendingRP77' in s
assert 'nativeSpanishOnly:Boolean,val nativeSizeAscending:Boolean' in s
assert 'nativeOnlyRP75,nativeSpanishOnlyRP77,nativeSizeAscendingRP77' in s
assert 'StreamOrdering.nativeAllowedRP77(it,audioLanguage,nativeSpanishOnlyRP77)' in s
for line in s.splitlines():
 if 'StreamOrdering.sorted(' in line and line.rstrip().endswith(') }'):
  assert 'nativeSpanishOnly=' in line
assert 'maxBytes,preferredAudio,spanishOnly,sizeAscending' in repo
assert 'matching.filter' in repo and 'StreamOrdering.sorted(options,preferredAudio' in repo
assert 'spanishVariantRank' in repo and 'nativeSpanishOnly=spanishOnly' in repo
assert 'nativeAllowedRP77(it,preferredAudio,nativeSpanishOnly)' in ordering
assert 'nativeSizeAscending && nativeRP77(it)' in ordering
assert 'state.saveNativeSpanishOnlyRP77' in settings and 'state.saveNativeSizeAscendingRP77' in settings
print('PASS profile-scoped audio/options, playback-audio-only wiring, result/file selection, cache/source-memory context and native-only filters')
