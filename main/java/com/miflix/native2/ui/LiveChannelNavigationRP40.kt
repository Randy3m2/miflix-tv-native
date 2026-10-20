package com.miflix.native2.ui

import com.miflix.native2.data.*
import com.miflix.native2.model.*
import kotlinx.coroutines.CancellationException

internal data class LiveBrowseSnapshotRP40(
    val category:BrowserCategoryRP34,val rows:List<BrowserChannelRP34>,val search:String="",
    val offset:Int=rows.size,val more:Boolean=false,val scrollIndex:Int=0,val scrollOffset:Int=0
)
/** Small, per-profile session memory. No backend writes or polling. */
internal object LiveChannelNavigationRP40 {
    private val snapshots=linkedMapOf<String,LiveBrowseSnapshotRP40>()
    private var hiddenPlaybackId:String?=null
    private fun owner(state:AppState)="${state.session?.userId ?: "local"}:${state.activeProfile.id}"
    fun save(state:AppState,snapshot:LiveBrowseSnapshotRP40) {
        val key=owner(state);snapshots.remove(key);snapshots[key]=snapshot
        while(snapshots.size>6) snapshots.remove(snapshots.keys.first())
    }
    fun matching(state:AppState,request:PlayerRequest?):LiveBrowseSnapshotRP40? {
        if(request?.live!=true) return null
        return snapshots[owner(state)]?.takeIf {snapshot -> snapshot.rows.any {it.provider==request.liveProvider && it.value.id==request.liveChannelId}}
    }
    fun startHidden(request:PlayerRequest)=request.playbackId==hiddenPlaybackId
    suspend fun step(state:AppState,request:PlayerRequest,direction:Int,chooseSports:((BrowserChannelRP34)->Unit)?=null) {
        if(state.kidsMode || state.partyRole==PartyRole.GUEST || state.playerRequest?.playbackId!=request.playbackId) return
        val snapshot=matching(state,request) ?: return
        val index=snapshot.rows.indexOfFirst {it.provider==request.liveProvider && it.value.id==request.liveChannelId}
        val target=snapshot.rows.getOrNull(index+direction) ?: return
        if(target.provider=="sports") {chooseSports?.invoke(target);return}
        val profile=owner(state)
        try {
            val stream=LiveRepository().streams(state.calendarLiveManifest(target.provider),target.value).firstOrNull()
                ?: error("No playable links")
            if(profile!=owner(state) || state.playerRequest?.playbackId!=request.playbackId || state.partyRole==PartyRole.GUEST) return
            val c=target.value
            val next=PlayerRequest(MediaSummary(c.id.hashCode(),"live",c.name,c.description,null,c.poster,0.0,"LIVE"),stream,0,0,0L,
                live=true,liveChannelId=c.id,liveProvider=target.provider,liveType=c.type)
            hiddenPlaybackId=next.playbackId
            state.openLivePlayer(next)
        } catch(e:CancellationException) {throw e} catch(_:Exception) {
            if(profile==owner(state) && state.playerRequest?.playbackId==request.playbackId)
                state.error=tr("No se pudo abrir el canal.","Could not open the channel.")
        }
    }
}
