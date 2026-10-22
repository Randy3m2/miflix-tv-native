"""Static regression checks. This does not compile Android or simulate a TV remote."""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
u=root/'app/src/main/java/com/miflix/native2/ui'
plan=(u/'EasyHomeRowsRP80.kt').read_text()
rows=re.findall(r'EasyHomeRowRP80\("([^"]+)","([^"]+)","([^"]+)","([^"]+)"(?:,(\d+))?\)',plan)
assert len(rows)==24, len(rows)
assert len({r[0] for r in rows})==24,'duplicate row keys'
allowed={'novelas','comfort','classics','kdrama','romance','top_movies','top_series','now_playing','movie','series'}
assert all(r[3] in allowed for r in rows),'unknown loader kind'
assert all(int(r[4])>0 for r in rows if r[3] in {'movie','series'}),'missing genre'
assert {'novelas','comfort','classics','romance','korean','westerns','documentaries','drama_series','comedy_series'} <= {r[0] for r in rows}
assert 'pages=1,limit=20' in plan,'unbounded genre queries'
for forbidden in ('social.','cloud.','streamRepo','nativeSources','Supabase'):
    assert forbidden not in plan,forbidden
state=(u/'AppState.kt').read_text()
a=state.index('    val easyRowsRP80=');b=state.index('    suspend fun easyResumeRP79',a)
loader=state[a:b]
assert 'Semaphore(3)' in loader
assert 'easyRowsAttemptedRP80 && !retry' in loader,'returning Home must reuse completed catalog'
assert 'filter {it.id !in easyRowsRP80}' in loader,'retry must retain successful rows'
assert 'if(current() && titles.isNotEmpty())' in loader,'stale profile/language data must not publish'
assert 'catch(e:CancellationException) {throw e}' in loader,'navigation cancellation must propagate'
assert 'if(!completed) easyRowsAttemptedRP80=false' in loader,'cancelled loads must resume remaining rows'
for forbidden in ('social.','cloud.','streamRepo','pushCloud','refreshSocial'):
    assert forbidden not in loader,forbidden
ui=(u/'EasyModeRP79.kt').read_text()
assert 'SearchScreen(state)' in ui and 'EasySearchRP79' not in ui,'original search must be used'
search_branch=ui.split('if(screen==Screen.SEARCH)',1)[1].split('AppShell(state,screen',1)[0]
assert 'LocalEasySearchRP80 provides true' in search_branch and 'return' in search_branch,'search must have exactly one shell'
assert 'LaunchedEffect(owner) {state.loadEasyRowsRP80()}' in ui
assert 'item(key="easy-continue")' in ui and 'item(key="easy-movies")' in ui and 'item(key="easy-series")' in ui
assert 'item(key="easy-extra:${row.id}")' in ui,'stable loading/scroll anchors missing'
assert ui.index('easy-continue')<ui.index('easy-movies')<ui.index('easy-extra:'),'Continue Watching must be first'
search=(u/'CinematicBrowsing.kt').read_text()
assert 'internal val LocalEasySearchRP80=staticCompositionLocalOf {false}' in search,'normal search must keep original default'
assert '"abcdefghijklmnopqrstuvwxyz1234567890".chunked(6)' in search
assert 'SearchTextEditor.move' in search and 'voice.launch(intent)' in search
assert 'fontSize=if(largeRP80) 16.sp else 13.sp' in search,'keyboard size regression'
assert 'width(if(easySearchRP80) 250.dp else 220.dp)' in search,'normal keyboard width changed'
assert 'allowContextMenu=!state.easyModeRP79' in search,'advanced title menus exposed'
assert 'if(!state.easyModeRP79 && com.miflix.native2.data.DiscoveryMood.intent' in search
assert 'versionCode 22184' in (root/'app/build.gradle').read_text()
hub=(u/'LiveHubRP41.kt').read_text()
logo=(u/'EasyExitLogoRP80.kt').read_text()
assert 'EasyMlbRP80(state' in ui and 'HubSportsRP41(state,"baseball",modifier,{})' in hub
assert 'val manifest=state.calendarLiveManifest("sports")' in hub
assert 'state.sportsRequestRP71(request,automatic)' in hub
assert 'manual && !state.easyModeRP79' in hub
assert 'state.easyMlbRP80=false;state.screen=Screen.LIVE_TV' in ui
assert 'state.easyMlbRP80=true;state.screen=Screen.LIVE_TV' in ui
assert 'easyMlbByOwnerRP80[localExtrasScope()]' in state
assert 'EasyExitLogoRP80(state) {exit=true}' in ui and 'onLongClick={exit=true}' not in ui
assert 'gate.up(key.keyCode,key.eventTime)' in logo and 'if(done) latestUnlock()' in logo
assert 'if(!it.isFocused) cancel()' in logo and 'if(!state.appForeground) cancel()' in logo
assert 'waitForUpOrCancellation()' in logo
print('PASS: 24 additional row definitions + 2 popular rows, original search, Easy-only typography, cache/owner/cancellation wiring, catalog-only metadata calls and configured MLB sports source')
