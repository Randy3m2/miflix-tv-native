package com.miflix.native2.ui

import android.content.Context
import com.miflix.native2.data.*
import kotlinx.coroutines.*

internal data class AssistantLiveRP155(val channel:LiveChannel,val provider:String,val custom:CustomChannelRP45?=null)
internal object AssistantLiveCatalogRP155 {
    private val scores=LiveScoresRP11()
    private val cache=linkedMapOf<String,Pair<Long,List<AssistantLiveRP155>>>()
    private fun matches(name:String,target:String):Boolean {
        val q=DiscoveryMood.normalize(target).split(" ").filter {it.length>=2 || it.toIntOrNull()!=null}
        val text=DiscoveryMood.normalize(name)
        return q.isNotEmpty() && q.all {text.contains(it)}
    }
    suspend fun channels(context:Context,state:AppState,target:String):List<AssistantLiveRP155> = withContext(Dispatchers.IO) {
        val local=CustomChannelsRP45.list(context,CustomChannelsRP45.owner(state)).filter {matches(it.name,target) || it.number.toString()==target.trim()}
            .map {AssistantLiveRP155(LiveChannel(it.id,"tv",it.name,it.logo,it.category,it.number),CustomChannelsRP45.provider,it)}
        if(local.isNotEmpty()) return@withContext local.take(15)
        val manifest=state.calendarLiveManifest("nauta");val key="$manifest:$target"
        synchronized(cache) {cache[key]}?.takeIf {System.currentTimeMillis()-it.first<300_000L}?.let {return@withContext it.second}
        val repo=LiveRepository();val cats=repo.categories(manifest)
        val selected=cats.filter {it.searchable}.take(1).ifEmpty {cats.take(3)}
        val rows=selected.flatMap {repo.channels(manifest,it,search=target)}.filter {matches(it.name,target)}.distinctBy {it.id}.take(15).map {AssistantLiveRP155(it,"nauta")}
        synchronized(cache) {if(cache.size>=12) cache.remove(cache.keys.first());cache[key]=System.currentTimeMillis() to rows}
        rows
    }
    suspend fun games(state:AppState,team:String):Pair<List<AssistantLiveRP155>,String> {
        val normalized=DiscoveryMood.normalize(team).removePrefix("los ").removePrefix("the ")
        require(normalized.length>=2)
        val league=if(listOf("aguilas","tigres","licey","leones","escogido","gigantes","toros","estrellas").any {it in normalized}) "LIDOM" else if(listOf("yankees","dodgers","mets","red sox","padres","astros","blue jays").any {it in normalized}) "MLB" else "NBA"
        val games=scores.games(league,LiveScoresRP11.today()).filter {it.state=="live" && (matches(it.home.name+" "+it.home.short,normalized) || matches(it.away.name+" "+it.away.short,normalized))}
        if(games.isEmpty()) return emptyList<AssistantLiveRP155>() to tr("No encontré un juego en vivo de ese equipo en el marcador actual.","No live game for that team was found in the current scoreboard.")
        val source=state.calendarLiveManifest("sports");val key="$source:$league"
        val existing=synchronized(cache) {cache[key]}?.takeIf {System.currentTimeMillis()-it.first<60_000L}?.second
        val rows=existing ?: run {
            val repo=LiveRepository()
            val cats=repo.categories(source).filter {val q=DiscoveryMood.normalize(it.name+" "+it.id+" "+it.genre.orEmpty());when(league) {"NBA"->"nba" in q || "basket" in q;else->league.lowercase() in q || "baseball" in q}}.take(5)
            val found=cats.flatMap {repo.channels(source,it)}.filterNot {SportsRecordingPolicyRP72.recording(it.id,it.name,it.description)}.distinctBy {it.id}.map {AssistantLiveRP155(it,"sports")}
            synchronized(cache) {if(cache.size>=12) cache.remove(cache.keys.first());cache[key]=System.currentTimeMillis() to found};found
        }
        val choices=rows.filter {r ->matches(r.channel.name+" "+r.channel.description,normalized) && games.any {g ->
            val opponent=if(matches(g.home.name+" "+g.home.short,normalized)) g.away else g.home
            opponent.name.split(" ").lastOrNull()?.let {DiscoveryMood.normalize(it) in DiscoveryMood.normalize(r.channel.name+" "+r.channel.description)}==true || opponent.short.lowercase() in r.channel.name.lowercase()
        }}.take(12)
        return choices to if(choices.isEmpty()) tr("El marcador confirma el juego, pero tu fuente configurada no devolvió un enlace identificable. Puedes revisar Live TV.","The scoreboard confirms the game, but your configured source returned no identifiable event. Check Live TV.") else games.joinToString {"${it.away.short} ${it.away.score} · ${it.home.short} ${it.home.score} · ${it.phase}"}
    }
    suspend fun play(state:AppState,choice:AssistantLiveRP155) {
        check(state.partyRole!=com.miflix.native2.model.PartyRole.GUEST) {"The party host controls playback"}
        choice.custom?.let {state.openLivePlayer(CustomChannelsRP45.request(it));return}
        val snapshot=state.localExtrasScope();val manifest=state.calendarLiveManifest(choice.provider)
        val streams=LiveRepository().streams(manifest,choice.channel);require(streams.isNotEmpty())
        check(snapshot==state.localExtrasScope() && manifest==state.calendarLiveManifest(choice.provider))
        val c=choice.channel
        val request=PlayerRequest(com.miflix.native2.model.MediaSummary(c.id.hashCode(),"live",c.name,c.description,null,c.poster,0.0,"LIVE"),streams.first(),0,0,0L,true,c.id,choice.provider,liveType=c.type,returnScreen=Screen.LIVE_TV)
        state.openLivePlayer(if(choice.provider=="sports") state.sportsRequestRP71(request,streams) else request)
    }
}
