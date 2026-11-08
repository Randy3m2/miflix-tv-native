package com.miflix.native2.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

internal object DailyRotationPolicyRP188 {
    const val rowKey="daily_rotation"
    const val maxCards=8
    const val maxProbes=14
    fun candidates(year:Int,day:Int):List<TirsoScheduleItemRP174> {
        val all=Tirso247ScheduleRP174.forDay(year,day).distinctBy {it.isMovie to it.tmdbId}
        val random=Random(year*367+day)
        val movies=all.filter {it.isMovie}.shuffled(random)
        val shows=all.filterNot {it.isMovie}.shuffled(random)
        return buildList {
            for(index in 0 until maxOf(movies.size,shows.size)) {
                movies.getOrNull(index)?.let {add(it)}
                shows.getOrNull(index)?.let {add(it)}
            }
        }.take(maxProbes)
    }
    fun homeOrder(saved:List<String>):List<String> {
        val order=HomeOrderPolicyRP106.order(saved).toMutableList()
        order.remove(rowKey)
        order.add(minOf(4,order.size),rowKey)
        return order
    }
}

/** In-memory, account/settings/date keyed results, including empty-result caching. */
internal class DailyRotationRepositoryRP188(private val clock:()->Long={System.currentTimeMillis()}) {
    private data class Cached(val at:Long,val rows:List<TirsoScheduleItemRP174>,val checked:Set<String>,val complete:Boolean)
    private val cache=linkedMapOf<String,Cached>()
    fun invalidate(key:String) {cache.remove(key)}
    suspend fun load(key:String,candidates:List<TirsoScheduleItemRP174>,verify:suspend (TirsoScheduleItemRP174)->Boolean,onFound:suspend (TirsoScheduleItemRP174)->Unit):List<TirsoScheduleItemRP174> {
        val old=cache[key]?.takeIf {clock()-it.at<5*60*1000L}
        old?.rows?.forEach {onFound(it)}
        if(old?.complete==true) return old.rows
        val rows=old?.rows?.toMutableList() ?: mutableListOf()
        val checked=old?.checked?.toMutableSet() ?: mutableSetOf()
        fun save(complete:Boolean) {
            if(key !in cache && cache.size>=4) cache.remove(cache.keys.first())
            cache[key]=Cached(clock(),rows.toList(),checked.toSet(),complete)
        }
        if(rows.size>=DailyRotationPolicyRP188.maxCards) {save(true);return rows}
        for(entry in candidates.take(DailyRotationPolicyRP188.maxProbes)) {
            if(entry.id in checked) continue
            val valid=try {withTimeoutOrNull(12_000) {verify(entry)}==true} catch(e:CancellationException) {throw e} catch(_:Exception) {false}
            checked+=entry.id
            if(valid) rows+=entry
            save(false)
            if(valid) onFound(entry)
            if(rows.size==DailyRotationPolicyRP188.maxCards) break
        }
        save(true)
        return rows
    }
}
