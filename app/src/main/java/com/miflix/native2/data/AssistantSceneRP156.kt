package com.miflix.native2.data

import com.miflix.native2.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

internal data class AssistantSceneHintRP156(val series:String,val clue:String,val season:Int=0,val episode:Int=0,val title:String="",val alternateSeries:List<String> = emptyList(),val absoluteEpisode:Int=0)
internal data class AssistantSceneHitRP156(val show:MediaSummary,val episode:EpisodeSummary,val suggested:Boolean)
internal object AssistantSceneRP156 {
    private val cache=linkedMapOf<String,Pair<Long,List<EpisodeSummary>>>()
    private val lock=Mutex()
    private suspend fun episodes(tmdb:TmdbRepository,id:Int,seasons:List<Int>,gate:Semaphore,english:Boolean=false):List<EpisodeSummary> = lock.withLock {
        val key="${tmdb.language}:$english:$id:${seasons.joinToString()}"
        cache[key]?.takeIf {System.currentTimeMillis()-it.first<6*60*60_000L}?.let {return@withLock it.second}
        val rows=coroutineScope {seasons.map {season ->async {gate.withPermit {if(english) tmdb.assistantEnglishSeasonRP157(id,season) else tmdb.season(id,season)}}}.awaitAll().flatten()}
        if(cache.size>=6) cache.remove(cache.keys.first())
        cache[key]=System.currentTimeMillis() to rows
        rows
    }

    fun detects(text:String):Boolean {
        val q=DiscoveryMood.normalize(text)
        return Regex("(?:episodio|capitulo|episode|scene|escena|scene finder|timestamp)").containsMatchIn(q) &&
            !Regex("^(?:pon |abre )?(?:el )?siguiente episodio$").matches(q)
    }
    fun local(text:String):AssistantSceneHintRP156 {
        val series=Regex("(?i)(?:episodio|cap[ií]tulo|episode)\\s+(?:de |of )?(.+?)(?:\\s+(?:donde|en que|where|when)\\b|$)").find(text)?.groupValues?.get(1)?.trim().orEmpty()
        val clue=text.replace(Regex("(?is)^.*?\\b(?:donde|en que|where|when)\\s+"),"").take(200)
        return AssistantSceneHintRP156(series.take(100),clue)
    }
    suspend fun find(tmdb:TmdbRepository,hint:AssistantSceneHintRP156):List<AssistantSceneHitRP156> {
        if(hint.series.isBlank()) return emptyList()
        val names=(listOf(hint.series)+hint.alternateSeries).filter {it.isNotBlank()}.distinct().take(3)
        val shows=names.flatMap {name ->
            val candidates=tmdb.search(name).filter {it.type=="series"}.take(4)
            val exact=candidates.filter {DiscoveryMood.normalize(it.title)==DiscoveryMood.normalize(name)}
            exact.ifEmpty {candidates.take(1)}.take(1)
        }.distinctBy {it.cloudId}
        val gate=Semaphore(3)
        val hits=mutableListOf<AssistantSceneHitRP156>()
        for(show in shows) {
            val detail=tmdb.details(show)
            val seasons=(1..minOf(detail.seasonCount,40)).toList()
            val rows=episodes(tmdb,show.id,seasons,gate)
            val english=if(hint.title.isNotBlank() && tmdb.language!="en-US" && rows.none {DiscoveryMood.normalize(it.title)==DiscoveryMood.normalize(hint.title)})
                episodes(tmdb,show.id,rows.map {it.season}.distinct().sorted(),gate,true) else emptyList()
            val named=(rows+english).filter {hint.title.isNotBlank() && DiscoveryMood.normalize(it.title)==DiscoveryMood.normalize(hint.title)}.map {it.season to it.episode}.toSet()
            val tokens=DiscoveryMood.normalize(hint.clue).split(' ').filter {it.length>=4 && it !in setOf("episodio","capitulo","donde","episode","where","scene","escena","dice","esta","unico","mantiene")}.distinct()
            rows.filter {it.airDate.isNotBlank() && it.airDate<=java.time.LocalDate.now().toString()}.forEach {ep ->
                val metadata=DiscoveryMood.normalize(ep.title+" "+ep.overview)
                val literal=tokens.size>=2 && tokens.all {metadata.contains(it)}
                val proposed=(ep.season to ep.episode) in named
                if(literal || proposed) hits+=AssistantSceneHitRP156(show,ep,!literal)
            }
        }
        return hits.distinctBy {"${it.show.cloudId}:${it.episode.season}:${it.episode.episode}"}.take(8)
    }
    fun hint(json:String):AssistantSceneHintRP156 {
        val j=JSONObject(json)
        return AssistantSceneHintRP156(j.optString("series").take(100),j.optString("clue").take(200),j.optInt("season").coerceIn(0,100),j.optInt("episode").coerceIn(0,2000),j.optString("title").take(150),(0 until minOf(2,j.optJSONArray("alternateSeries")?.length() ?: 0)).map {j.getJSONArray("alternateSeries").optString(it).take(100)}.filter {it.isNotBlank() && !it.contains("://")},j.optInt("absoluteEpisode").coerceIn(0,2000))
    }
}
