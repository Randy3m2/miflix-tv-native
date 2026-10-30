package com.miflix.native2.data

import com.miflix.native2.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import org.json.JSONObject

internal data class AssistantPersonQueryRP157(val name:String,val role:String="actor",val type:String="movie")
internal data class AssistantTitleGuessRP157(val title:String,val type:String,val year:Int=0)
internal object AssistantCatalogRP157 {
    private val peopleCache=java.util.WeakHashMap<TmdbRepository,LinkedHashMap<String,Pair<Long,Pair<List<AssistantPickRP150>,List<String>>>>>()
    private val peopleLock=Mutex()

    fun person(text:String):AssistantPersonQueryRP157? {
        val match=Regex("(?i)(?:pel[ií]culas?|series|movies?|films?|shows?)\\s+(?:(?:donde|en (?:las|la) que)\\s+(?:sale|act[uú]a|aparece|participa)|(?:con|de|with|starring|featuring)|(?:dirigid[ao]s? por|directed by))\\s+(.+)").find(text) ?: return trailingPersonRP160(text)
        val name=match.groupValues[1].split(Regex("(?i),|\\s+(?:que duren|menos de|m[aá]s de|under|over)\\s+")).first().trim().take(100)
        if(name.length<3) return null
        val normalized=DiscoveryMood.normalize(name)
        val filters=AssistantPlanRP150.local(name)
        if(Regex("^(?:un|una|unos|unas|a|an|the) ").containsMatchIn(normalized) || filters.genres.isNotEmpty() || filters.keyword.isNotBlank() || filters.decade>0) return null
        return AssistantPersonQueryRP157(name,if(Regex("(?i)dirigid|directed").containsMatchIn(match.value)) "director" else "actor",if(Regex("(?i)^(series|shows?)").containsMatchIn(match.value)) "series" else "movie")
    }
    private fun trailingPersonRP160(text:String):AssistantPersonQueryRP157? {
        val stripped=text.trim().replace(Regex("(?i)^(?:(?:can|could|would) you )?(?:please )?(?:find|show|search|get|recommend)(?: for me| me)? (?:some |any |a |the )?"),"")
        val match=Regex("(?i)^(.+?)\\s+(movies?|films?|series|shows?)[?.!]*$").find(stripped) ?: return null
        val name=match.groupValues[1].trim()
        if(name.split(Regex("\\s+")).size !in 2..5 || name.length>100) return null
        val filters=AssistantPlanRP150.local(name)
        if(filters.genres.isNotEmpty() || filters.keyword.isNotBlank() || filters.decade>0) return null
        return AssistantPersonQueryRP157(name,type=if(match.groupValues[2].lowercase() in listOf("series","show","shows")) "series" else "movie")
    }
    fun description(text:String):Boolean {
        val q=DiscoveryMood.normalize(text)
        return person(text)==null && Regex("(?:pelicula|movie|film|serie|show) (?:donde|en la que|en el que|where|about|de un|de una)|(?:no recuerdo|como se llama|what is the name|identify|identifica)").containsMatchIn(q)
    }
    fun guesses(raw:String):List<AssistantTitleGuessRP157> {
        val a=JSONObject(raw).optJSONArray("titles") ?: return emptyList()
        return (0 until minOf(3,a.length())).mapNotNull {i ->
            val j=a.optJSONObject(i) ?: return@mapNotNull null
            val title=j.optString("title").trim();val type=j.optString("type");val year=j.optInt("year")
            if(title.length !in 1..150 || title.contains("://") || title.any {it.isISOControl()} || type !in listOf("movie","series") || year!=0 && year !in 1880..2100) null else AssistantTitleGuessRP157(title,type,year)
        }.distinct()
    }
    suspend fun identify(tmdb:TmdbRepository,guesses:List<AssistantTitleGuessRP157>):List<AssistantPickRP150> {
        val rows=mutableListOf<AssistantPickRP150>()
        for(guess in guesses.take(3)) {
            val candidates=tmdb.search(guess.title).filter {it.type==guess.type && (guess.year==0 || it.year==guess.year.toString())}.take(4)
            val matches=candidates.filter {item ->
                val expected=DiscoveryMood.normalize(guess.title)
                DiscoveryMood.normalize(item.title)==expected || DiscoveryMood.normalize(tmdb.details(item).originalTitle)==expected
            }
            // A title shared by multiple releases remains a choice, never an automatic first-result jump.
            for(item in matches) rows+=AssistantPickRP150(item,tmdb.details(item).runtimeMinutes)
        }
        return rows.distinctBy {it.item.cloudId}
    }
    suspend fun filmography(tmdb:TmdbRepository,query:AssistantPersonQueryRP157,plan:AssistantPlanRP150):Pair<List<AssistantPickRP150>,List<String>> = peopleLock.withLock {
        val key="${tmdb.language}:$query:$plan"
        val cache=peopleCache.getOrPut(tmdb) {linkedMapOf()}
        cache[key]?.takeIf {System.currentTimeMillis()-it.first<15*60_000L}?.let {return@withLock it.second}
        val people=tmdb.filterPeople(query.name)
        val exact=people.filter {DiscoveryMood.normalize(it.name)==DiscoveryMood.normalize(query.name)}
        val matchingRole=exact.filter {it.department.equals(if(query.role=="director") "Directing" else "Acting",true)}
        val resolved=matchingRole.singleOrNull() ?: exact.singleOrNull()
        if(resolved==null) return@withLock emptyList<AssistantPickRP150>() to people.take(4).map {it.name+it.knownFor.takeIf {known->known.isNotBlank()}?.let {known->" · $known"}.orEmpty()}
        val items=tmdb.personCreditsRP141(resolved.id,query.role).filter {it.type==query.type}.take(30)
        val gate=Semaphore(3)
        val rows=coroutineScope {items.map {item ->async {gate.withPermit {
            try {
                val detail=tmdb.details(item);val duration=detail.runtimeMinutes
                if((plan.minMinutes>0 && duration<plan.minMinutes) || (plan.minutes>0 && (duration<=0 || duration>plan.minutes)) || (plan.decade>0 && (item.year.toIntOrNull() ?: 0) !in plan.decade..plan.decade+9) || (plan.genres.isNotEmpty() && !detail.genreIds.containsAll(plan.genres))) null
                else AssistantPickRP150(item,duration)
            } catch(e:CancellationException) {throw e} catch(_:Exception) {null}
        }}}.awaitAll().filterNotNull()}
        val answer=rows to emptyList<String>()
        if(cache.size>=12) cache.remove(cache.keys.first())
        cache[key]=System.currentTimeMillis() to answer
        answer
    }
}
