"""Static integration checks; not an Android compile, live TMDB test or microphone test."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
u=root/'app/src/main/java/com/miflix/native2/ui'
d=root/'app/src/main/java/com/miflix/native2/data'
s=(u/'AppState.kt').read_text();repo=(d/'TmdbRepository.kt').read_text();ui=(u/'CinematicBrowsing.kt').read_text()
lookup=s.split('    suspend fun searchFlexible(',1)[1].split('    private data class SourceFallbackPlan',1)[0]
assert 'SearchQueryRP81.parse(query)' in lookup
assert 'tmdb.searchPeopleRP81(parsed,locale)' in lookup
assert 'withTimeoutOrNull(6500L)' in lookup and 'withTimeoutOrNull(4500L)' in lookup
assert 'if(parsed.explicitPerson) filmography' in lookup,'explicit people query must not add unrelated titles'
assert 'else if(filmography.isNotEmpty()) 120 else 60' in lookup
assert 'wasKids) tmdb.kidsFilter(all)' in lookup,'Kids restriction lost'
assert '"en-US" else "es-MX"' in lookup,'bilingual title fallback missing'
assert 'val titles=(remote+local).distinctBy' in lookup,'localized provider result precedence lost'
assert 'aliases[item.cloudId]' in lookup and 'titleScore(it)' in lookup,'translated title aliases not ranked'
assert 'owner!=localExtrasScope()' in lookup and 'locale!=tmdb.language' in lookup
people=repo.split('    private val peopleCacheRP81=',1)[1].split('    suspend fun discoverGenre',1)[0]
assert '"/search/person"' in people and 'combined_credits' in people
assert 'query.role!="director"' in people and 'query.role!="actor"' in people
assert 'it.optString("job")=="Director"' in people,'crew role must be Director'
assert '"Directing"' in people and '"Acting"' in people
assert 'chosen=ordered.filter' in people and '.take(2)' in people
assert 'if(candidates.any {score(it)>=750}) break' in people,'bounded name retry missing'
assert 'score(it)>=700' in people,'unrelated name guard missing'
assert '!credit.optBoolean("adult")' in people and '10763,10767' in people
assert 'query.mediaType=="all"' in people
assert 'if(peopleCacheRP81.size>=16)' in people
for forbidden in ('social.','cloud.','streamRepo','nativeSources','pushCloud'):
    assert forbidden not in lookup and forbidden not in people,forbidden
assert 'EXTRA_LANGUAGE,state.voiceLocaleRP81' in ui
assert 'EXTRA_MAX_RESULTS,5' in ui
assert 'voiceOwnerRP81==state.searchHistoryOwner()' in ui
assert 'voiceAlternativesRP81=heard' in ui and 'items(voiceAlternativesRP81,key={it})' in ui
assert 'state.setVoiceLocaleRP81("es-MX")' in ui and 'state.setVoiceLocaleRP81("en-US")' in ui
assert '"voice_rp81:${localExtrasScope()}"' in s
assert 'state.searchFlexible(query.trim())' in ui
assert 'LocalEasySearchRP80.current' in ui and 'allowContextMenu=!state.easyModeRP79' in ui
assert 'versionCode 22185' in (root/'app/build.gradle').read_text()
print('PASS: shared actor/director search, role/media filters, ES/EN aliases, per-profile voice alternatives, bounded lookup/cache, Kids and stale-owner protection')
