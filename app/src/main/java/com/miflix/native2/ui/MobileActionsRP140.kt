package com.miflix.native2.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.StreamChoice
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

internal fun mobileAddonRP140(stream:StreamChoice)=stream.addonName.trim().ifBlank {stream.name.substringBefore("\n").trim().ifBlank {"Other"}}

private val shareClientRP140 by lazy {OkHttpClient.Builder().callTimeout(8,TimeUnit.SECONDS).build()}
private var shareJobRP140:Job?=null
private var shareDialogRP140:android.app.AlertDialog?=null
internal fun shareMobileRP140(context:Context,item:MediaSummary,knownImdb:String?=null) {
    val activity=context.playbackActivity() ?: return
    if(shareDialogRP140?.isShowing==true || shareJobRP140?.isActive==true) return
    shareDialogRP140=android.app.AlertDialog.Builder(activity,android.R.style.Theme_DeviceDefault_Dialog_Alert)
        .setTitle(tr("Compartir contenido","Share content"))
        .setItems(arrayOf("BruniO",tr("Enlace de IMDb","IMDb link"))) {_,choice->
            if(choice==0) sendMobileShareRP140(context,item,com.miflix.native2.data.MobileShareRP140.link(item))
            else {
                Toast.makeText(context,tr("Buscando enlace de IMDb…","Finding IMDb link…"),Toast.LENGTH_SHORT).show()
                shareJobRP140=activity.lifecycleScope.launch {
                    val imdb=knownImdb ?: try {
                        com.miflix.native2.data.TmdbRepository(com.miflix.native2.BuildConfig.MIFLIX_TMDB_TOKEN).apply {language=if(UiLanguage.code=="es") "es-MX" else "en-US"}.details(item).imdbId
                    } catch(e:CancellationException) {throw e} catch(_:Exception) {null}
                    val url=com.miflix.native2.data.MobileShareRP140.imdbLink(imdb)
                    shareJobRP140=null
                    if(url!=null) sendMobileShareRP140(context,item,url)
                    else Toast.makeText(context,tr("No encontramos un enlace de IMDb para este título.","No IMDb link was found for this title."),Toast.LENGTH_LONG).show()
                }
            }
        }.create()
    val dialog=shareDialogRP140 ?: return
    val observer=object:androidx.lifecycle.DefaultLifecycleObserver {
        override fun onDestroy(owner:androidx.lifecycle.LifecycleOwner) {dialog.dismiss()}
    }
    activity.lifecycle.addObserver(observer)
    dialog.setOnDismissListener {activity.lifecycle.removeObserver(observer);if(shareDialogRP140===dialog) shareDialogRP140=null}
    dialog.show()
}

private fun sendMobileShareRP140(context:Context,item:MediaSummary,url:String) {
    if(shareJobRP140?.isActive==true) return
    val activity=context.playbackActivity() ?: return
    Toast.makeText(context,tr("Preparando contenido…","Preparing content…"),Toast.LENGTH_SHORT).show()
    shareJobRP140=activity.lifecycleScope.launch {
        val text=com.miflix.native2.data.MobileShareRP140.text(item,url)
        val cover=withContext(Dispatchers.IO) {runCatching {
            val url=item.poster ?: return@runCatching null
            if(!url.startsWith("https://")) return@runCatching null
            shareClientRP140.newCall(Request.Builder().url(url).build()).execute().use {response->
                if(!response.isSuccessful) return@use null
                val body=response.body ?: return@use null
                val mime=body.contentType()?.toString().orEmpty().substringBefore(';')
                if(mime !in listOf("image/jpeg","image/png","image/webp")) return@use null
                val bytes=body.byteStream().use {input->
                    val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                    while(output.size()<=4*1024*1024) {val n=input.read(buffer);if(n<0) break;output.write(buffer,0,n)}
                    output.toByteArray()
                }
                if(bytes.size>4*1024*1024 || bytes.isEmpty()) return@use null
                val dir=File(context.cacheDir,"shares").apply {mkdirs()}
                dir.listFiles()?.filter {System.currentTimeMillis()-it.lastModified()>7*86400000L}?.forEach {it.delete()}
                val ext=when(mime) {"image/png"->"png";"image/webp"->"webp";else->"jpg"}
                val file=File.createTempFile("cover-",".$ext",dir).apply {writeBytes(bytes)}
                FileProvider.getUriForFile(context,context.packageName+".updates",file) to mime
            }
        }.getOrNull()}
        val send=Intent(Intent.ACTION_SEND).setType(cover?.second ?: "text/plain").putExtra(Intent.EXTRA_TEXT,text).putExtra(Intent.EXTRA_SUBJECT,item.title)
        if(cover!=null) {send.putExtra(Intent.EXTRA_STREAM,cover.first);send.clipData=android.content.ClipData.newRawUri("Cover",cover.first);send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)}
        runCatching {activity.startActivity(Intent.createChooser(send,tr("Compartir contenido","Share content")))}
            .onFailure {Toast.makeText(context,tr("No se pudo compartir","Unable to share"),Toast.LENGTH_SHORT).show()}
    }
}

internal fun openMobileCastRP140(context:Context) {
    runCatching {context.startActivity(Intent(android.provider.Settings.ACTION_CAST_SETTINGS))}
        .onFailure {Toast.makeText(context,tr("Este dispositivo no ofrece transmisión de pantalla","Screen casting is unavailable on this device"),Toast.LENGTH_LONG).show()}
}
