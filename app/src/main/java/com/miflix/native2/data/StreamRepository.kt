package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import com.miflix.native2.model.SubtitleChoice
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.net.URI

class StreamRepository internal constructor(initialManifestUrl:String,
    private val fetch:suspend (String,Int)->String) {
    constructor(initialManifestUrl:String):this(initialManifestUrl,{url,timeout ->AddonHttpRP12.text(url,timeout)})
    private val torrentCounts=java.util.concurrent.ConcurrentHashMap<String,Int>()
    fun hasTorrentOnlyResults():Boolean = manifests().any {(torrentCounts[it] ?: 0)>0}

    private data class ManifestInfo(val name:String,val streams:Boolean,val at:Long)
    private val statuses=java.util.concurrent.ConcurrentHashMap<String,String>()
    fun status(manifest:String):String = statuses[manifest].orEmpty()
    fun addonLabel(manifest:String):String = label(manifest,manifests().indexOf(manifest).coerceAtLeast(0))
    private val metadata=java.util.concurrent.ConcurrentHashMap<String,ManifestInfo>()
    private fun label(manifest:String,position:Int):String {
        val host=runCatching {URI(manifest.replace("|","%7C")).host}.getOrNull().orEmpty().ifBlank {"Add-on"}
        val known=when(host) {"peerflix.mov"->"Peerflix";"addonlatam.duckdns.org"->"Addon Latam";"torrentio.strem.fun"->"Torrentio";else->host}
        return (metadata[manifest]?.name?.takeIf {it.isNotBlank()} ?: known)+" · "+(position+1)
    }
    suspend fun inspectSourceAddons() = coroutineScope {
        manifests().map {manifest ->async {info(manifest)}}.awaitAll()
        Unit
    }
    internal fun sourceAddons():List<InstalledSourceAddonRP2> = manifests().mapIndexedNotNull {index,url ->
        if(metadata[url]?.streams==false) null else InstalledSourceAddonRP2(AddonPriorityRP2.id(url),label(url,index),runCatching {URI(url).host.orEmpty()}.getOrDefault(""))
    }
    fun sourceAddonNames():List<String> = manifests().mapIndexedNotNull {index,url ->if(metadata[url]?.streams==false) null else label(url,index)}
    private suspend fun info(manifest:String):ManifestInfo {
        val now=System.currentTimeMillis()
        metadata[manifest]?.takeIf {now-it.at<6*60*60_000L}?.let {return it}
        return try {
            val json=JSONObject(fetch(manifest,2000))
            val resources=json.optJSONArray("resources")
            if(json.optJSONObject("behaviorHints")?.optBoolean("configurationRequired")==true)
                statuses[manifest]="Configuration required"
            val streams=resources==null || (0 until resources.length()).any {i ->val r=resources.opt(i);r=="stream" || (r as? JSONObject)?.optString("name")=="stream"}
            ManifestInfo(json.optString("name").replace(Regex("[\\r\\n]")," ").take(64),streams,now).also {metadata[manifest]=it}
        } catch(e:kotlinx.coroutines.CancellationException) {throw e} catch(_:Exception) {ManifestInfo("",true,now).also {metadata[manifest]=it}}
    }
    private val manifestUrls = linkedSetOf<String>()

    init {
        addManifest(initialManifestUrl)
    }

    var manifestUrl: String
        get() = manifestUrls.firstOrNull().orEmpty()
        set(value) {
            if (value.isNotBlank()) {
                manifestUrls.remove(value.trim().replace("|","%7C"))
                val copy = manifestUrls.toList()
                manifestUrls.clear()
                manifestUrls.add(value.trim().replace("|","%7C"))
                manifestUrls.addAll(copy)
            }
        }

    fun manifests(): List<String> = manifestUrls.toList()

    fun setManifests(values: Collection<String>) {
        manifestUrls.clear()
        values.map { it.trim() }.filter { it.isNotBlank() }.forEach { addManifest(it) }
    }

    fun addManifest(value: String): Boolean {
        val clean = value.trim().replace("|","%7C")
        if (clean.isBlank()) return false
        val normalized = if (clean.endsWith("/manifest.json")) clean else clean.trimEnd('/') + "/manifest.json"
        return manifestUrls.add(normalized)
    }

    fun removeManifest(value: String) {
        manifestUrls.remove(value.trim().replace("|","%7C"))
    }

    suspend fun resolve(imdbId: String, type: String, season: Int = 0, episode: Int = 0, movieYear: String? = null, maxBytes: Long = 0, movieTitle: String? = null, preferredAudio: String = ""): List<StreamChoice> = coroutineScope {
        val manifests = manifestUrls.toList()
        if (manifests.isEmpty()) throw IllegalStateException("No stream add-on is configured")
        val rows = manifests.mapIndexed { index,manifest ->
            async {
                try {
                    val raw=resolveOne(manifest,index,imdbId,type,season,episode)
                    val accepted=raw.filter {StreamFilters.allowed(it,type,movieYear,maxBytes,movieTitle)}
                    val unknownSize=if(maxBytes>0) raw.count {StreamOrdering.bytes(it)<=0 && !StreamFilters.allowsUnknownSize(it)} else 0
                    statuses[manifest]="Returned: ${raw.size} · Available: ${accepted.size}"+
                        (if(unknownSize>0) " · Hidden (unknown size): $unknownSize" else "")+
                        (if((torrentCounts[manifest] ?: 0)>0) " · P2P required: ${torrentCounts[manifest]}" else "")
                    accepted
                } catch(e:kotlinx.coroutines.CancellationException) {throw e} catch(e:Exception) {
                    val configured=statuses[manifest]=="Configuration required"
                    statuses[manifest]=if(configured) "Configuration required" else
                        if(e is AddonHttpFailureRP12) "HTTP ${e.status}" else if(e is HttpFailure) "HTTP ${e.status}" else "Request failed"
                    emptyList()
                }
            }
        }.awaitAll().flatten()

        StreamOrdering.sorted(rows.filter { StreamFilters.allowed(it,type,movieYear,maxBytes,movieTitle) },preferredAudio)
    }

    private suspend fun resolveOne(manifest: String, index:Int, imdbId: String, type: String, season: Int, episode: Int): List<StreamChoice> = coroutineScope {
        val base = manifest.removeSuffix("/manifest.json")
        val stremioType = if (type == "series") "series" else "movie"
        val mediaId = if (stremioType == "series") "$imdbId:$season:$episode" else imdbId
        torrentCounts[manifest]=0
        if(metadata[manifest]?.streams==false) return@coroutineScope emptyList()
        // Metadata must not add a second sequential wait before the stream request.
        val manifestInfo=async {info(manifest)}
        val streamResponse=async {JSONObject(fetch("$base/stream/$stremioType/$mediaId.json",8000))}
        val json=streamResponse.await()
        if(!manifestInfo.await().streams) return@coroutineScope emptyList()
        val addonName = label(manifest,index)
        val a = json.optJSONArray("streams") ?: return@coroutineScope emptyList()
        torrentCounts[manifest]=(0 until a.length()).count {i ->
            val row=a.optJSONObject(i)
            row!=null && row.optString("url").let {url ->
                !url.startsWith("http://") && !url.startsWith("https://") &&
                    (row.optString("infoHash").isNotBlank() || url.startsWith("magnet:"))
            }
        }
        (0 until a.length()).mapNotNull { i ->
            val s = a.optJSONObject(i) ?: return@mapNotNull null
            val url = s.optString("url").trim().takeIf { it.startsWith("http://") || it.startsWith("https://") } ?: return@mapNotNull null
            val hints = s.optJSONObject("behaviorHints")
            val filename = hints?.optString("filename").orEmpty().ifBlank {s.optString("filename")}
            val videoHash = hints?.optString("videoHash").orEmpty()
            val videoSize = hints?.optLong("videoSize", 0L) ?: 0L
            // Only explicit audio fields; subtitle language is not audio evidence.
            fun languageValues(value: Any?): List<String> = when(value) {
                is org.json.JSONArray -> (0 until value.length()).mapNotNull { value.optString(it).takeIf { x -> x.isNotBlank() } }
                is String -> value.split(Regex("[,;|/\\s]+" )).filter { it.isNotBlank() }
                else -> emptyList()
            }
            val audioLanguages = (languageValues(s.opt("audioLanguages")) + languageValues(s.opt("languages")) +
                languageValues(s.opt("audioLanguage")) + languageValues(hints?.opt("audioLanguages"))).distinct()
            val subs = s.optJSONArray("subtitles")?.let { sa ->
                (0 until sa.length()).mapNotNull { si ->
                    val sub = sa.optJSONObject(si) ?: return@mapNotNull null
                    val su = sub.optString("url").takeIf { it.startsWith("http") } ?: return@mapNotNull null
                    SubtitleChoice(su, normalizeLang(sub.optString("lang")), sub.optString("label"), source = addonName)
                }
            } ?: emptyList()
            val proxy=hints?.optJSONObject("proxyHeaders")?.optJSONObject("request")
            val headers=proxy?.keys()?.asSequence()?.associateWith {proxy.optString(it)} ?: emptyMap()
            StreamChoice(
                name = s.optString("name", addonName),
                title = listOf(s.optString("title").ifBlank {s.optString("name","Stream")},s.optString("description"))
                    .filter {it.isNotBlank()}.distinct().joinToString("\n"),
                url = url,
                subtitles = subs,
                filename = filename,
                videoHash = videoHash,
                videoSize = videoSize,
                addonName = addonName,
                requestHeaders = headers,
                sourceAddonHost = runCatching {URI(manifest).host.orEmpty().lowercase(java.util.Locale.ROOT)}.getOrDefault(""),
                audioLanguages = audioLanguages
            )
        }
    }

    private fun normalizeLang(raw: String): String {
        val x = raw.trim().lowercase()
        return when (x) {
            "spa", "es-es", "es_419", "spanish", "español" -> "es"
            "eng", "en-us", "en-gb", "english" -> "en"
            else -> x.ifBlank { raw }
        }
    }

}
