package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal object IptvRepository {
    const val PLAYLIST="https://iptv-org.github.io/iptv/index.category.m3u"
    private val lock=Mutex()
    private var loadedAt=0L
    private var entries=emptyMap<String,List<IptvEntry>>()
    private var channels=emptyList<LiveChannel>()
    private var searchKeys=emptyMap<String,String>()
    private var groupKeys=emptyMap<String,Set<String>>()
    private suspend fun load(force: Boolean=false) = lock.withLock {
        if(!force && channels.isNotEmpty() && System.currentTimeMillis()-loadedAt<6*60*60*1000) return@withLock
        val raw=try { SourceHttp.text(PLAYLIST,"IPTV-org") } catch(e: kotlinx.coroutines.CancellationException) { throw e } catch(e: Exception) { if(!force && channels.isNotEmpty()) return@withLock else throw e }
        val parsed=withContext(Dispatchers.Default) { IptvPlaylist.parse(raw) }
        check(parsed.isNotEmpty()) { "IPTV-org · empty playlist" }
        val grouped=withContext(Dispatchers.Default) { parsed.groupBy { it.id } }
        val nextChannels=withContext(Dispatchers.Default) {
            grouped.values.sortedBy { IptvPlaylist.searchKey(it.first().name) }.mapIndexed { i,rows ->
                val row=rows.first();LiveChannel(row.id,"tv",row.name,row.logo,rows.flatMap { it.groups }.distinct().joinToString(" · "),i+1)
            }
        }
        val nextSearch=withContext(Dispatchers.Default) { nextChannels.associate { it.id to IptvPlaylist.searchKey(it.name) } }
        val nextGroups=withContext(Dispatchers.Default) { grouped.mapValues { (_,rows) -> rows.flatMap { it.groups }.toSet() } }
        entries=grouped;channels=nextChannels;searchKeys=nextSearch;groupKeys=nextGroups
        loadedAt=System.currentTimeMillis()
    }
    suspend fun refresh() { load(true) }
    suspend fun categories(): List<LiveCategory> {
        load()
        val groups=groupKeys.values.flatten().distinct().sorted()
        return listOf(LiveCategory("tv","iptv:all","All / Todos (${channels.size})",paginated=true,searchable=true))+
            groups.map { group -> LiveCategory("tv","iptv:$group",group,group,paginated=true,searchable=true) }
    }
    suspend fun channels(category: LiveCategory,skip: Int,search: String): List<LiveChannel> {
        load();val query=IptvPlaylist.searchKey(search.trim())
        val rows=channels;val groups=groupKeys;val names=searchKeys
        return withContext(Dispatchers.Default) { rows.asSequence().filter { channel ->
            (category.genre==null || category.genre in groups[channel.id].orEmpty()) &&
                (query.isBlank() || names[channel.id].orEmpty().contains(query) || channel.number.toString()==query)
        }.drop(skip.coerceAtLeast(0)).take(200).toList() }
    }
    suspend fun streams(channel: LiveChannel): List<StreamChoice> {
        load()
        return entries[channel.id].orEmpty().map { StreamChoice("IPTV-org",it.url,it.name,requestHeaders=it.headers,addonName="IPTV-org") }
    }
}
