"""Source integration checks, not an Android compilation."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]/'app/src/main/java/com/miflix/native2'
s=(r/'ui/Screens.kt').read_text();state=(r/'ui/AppState.kt').read_text();order=(r/'data/StreamOrdering.kt').read_text();subs=(r/'data/SubtitleRepository.kt').read_text();streams=(r/'data/StreamRepository.kt').read_text();native=(r/'data/NativeSourceRepository.kt').read_text()
assert s.count('"ko" to "Korean · 한국어"')==3
assert 'listOf("ko-KR","ko")' in s and 'setPreferredTextLanguage(normalizeMediaLanguage(preferredText))' in s
assert 'setOf("","es","en","pt","fr","ko")' in state
assert 'subtitle_rp78:${localExtrasScope()}' in state and 'putString("subtitle",subtitle)' not in state
assert '${localExtrasScope()}:$subtitleLanguage:${item.cloudId}' in state
assert 'preferredLanguage=preferredSubtitleRP78' in state and 'preferredLanguage=subtitleLanguage' in state
assert 'else if(korean) {if(hasAudioRP62(it,preferredAudio)) 0' in order
assert 'preferred.isBlank()) hasEs && hasEn else primary.any' in subs
assert 'normalizeKoreanLanguage(x)' in subs and 'normalizeKoreanLanguage(x)' in streams
assert 'NativeSearchPlan.queries(names,type,year,season,preferredAudio)' in native
assert 'StreamLanguagePriority.prefersKorean(preferredAudio)' in native
assert 'Inline notice: no Dialog/window' in s
assert 'TorBoxSetupRP75(state)' not in s
print('PASS three Korean selectors, per-profile subtitles, player/normalizer wiring, preferred-language fallback, general/native source ranking and optional TorBox preservation')
