package com.miflix.native2.data

import com.miflix.native2.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

internal data class AssistantSceneHintRP156(val series:String,val clue:String,val season:Int=0,val episode:Int=0,val title:String="")
internal data class AssistantSceneHitRP156(val show:MediaSummary,val episode:EpisodeSummary,val suggested:Boolean)
internal object AssistantSceneRP156 {
    private val cache=linkedMapOf<String,Pair<Long,List<EpisodeSummary>>>()
    private val lock=Mutex()
    private suspend fun episodes(tmdb:TmdbRepository,id:Int,seasons:List<Int>,gate:Semaphore):List<EpisodeSummary> = lock.withLock {
        val key="${tmdb.language}:$id:${seasons.joinToString()}"
        cache[key]?.takeIf {System.currentTimeMillis()-it.first<6*60*60_000L}?.let {return@withLock it.second}
        val rows=coroutineScope {seasons.map {season ->async {gate.withPermit {tmdb.season(id,season)}}}.awaitAll().flatten()}
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
        val candidates=tmdb.search(hint.series).filter {it.type=="series"}.take(3)
        val exact=candidates.filter {DiscoveryMood.normalize(it.title)==DiscoveryMood.normalize(hint.series)}
        val shows=exact.ifEmpty {candidates}.take(1)
        val gate=Semaphore(3)
        val hits=mutableListOf<AssistantSceneHitRP156>()
        for(show in shows) {
            val detail=tmdb.details(show)
            val seasons=if(hint.season>0) listOf(hint.season).filter {it<=detail.seasonCount} else (1..minOf(detail.seasonCount,40)).toList()
            val rows=episodes(tmdb,show.id,seasons,gate)
            val tokens=DiscoveryMood.normalize(hint.clue).split(' ').filter {it.length>=4 && it !in setOf("episodio","capitulo","donde","episode","where","scene","escena","dice","esta","unico","mantiene")}.distinct()
            rows.filter {it.airDate.isNotBlank() && it.airDate<=java.time.LocalDate.now().toString()}.forEach {ep ->
                val metadata=DiscoveryMood.normalize(ep.title+" "+ep.overview)
                val literal=tokens.size>=2 && tokens.all {metadata.contains(it)}
                val proposed=hint.episode>0 && hint.season>0 && ep.season==hint.season && ep.episode==hint.episode && hint.title.isNotBlank() && DiscoveryMood.normalize(ep.title)==DiscoveryMood.normalize(hint.title)
                if(literal || proposed) hits+=AssistantSceneHitRP156(show,ep,!literal)
            }
        }
        return hits.distinctBy {"${it.show.cloudId}:${it.episode.season}:${it.episode.episode}"}.take(8)
    }
    fun hint(json:String):AssistantSceneHintRP156 {
        val j=JSONObject(json)
        return AssistantSceneHintRP156(j.optString("series").take(100),j.optString("clue").take(200),j.optInt("season").coerceIn(0,100),j.optInt("episode").coerceIn(0,2000),j.optString("title").take(150))
    }
}
