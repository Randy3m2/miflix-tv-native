package com.miflix.native2.data

import android.content.ClipData
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
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class UpdateRepository {
    @Suppress("UNUSED_PARAMETER")
    suspend fun check(currentVersion:String,currentCode:Int):UpdateInfo =
        UpdateMetadataRP187(com.miflix.native2.BuildConfig.APPLICATION_ID,::metadataText).check(currentCode)

    private suspend fun metadataText(url:String):String = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()
        val fresh=url+(if(url.contains('?')) "&" else "?")+"check="+System.currentTimeMillis()
        val c=connection(fresh,metadata=true)
        try {
            c.inputStream.use {input ->
                val output=java.io.ByteArrayOutputStream()
                val bytes=ByteArray(8192)
                while(true) {
                    currentCoroutineContext().ensureActive()
                    val count=input.read(bytes);if(count<0) break
                    require(output.size()+count<=256*1024) {"Update metadata is too large"}
                    output.write(bytes,0,count)
                }
                output.toString("UTF-8")
            }
        } finally {c.disconnect()}
    }

    private fun connection(initial:String,metadata:Boolean=false):HttpURLConnection {
        var url=initial
        repeat(6) {
            require(UpdatePolicy.downloadHost(url) || (metadata && UpdatePolicy.metadataApiUrl(url))) { "Untrusted download destination" }
            val c=URL(url).openConnection() as HttpURLConnection
            c.instanceFollowRedirects=false;c.connectTimeout=15000;c.readTimeout=20000
            c.setRequestProperty("User-Agent","BruniO-TV");c.setRequestProperty("Accept-Encoding","identity")
            if(metadata) {c.setRequestProperty("Accept","application/json");c.setRequestProperty("Cache-Control","no-cache")}
            val code=try {c.responseCode} catch(e:Exception) {c.disconnect();throw e}
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
        val unique="update-${info.versionCode}-${java.util.UUID.randomUUID()}"
        val partial=File(dir,"$unique.part.apk");val complete=File(dir,"$unique.apk")
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
        require(apk.packageName==context.packageName) { "Package mismatch: installed=${context.packageName}; APK=${apk.packageName}. Keep the installed app and use its matching package." }
        require(code(apk)==expectedCode.toLong() && code(apk)>code(installed)) { "Version mismatch: installed=${code(installed)}; APK=${code(apk)}; expected=$expectedCode. Download a newer release." }
        val current=signatures(installed);val incoming=signatures(apk)
        require(current.isNotEmpty() && current==incoming) { "Signing certificate mismatch. Updating without uninstalling requires the original signing key. Installed SHA-256: ${current.joinToString()}. APK SHA-256: ${incoming.joinToString()}." }
    }

    @Suppress("DEPRECATION")
    fun installationDetails(context: Context): String {
        val flags=if(Build.VERSION.SDK_INT>=28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val installed=context.packageManager.getPackageInfo(context.packageName,flags)
        return "Package: ${context.packageName}\nInstalled: ${installed.versionName} (${code(installed)})\nAndroid API: ${Build.VERSION.SDK_INT}\nSigning SHA-256:\n${signatures(installed).joinToString("\n")}"
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
        // Retain the installer fallback for older TV package installers.
        @Suppress("DEPRECATION")
        val intent=Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri,"application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.clipData=ClipData.newRawUri("BruniO update APK",uri)
        try { context.startActivity(intent) }
        catch(e: android.content.ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive")
                .apply { clipData=ClipData.newRawUri("BruniO update APK",uri) }
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        return true
    }
}
