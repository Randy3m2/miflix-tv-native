package com.miflix.native2.data

import com.miflix.native2.model.UpdateInfo
import org.json.JSONObject

class UpdateRepository {
    private val releaseApi = "https://api.github.com/repos/Randy3m2/miflix-tv-native/releases/tags/native-latest"
    private val fallbackApk = "https://github.com/Randy3m2/miflix-tv-native/releases/download/native-latest/MiFlix-TV-Native.apk"

    suspend fun check(currentVersion: String): UpdateInfo {
        val j = JSONObject(Http.text(releaseApi, headers = mapOf("User-Agent" to "MiFlix-TV-Native")))
        val body = j.optString("body")
        val version = Regex("Current version:\\s*([^\\s]+)", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.getOrNull(1)
            ?: j.optString("name").substringAfterLast(" ").ifBlank { currentVersion }
        val assets = j.optJSONArray("assets")
        var download = fallbackApk
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val a = assets.optJSONObject(i) ?: continue
                if (a.optString("name") == "MiFlix-TV-Native.apk") {
                    download = a.optString("browser_download_url", fallbackApk)
                    break
                }
            }
        }
        return UpdateInfo(version, download, version != currentVersion)
    }
}
