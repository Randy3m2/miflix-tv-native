package com.miflix.native2.ui

import com.miflix.native2.data.AssistantSceneHitRP156

/** Opens the existing episode details UI. Identifying a scene never starts playback. */
internal suspend fun AppState.openAssistantEpisodeRP157(hit:AssistantSceneHitRP156,owner:String) {
    if(localExtrasScope()!=owner) return
    open(hit.show)
    if(localExtrasScope()!=owner || selected?.cloudId!=hit.show.cloudId || screen!=Screen.DETAILS) return
    loadSeason(hit.episode.season)
    if(localExtrasScope()==owner && selected?.cloudId==hit.show.cloudId && screen==Screen.DETAILS) assistantEpisodeRP157=hit
}
