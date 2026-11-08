package com.miflix.native2.data

import com.miflix.native2.model.UpdateInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.json.JSONObject

/** No HTML, remote error bodies, URLs or credentials may reach the update dialog. */
internal class UpdateCheckUnavailableRP187:IllegalStateException("The update service is temporarily unavailable. Please retry shortly.")

internal class UpdateMetadataRP187(
    private val applicationId:String,
    private val fetch:suspend (String)->String,
    private val pause:suspend ()->Unit={delay(350)}
) {
    private val channel=if(applicationId.startsWith("com.brunio.mobile")) "mobile-latest" else "native-latest"
    private val direct="https://github.com/Randy3m2/miflix-tv-native/releases/download/$channel/update.json"
    private val api="https://api.github.com/repos/Randy3m2/miflix-tv-native/releases/tags/$channel"

    suspend fun check(currentCode:Int):UpdateInfo {
        // Release metadata avoids the API dependency and its unauthenticated quota.
        // On failure use release discovery, then one bounded metadata retry.
        try {return parse(fetch(direct),currentCode)} catch(e:CancellationException) {throw e} catch(_:Exception) { }
        try {
            val release=JSONObject(fetch(api));val assets=release.optJSONArray("assets")
            val asset=(0 until (assets?.length() ?: 0)).mapNotNull {assets?.optJSONObject(it)}.firstOrNull {it.optString("name")=="update.json"}
                ?: throw UpdateCheckUnavailableRP187()
            val url=asset.getString("browser_download_url")
            require(UpdatePolicy.releaseUrl(url))
            pause()
            return parse(fetch(url),currentCode)
        } catch(e:CancellationException) {throw e} catch(_:Exception) {throw UpdateCheckUnavailableRP187()}
    }

    private fun parse(body:String,currentCode:Int):UpdateInfo {
        val meta=JSONObject(body)
        val version=meta.getString("version");val code=meta.getInt("versionCode")
        val url=meta.getString("downloadUrl");val hash=meta.getString("sha256").lowercase(java.util.Locale.ROOT)
        val size=meta.getLong("sizeBytes")
        // Older TV metadata has no applicationId; its channel must still match.
        if(meta.has("applicationId")) require(meta.getString("applicationId")==applicationId)
        val tag=java.net.URI(url).path.substringAfter("/releases/download/").substringBefore('/')
        require(tag.startsWith(if(channel=="mobile-latest") "mobile-" else "native-") && java.net.URI(url).path.endsWith(".apk",ignoreCase=true))
        require(code>0 && version.length<=80 && Regex("\\d+\\.\\d+\\.\\d+(?:-[A-Za-z0-9.-]+)?").matches(version))
        require(UpdatePolicy.releaseUrl(url) && Regex("[a-f0-9]{64}").matches(hash) && size in 1..UpdatePolicy.MAX_BYTES)
        return UpdateInfo(version,url,code>currentCode,code,hash,size)
    }
}
