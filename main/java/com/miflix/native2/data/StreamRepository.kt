package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import com.miflix.native2.model.SubtitleChoice
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.net.URI

class StreamRepository(initialManifestUrl: String) {
    private data class ManifestInfo(val name:String,val streams:Boolean,val at:Long)
    private val metadata=java.util.concurrent.ConcurrentHashMap<String,ManifestInfo>()
    private fun label(manifest:String,position:Int):String {
        val host=runCatching {URI(manifest).host}.getOrNull().orEmpty().ifBlank {"Add-on"}
        return (metadata[manifest]?.name?.takeIf {it.isNotBlank()} ?: host)+" · "+(position+1)
    }
    fun sourceAddonNames():List<String> = manifests().mapIndexedNotNull {index,url ->if(metadata[url]?.streams==false) null else label(url,index)}
    private suspend fun info(manifest:String):ManifestInfo {
        val now=System.currentTimeMillis()
        metadata[manifest]?.takeIf {now-it.at<6*60*60_000L}?.let {return it}
        return try {
            val json=JSONObject(Http.text(manifest,timeoutMs=8000))
            val resources=json.optJSONArray("resources")
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
                manifestUrls.remove(value.trim())
                val copy = manifestUrls.toList()
                manifestUrls.clear()
                manifestUrls.add(value.trim())
                manifestUrls.addAll(copy)
            }
        }

    fun manifests(): List<String> = manifestUrls.toList()

    fun setManifests(values: Collection<String>) {
        manifestUrls.clear()
        values.map { it.trim() }.filter { it.isNotBlank() }.forEach { addManifest(it) }
    }

    fun addManifest(value: String): Boolean {
        val clean = value.trim()
        if (clean.isBlank()) return false
        val normalized = if (clean.endsWith("/manifest.json")) clean else clean.trimEnd('/') + "/manifest.json"
        return manifestUrls.add(normalized)
    }

    fun removeManifest(value: String) {
        manifestUrls.remove(value.trim())
    }

    suspend fun resolve(imdbId: String, type: String, season: Int = 0, episode: Int = 0, movieYear: String? = null, maxBytes: Long = 0, movieTitle: String? = null, preferredAudio: String = ""): List<StreamChoice> = coroutineScope {
        val manifests = manifestUrls.toList()
        if (manifests.isEmpty()) throw IllegalStateException("No stream add-on is configured")
        val rows = manifests.mapIndexed { index,manifest ->
            async { runCatching { resolveOne(manifest, index, imdbId, type, season, episode) }.onFailure { if(it is kotlinx.coroutines.CancellationException) throw it }.getOrDefault(emptyList()) }
        }.awaitAll().flatten()

        StreamOrdering.sorted(rows.filter { StreamFilters.allowed(it,type,movieYear,maxBytes,movieTitle) },preferredAudio)
    }

    private suspend fun resolveOne(manifest: String, index:Int, imdbId: String, type: String, season: Int, episode: Int): List<StreamChoice> {
        val base = manifest.removeSuffix("/manifest.json")
        val stremioType = if (type == "series") "series" else "movie"
        val mediaId = if (stremioType == "series") "$imdbId:$season:$episode" else imdbId
        if(!info(manifest).streams) return emptyList()
        val addonName = label(manifest,index)
        val json = JSONObject(Http.text("$base/stream/$stremioType/$mediaId.json", timeoutMs = 18000))
        val a = json.optJSONArray("streams") ?: return emptyList()
        return (0 until a.length()).mapNotNull { i ->
            val s = a.optJSONObject(i) ?: return@mapNotNull null
            val url = s.optString("url").takeIf { it.startsWith("http") } ?: return@mapNotNull null
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
