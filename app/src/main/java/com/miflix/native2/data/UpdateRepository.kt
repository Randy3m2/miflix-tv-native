package com.miflix.native2.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.miflix.native2.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class UpdateRepository {
    private val releaseApi="https://api.github.com/repos/Randy3m2/miflix-tv-native/releases/tags/native-latest"
    private val fallbackApk="https://github.com/Randy3m2/miflix-tv-native/releases/download/native-latest/MiFlix-TV-Native.apk"

    suspend fun check(currentVersion: String,currentCode: Int): UpdateInfo {
        val j=JSONObject(Http.text(releaseApi,headers=mapOf("User-Agent" to "BruniO-TV","Cache-Control" to "no-cache")))
        val assets=j.optJSONArray("assets")
        val manifest=(0 until (assets?.length() ?: 0)).mapNotNull { assets?.optJSONObject(it) }
            .firstOrNull { it.optString("name")=="update.json" }
        if(manifest!=null) {
            val url=manifest.getString("browser_download_url")
            require(UpdatePolicy.releaseUrl(url)) { "Invalid update metadata URL" }
            val meta=JSONObject(Http.text(url+"?check="+System.currentTimeMillis(),headers=mapOf("Cache-Control" to "no-cache")))
            val code=meta.getInt("versionCode"); val download=meta.getString("downloadUrl")
            val digest=meta.getString("sha256").lowercase(java.util.Locale.ROOT);val size=meta.getLong("sizeBytes")
            require(UpdatePolicy.releaseUrl(download) && Regex("[a-f0-9]{64}").matches(digest) && size in 1..UpdatePolicy.MAX_BYTES) { "Invalid update metadata" }
            return UpdateInfo(meta.getString("version"),download,code>currentCode,code,digest,size)
        }
        // Legacy releases can still be checked, but never treat an older RC as an update.
        val version=Regex("Current version:\\s*([^\\s]+)",RegexOption.IGNORE_CASE)
            .find(j.optString("body"))?.groupValues?.getOrNull(1) ?: currentVersion
        return UpdateInfo(version,fallbackApk,UpdatePolicy.newer(version,currentVersion))
    }

    private fun connection(initial: String): HttpURLConnection {
        var url=initial
        repeat(6) {
            require(UpdatePolicy.downloadHost(url)) { "Untrusted download destination" }
            val c=URL(url).openConnection() as HttpURLConnection
            c.instanceFollowRedirects=false;c.connectTimeout=15000;c.readTimeout=20000
            c.setRequestProperty("User-Agent","BruniO-TV");c.setRequestProperty("Accept-Encoding","identity")
            val code=c.responseCode
            if(code in listOf(301,302,303,307,308)) {
                val next=c.getHeaderField("Location");c.disconnect()
                require(!next.isNullOrBlank()) { "Missing download redirect" }
                url=URL(URL(url),next).toString()
            } else {
                if(code !in 200..299) { c.disconnect();error("Download HTTP $code") }
                return c
            }
        }
        error("Too many download redirects")
    }

    suspend fun download(context: Context,info: UpdateInfo,onProgress: suspend (Long,Long)->Unit): File = withContext(Dispatchers.IO) {
        require(info.isNewer && info.versionCode>0 && info.sha256.isNotBlank()) { "This release has no verified update metadata" }
        val dir=File(context.cacheDir,"updates").apply { mkdirs() }
        val partial=File(dir,"update.part");val complete=File(dir,"update.apk")
        partial.delete();complete.delete()
        var c: HttpURLConnection?=null
        try {
            val downloadConnection=connection(info.downloadUrl)
            c=downloadConnection
            val length=downloadConnection.getHeaderField("Content-Length")?.toLongOrNull()
            require(length==null || length==info.sizeBytes) { "Download size mismatch" }
            require(dir.usableSpace>info.sizeBytes+10L*1024*1024) { "Not enough storage for the update" }
            val hash=MessageDigest.getInstance("SHA-256");var received=0L;var lastProgress=0L
            val active=currentCoroutineContext()
            downloadConnection.inputStream.use { input -> partial.outputStream().use { output ->
                val buffer=ByteArray(64*1024)
                while(true) {
                    active.ensureActive();val count=input.read(buffer);if(count<0) break
                    received+=count
                    require(received<=info.sizeBytes && received<=UpdatePolicy.MAX_BYTES) { "Update file is too large" }
                    output.write(buffer,0,count);hash.update(buffer,0,count)
                    val now=System.currentTimeMillis()
                    if(now-lastProgress>=150) { onProgress(received,info.sizeBytes);lastProgress=now }
                }
            } }
            require(received==info.sizeBytes) { "Incomplete update download" }
            val actual=hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
            require(actual==info.sha256) { "Update checksum mismatch" }
            validate(context,partial,info.versionCode)
            require(partial.renameTo(complete)) { "Could not save the verified APK" }
            onProgress(received,info.sizeBytes)
            complete
        } catch(e: Exception) { partial.delete();complete.delete();throw e }
        finally { c?.disconnect() }
    }

    @Suppress("DEPRECATION")
    private fun signatures(info: PackageInfo): Set<String> {
        val values=if(Build.VERSION.SDK_INT>=28) info.signingInfo?.apkContentsSigners else info.signatures
        return values.orEmpty().map { signature -> MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 255) } }.toSet()
    }
    @Suppress("DEPRECATION")
    private fun code(info: PackageInfo): Long = if(Build.VERSION.SDK_INT>=28) info.longVersionCode else info.versionCode.toLong()

    @Suppress("DEPRECATION")
    fun validate(context: Context,file: File,expectedCode: Int) {
        val pm=context.packageManager
        val flags=if(Build.VERSION.SDK_INT>=28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val apk=pm.getPackageArchiveInfo(file.absolutePath,flags) ?: error("Invalid APK")
        val installed=pm.getPackageInfo(context.packageName,flags)
        require(apk.packageName==context.packageName) { "This APK belongs to another app" }
        require(code(apk)==expectedCode.toLong() && code(apk)>code(installed)) { "This APK is not a newer build" }
        val current=signatures(installed);val incoming=signatures(apk)
        require(current.isNotEmpty() && current==incoming) { "Update signature differs from this installation. Use the stable signed build." }
    }

    /** Returns false when the user must grant install permission and press Install again. */
    fun install(context: Context,file: File,expectedCode: Int): Boolean {
        validate(context,file,expectedCode)
        if(Build.VERSION.SDK_INT>=26 && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return false
        }
        val uri=FileProvider.getUriForFile(context,context.packageName+".updates",file)
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }
}
