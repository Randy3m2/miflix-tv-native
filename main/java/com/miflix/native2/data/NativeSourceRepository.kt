package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

data class NativeSource(val hash: String,val fileName: String,val fileSize: Long)

/** Native search → cached-file verification → selected file resolution. No Stremio manifest needed. */
class NativeSourceRepository(private val transport: suspend (String,String,Map<String,String>,String?,String) -> String =
    { url,service,headers,body,contentType -> SourceHttp.text(url,service,headers,body,contentType) }) {
    private val base="https://api.torbox.app/v1/api"
    private val lock=Mutex()
    private val playbackLock=Mutex()
    private data class SearchPage(val at: Long,val hits: List<JSONObject>)
    private val pages=java.util.concurrent.ConcurrentHashMap<String,SearchPage>()
    private var lastQuery=""
    private var lastOwner=""
    private var lastAt=0L
    private var lastResults=emptyList<StreamChoice>()
    private var blockedUntil=0L
    private fun enc(value: String)=URLEncoder.encode(value,"UTF-8")
    private suspend fun api(path: String,token: String,body: String?=null,contentType: String="application/json"): JSONObject {
        check(System.currentTimeMillis()>=blockedUntil) { "TorBox · espera un minuto / wait one minute" }
        val json=try { JSONObject(transport("$base/$path","TorBox",mapOf("Authorization" to "Bearer $token"),body,contentType)) }
        catch(e: Exception) {
            if(e is CancellationException) throw e
            if(e.message?.contains("429")==true) blockedUntil=System.currentTimeMillis()+60_000
            throw e
        }
        check(json.optBoolean("success")) { "TorBox · "+json.optString("error","request failed").replace(Regex("[^a-zA-Z0-9_ -]"),"").take(80) }
        return json
    }
    suspend fun validate(token: String) { require(token.isNotBlank());api("user/me",token) }
    fun invalidate() { pages.clear();lastQuery="";lastOwner="";lastResults=emptyList();lastAt=0 }
    private fun files(value: JSONObject): List<JSONObject> {
        val a=value.optJSONArray("files") ?: return emptyList()
        return (0 until a.length()).mapNotNull { a.optJSONObject(it) }
    }
    private fun fileName(file: JSONObject)=file.optString("name").ifBlank { file.optString("short_name") }
    suspend fun search(owner: String,token: String,titles: List<String>,type: String,year: String,season: Int,episode: Int,maxBytes: Long,preferredAudio: String = ""): List<StreamChoice> = lock.withLock {
        val names=titles.filter { it.isNotBlank() }.distinctBy { NativeSourceMatching.normalize(it) }.take(2)
        val fingerprint=java.security.MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
        val cacheKey=listOf(fingerprint,names.joinToString("|"),type,year,season,episode,maxBytes,preferredAudio).joinToString(":")
        if(lastOwner==owner && lastQuery==cacheKey && System.currentTimeMillis()-lastAt<120_000) return@withLock lastResults
        // Cache index results by season, but verify TorBox availability for every new episode.
        val indexKey=listOf(owner,fingerprint,names.joinToString("|"),type,year,season,preferredAudio).joinToString(":")
        val page=pages[indexKey]?.takeIf { System.currentTimeMillis()-it.at<120_000 }
        val hits=page?.hits ?: supervisorScope {
            val queries=NativeSearchPlan.queries(names,type,year,season,StreamLanguagePriority.prefersSpanish(preferredAudio))
            val outcomes=queries.map { query -> async {
                try {
                    currentCoroutineContext().ensureActive()
                    val request=JSONObject().put("search_type","100%").put("search_field","title").put("query",query)
                        .put("size",60).put("order_by","seeders").put("order_direction","desc").put("hide_unsafe",true).put("hide_xxx",true)
                    val a=JSONObject(transport("https://api.knaben.org/v1","Knaben",emptyMap(),request.toString(),"application/json")).optJSONArray("hits") ?: JSONArray()
                    Result.success((0 until a.length()).mapNotNull { a.optJSONObject(it) })
                } catch(e: CancellationException) { throw e } catch(e: Exception) { Result.failure<List<JSONObject>>(e) }
            } }.awaitAll()
            if(outcomes.isNotEmpty() && outcomes.all { it.isFailure }) throw outcomes.first().exceptionOrNull()!!
            outcomes.flatMap { it.getOrNull().orEmpty() }.also {
                if(pages.size>=8) pages.minByOrNull { it.value.at }?.key?.let { key -> pages.remove(key) }
                pages[indexKey]=SearchPage(System.currentTimeMillis(),it)
            }
        }
        val candidates=hits.filter { h ->
            val name=h.optString("title")
            h.optString("hash").matches(Regex("(?i)[a-f0-9]{40}")) && NativeSourceMatching.titleMatches(name,names) &&
                (type!="movie" || names.any { StreamFilters.releaseYear(name,it)==year.take(4).toIntOrNull() })
        }.distinctBy { it.optString("hash").lowercase(Locale.ROOT) }
            .sortedBy { if(StreamLanguagePriority.prefersSpanish(preferredAudio)) StreamLanguagePriority.rank("",it.optString("title")) else 0 }.take(80)
        if(candidates.isEmpty()) return@withLock emptyList()
        val hashes=candidates.joinToString(",") { it.getString("hash").lowercase(Locale.ROOT) }
        val cached=api("torrents/checkcached?hash=$hashes&format=object&list_files=true",token).optJSONObject("data") ?: JSONObject()
        val results=candidates.mapNotNull { hit ->
            val hash=hit.getString("hash").lowercase(Locale.ROOT)
            val data=cached.optJSONObject(hash) ?: cached.optJSONObject(hash.uppercase(Locale.ROOT)) ?: return@mapNotNull null
            val videos=files(data).filter { NativeSourceMatching.video(fileName(it)) }
            val matching=if(type=="series") videos.filter { NativeSourceMatching.episodeMatches(fileName(it),season,episode) }
                else if(videos.size==1) videos else videos.filter { NativeSourceMatching.titleMatches(fileName(it).substringAfterLast('/'),names) }
            val spanish=StreamLanguagePriority.prefersSpanish(preferredAudio)
            val file=matching.filter { it.optLong("size")>0 && (maxBytes<=0 || it.optLong("size")<=maxBytes) }.sortedWith(compareBy<JSONObject> {
                if(spanish) StreamLanguagePriority.rank("",fileName(it)) else 0
            }.thenByDescending { it.optLong("size") }).firstOrNull() ?: return@mapNotNull null
            val name=fileName(file);val size=file.optLong("size")
            if(size<=0 || maxBytes>0 && size>maxBytes) return@mapNotNull null
            StreamChoice("BruniO Native · TorBox","brunio-native://$hash/${enc(name)}",hit.getString("title")+"\n"+String.format(Locale.US,"%.2f GB",size/1_000_000_000.0),
                filename=name,videoSize=size,addonName="BruniO Native",nativeSource=NativeSource(hash,name,size))
        }
        val sorted=StreamOrdering.sorted(results.filter { StreamFilters.allowed(it,type,year,maxBytes,names.firstOrNull()) }, preferredAudio)
        lastOwner=owner;lastQuery=cacheKey;lastAt=System.currentTimeMillis();lastResults=sorted
        sorted
    }
    suspend fun materialize(stream: StreamChoice,token: String,stillValid: () -> Boolean): StreamChoice = playbackLock.withLock {
        val source=stream.nativeSource ?: return@withLock stream
        currentCoroutineContext().ensureActive();check(stillValid()) { "Playback changed" }
        // Recheck availability immediately before registering only the selected cached torrent.
        val cached=api("torrents/checkcached?hash=${source.hash}&format=object&list_files=true",token).optJSONObject("data")
        val data=cached?.optJSONObject(source.hash) ?: cached?.optJSONObject(source.hash.uppercase(Locale.ROOT))
        check(data!=null && files(data).any { fileName(it)==source.fileName && it.optLong("size")==source.fileSize }) { "TorBox · cached file no longer available" }
        check(stillValid()) { "Playback changed" }
        val boundary="BruniO${java.util.UUID.randomUUID()}"
        val fields=mapOf("magnet" to "magnet:?xt=urn:btih:${source.hash}","seed" to "3","allow_zip" to "false")
        val body=fields.entries.joinToString("") { (k,v) -> "--$boundary\r\nContent-Disposition: form-data; name=\"$k\"\r\n\r\n$v\r\n" }+"--$boundary--\r\n"
        val created=api("torrents/createtorrent",token,body,"multipart/form-data; boundary=$boundary").optJSONObject("data")
        val id=created?.optLong("torrent_id",-1) ?: -1
        check(id>=0) { "TorBox · cached torrent not ready" };check(stillValid()) { "Playback changed" }
        val torrent=api("torrents/mylist?id=$id&bypass_cache=true",token).optJSONObject("data")
            ?: error("TorBox · file list unavailable; retry")
        check(torrent.optBoolean("download_present")) { "TorBox · file not ready; choose another link" }
        val file=files(torrent).firstOrNull { fileName(it)==source.fileName && it.optLong("size")==source.fileSize }
            ?: files(torrent).filter { fileName(it).substringAfterLast('/')==source.fileName.substringAfterLast('/') && it.optLong("size")==source.fileSize }.singleOrNull()
            ?: error("TorBox · selected file unavailable")
        val fileId=file.optLong("id",-1);check(fileId>=0 && stillValid()) { "Playback changed" }
        val link=api("torrents/requestdl?token=${enc(token)}&torrent_id=$id&file_id=$fileId&redirect=false",token).optString("data")
        check(link.startsWith("https://")) { "TorBox · invalid playback link" }
        check(stillValid()) { "Playback changed" }
        stream.copy(url=link,nativeSource=null,sourceUrl=stream.sourceUrl.ifBlank {stream.url})
    }
}
