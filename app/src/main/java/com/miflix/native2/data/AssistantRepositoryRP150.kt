package com.miflix.native2.data

import com.miflix.native2.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import kotlin.math.ceil

internal data class AssistantPickRP150(val item:MediaSummary,val minutes:Int,val episode:EpisodeSummary?=null,val resume:Boolean=false)
internal object AssistantTimeRP150 {
    fun remaining(runtime:Int,progress:PlaybackProgress?):Int {
        val duration=progress?.duration?.takeIf {it>0L} ?: (runtime.coerceAtLeast(0)*60000L)
        if(duration<=0) return 0
        val consumed=progress?.position?.coerceAtLeast(0L) ?: 0L
        return ceil(((duration-consumed).coerceAtLeast(0L))/60000.0).toInt()
    }
}
internal class AssistantRepositoryRP150(private val tmdb:TmdbRepository) {
    private val cache=linkedMapOf<String,Pair<Long,List<AssistantPickRP150>>>()
    private val lock=Mutex()
    suspend fun picks(owner:String,plan:AssistantPlanRP150,continueRows:List<MediaSummary>,progress:Map<String,PlaybackProgress>):List<AssistantPickRP150> = lock.withLock {
        val key="$owner:${tmdb.language}:$plan:${(continueRows.filter {it.type=="movie"}.take(4)+continueRows.filter {it.type=="series"}.take(10)).distinctBy {it.cloudId}.joinToString {"${it.cloudId}:${progress[it.cloudId]?.position}:${progress[it.cloudId]?.duration}:${progress[it.cloudId]?.season}:${progress[it.cloudId]?.episode}"}}"
        cache[key]?.takeIf {System.currentTimeMillis()-it.first<15*60_000L}?.let {return@withLock it.second}
        val gate=Semaphore(3)
        val movies=tmdb.assistantDiscoverRP150(plan).take(12)
        val rows=coroutineScope {movies.map {item ->async {
            gate.withPermit {
                try {val detail=tmdb.details(item);val minutes=detail.runtimeMinutes
                    if(minutes<=0 || minutes<plan.minMinutes || plan.minutes>0 && minutes>plan.minutes) null else AssistantPickRP150(if(item.poster.isNullOrBlank() && item.backdrop.isNullOrBlank()) detail.summary else item,minutes)
                } catch(e:CancellationException) {throw e} catch(_:Exception) {null}
            }
        }}.awaitAll().filterNotNull()}
        val genericTime=plan.minMinutes==0 && plan.minutes>0 && plan.genres.isEmpty() && plan.keyword.isBlank() && plan.query.isBlank() && plan.decade==0
        val resumedMovies=if(genericTime) coroutineScope {
            continueRows.filter {it.type=="movie"}.distinctBy {it.cloudId}.take(4).map {item ->async {gate.withPermit {
                try {val saved=progress[item.cloudId] ?: return@withPermit null
                    val left=if(saved.duration>0) AssistantTimeRP150.remaining(0,saved) else AssistantTimeRP150.remaining(tmdb.details(item).runtimeMinutes,saved)
                    if(saved.position>0 && saved.percent<90 && left in 1..plan.minutes) AssistantPickRP150(item,left,resume=true) else null
                } catch(e:CancellationException) {throw e} catch(_:Exception) {null}
            }}}.awaitAll().filterNotNull()
        } else emptyList()
        val episodes=if(genericTime) {
            val suggestions:List<MediaSummary> = try {tmdb.moodPage("quick",1).rows.filter {it.type=="series"}.take(6)} catch(e:CancellationException) {throw e} catch(_:Exception) {emptyList()}
            val candidates=(continueRows.filter {it.type=="series"}+suggestions).distinctBy {it.cloudId}.take(10)
            coroutineScope {candidates.map {item ->async {gate.withPermit {
                try {val saved=progress[item.cloudId]?.takeIf {it.season>0 && it.episode>0 && it.percent<90}
                    val season=tmdb.season(item.id,saved?.season ?: 1)
                    val ep=if(saved!=null) season.firstOrNull {it.season==saved.season && it.episode==saved.episode} else season.firstOrNull {it.episode>0 && it.runtimeMinutesRP150>0 && it.airDate.isNotBlank() && it.airDate<=java.time.LocalDate.now().toString()}
                    if(ep==null) return@withPermit null
                    val left=AssistantTimeRP150.remaining(ep.runtimeMinutesRP150,saved)
                    if(left in 1..plan.minutes) AssistantPickRP150(item,left,ep,saved!=null && saved.position>0) else null
                } catch(e:CancellationException) {throw e} catch(_:Exception) {null}
            }}}.awaitAll().filterNotNull()}
        } else emptyList()
        val answer=(resumedMovies+episodes+rows).filter { !it.item.poster.isNullOrBlank() || !it.item.backdrop.isNullOrBlank() }.distinctBy {"${it.item.cloudId}:${it.episode?.season}:${it.episode?.episode}"}
        if(cache.size>=12) cache.remove(cache.keys.first())
        cache[key]=System.currentTimeMillis() to answer
        answer
    }
}
