package com.miflix.native2.ui

import android.app.DownloadManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.miflix.native2.data.LocalStore
import com.miflix.native2.model.*
import kotlinx.coroutines.*
import org.json.JSONObject

internal data class MobileDownloadEntryRP134(val id:Long,val item:MediaSummary,val season:Int,val episode:Int,val status:Int,val bytes:Long,val total:Long,val uri:String?,val reason:Int)

internal object MobileDownloadsRP134 {
    val revision=kotlinx.coroutines.flow.MutableStateFlow(0L)
    private fun prefs(c:Context)=c.getSharedPreferences("brunio_mobile_downloads_rp134",Context.MODE_PRIVATE)
    fun owner(c:Context):String {val local=LocalStore(c);return "${local.loadSession()?.userId ?: "local"}:${local.profileId() ?: "primary"}"}
    @Synchronized fun enqueue(c:Context,item:MediaSummary,stream:StreamChoice,season:Int=0,episode:Int=0,scope:String=owner(c)):Long {
        val existing=entries(c,scope).firstOrNull {it.item.cloudId==item.cloudId && it.season==season && it.episode==episode && (it.status in listOf(DownloadManager.STATUS_PENDING,DownloadManager.STATUS_RUNNING,DownloadManager.STATUS_PAUSED) || it.uri!=null)}
        if(existing!=null) return existing.id
        entries(c,scope).filter {it.item.cloudId==item.cloudId && it.season==season && it.episode==episode && it.status==DownloadManager.STATUS_FAILED}.forEach {old->
            (c.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).remove(old.id)
            prefs(c).edit().remove("entry:${old.id}").apply()
        }
        val cover=java.io.File(c.filesDir,"download-covers-rp134/${item.type}-${item.id}.jpg")
        runCatching {
            val uri=java.net.URI(item.poster.orEmpty())
            if(!cover.isFile && uri.scheme=="https" && uri.host=="image.tmdb.org") {
                cover.parentFile?.mkdirs()
                val connection=uri.toURL().openConnection().apply {connectTimeout=5000;readTimeout=5000}
                val bytes=connection.getInputStream().use {input->
                    val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                    while(out.size()<=2*1024*1024) {val n=input.read(buffer);if(n<0) break;out.write(buffer,0,n)}
                    out.toByteArray()
                }
                if(bytes.isNotEmpty() && bytes.size<=2*1024*1024) cover.writeBytes(bytes)
            }
        }
        val id=MobileDownloadRP133.enqueue(c,item.title+(if(season>0) " S$season E$episode" else ""),stream)
        val data=JSONObject().put("owner",scope).put("media",item.id).put("type",item.type).put("title",item.title)
            .put("poster",item.poster.orEmpty()).put("localPoster",if(cover.isFile) cover.absolutePath else "").put("backdrop",item.backdrop.orEmpty()).put("year",item.year)
            .put("season",season).put("episode",episode)
        prefs(c).edit().putString("entry:$id",data.toString()).commit();revision.value++
        return id
    }
    fun planned(c:Context,scope:String,item:MediaSummary,season:Int,total:Int) {prefs(c).edit().putInt("season:$scope:${item.cloudId}:$season",total).apply();revision.value++}
    fun planned(c:Context,scope:String,item:MediaSummary,season:Int)=prefs(c).getInt("season:$scope:${item.cloudId}:$season",0)
    fun entries(c:Context,scope:String):List<MobileDownloadEntryRP134> {
        val manager=c.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return prefs(c).all.entries.filter {it.key.startsWith("entry:")}.mapNotNull {(key,value)->runCatching {
            val j=JSONObject(value as String);if(j.optString("owner")!=scope) return@runCatching null
            val id=key.substringAfter(':').toLong()
            manager.query(DownloadManager.Query().setFilterById(id))?.use {cursor->
                if(!cursor.moveToFirst()) return@use null
                fun number(name:String)=cursor.getLong(cursor.getColumnIndexOrThrow(name))
                val status=number(DownloadManager.COLUMN_STATUS).toInt()
                val uri=if(status==DownloadManager.STATUS_SUCCESSFUL) manager.getUriForDownloadedFile(id)?.toString() else null
                MobileDownloadEntryRP134(id,MediaSummary(j.getInt("media"),j.getString("type"),j.getString("title"),"",j.optString("backdrop").ifBlank {null},(j.optString("localPoster").takeIf {it.isNotBlank() && java.io.File(it).isFile} ?: j.optString("poster")).ifBlank {null},0.0,j.optString("year")),j.optInt("season"),j.optInt("episode"),status,number(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),number(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),uri,number(DownloadManager.COLUMN_REASON).toInt())
            }
        }.getOrNull()}.sortedWith(compareBy<MobileDownloadEntryRP134> {it.item.title}.thenBy {it.season}.thenBy {it.episode})
    }
    fun remove(c:Context,id:Long,scope:String) {
        if(entries(c,scope).none {it.id==id}) return
        (c.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).remove(id)
        prefs(c).edit().remove("entry:$id").apply();revision.value++
    }
}

@Composable
internal fun MobileDownloadsScreenRP134(state:AppState) {
    val context=LocalContext.current.applicationContext
    val scope=MobileDownloadsRP134.owner(context)
    val revision by MobileDownloadsRP134.revision.collectAsState()
    var rows by remember(scope) {mutableStateOf<List<MobileDownloadEntryRP134>>(emptyList())}
    var remove by remember {mutableStateOf<MobileDownloadEntryRP134?>(null)}
    LaunchedEffect(scope,revision) {
        do {
            rows=withContext(Dispatchers.IO) {MobileDownloadsRP134.entries(context,scope)}
            val pending=rows.any {it.status in listOf(DownloadManager.STATUS_PENDING,DownloadManager.STATUS_RUNNING,DownloadManager.STATUS_PAUSED)}
            if(pending) delay(1500)
        } while(pending && isActive)
    }
    remove?.let {entry->MobileSheetRP133(tr("Eliminar descarga","Remove download"),{remove=null}) {
        Text(entry.item.title,color=Color.White)
        Text(tr("Se eliminará el archivo guardado en este celular.","The file saved on this phone will be removed."),color=Muted)
        MobileButtonRP133(tr("Eliminar archivo","Delete file"),"close",Modifier.fillMaxWidth()) {MobileDownloadsRP134.remove(context,entry.id,scope);remove=null}
    }}
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {Text(tr("Descargas","Downloads"),color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Bold)}
        if(state.mobileDownloadStatusRP134.isNotBlank()) item {Text(state.mobileDownloadStatusRP134,color=Muted)}
        if(rows.isEmpty()) item {Text(tr("Tus películas y temporadas descargadas aparecerán aquí.","Your downloaded movies and seasons will appear here."),color=Muted)}
        rows.groupBy {it.item.cloudId+":"+it.season}.forEach {(group,entries)->
            val first=entries.first();val ready=entries.count {it.uri!=null}
            val expected=MobileDownloadsRP134.planned(context,scope,first.item,first.season).coerceAtLeast(entries.size)
            item(key=group) {
                Column(Modifier.fillMaxWidth().mobileGlassRP133().padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        AsyncImage(first.item.poster,first.item.title,Modifier.width(60.dp).height(88.dp),contentScale=ContentScale.Crop)
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text(first.item.title,color=Color.White,fontWeight=FontWeight.Bold)
                            Text(if(first.season>0) tr("Temporada ","Season ")+first.season+" · $ready / $expected "+tr("listos","ready") else if(ready>0) tr("Lista para ver sin conexión","Ready to watch offline") else tr("Descargando…","Downloading…"),color=Muted,fontSize=12.sp)
                        }
                    }
                    entries.forEach {entry->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        val status=when(entry.status) {
                            DownloadManager.STATUS_SUCCESSFUL->if(entry.uri!=null) tr("Lista","Ready") else tr("Archivo no disponible","File unavailable")
                            DownloadManager.STATUS_FAILED->tr("Falló · código ","Failed · code ")+entry.reason
                            DownloadManager.STATUS_PAUSED->tr("En espera de red / sistema","Waiting for network / system")
                            else->if(entry.total>0) "${entry.bytes*100/entry.total}%" else "${entry.bytes/1024/1024} MB"
                        }
                        Text((if(entry.season>0) "E${entry.episode} · " else "")+status,color=Color.White,fontSize=13.sp,modifier=Modifier.weight(1f))
                        if(entry.status==DownloadManager.STATUS_FAILED) MobileIconButtonRP133("refresh",tr("Reintentar descarga","Retry download")) {state.launch {state.downloadMobileItemRP134(entry.item,entry.season,entry.episode)}}
                        if(entry.uri!=null) MobileIconButtonRP133("play",tr("Reproducir sin conexión","Play offline")) {state.openMobileDownloadRP134(entry)}
                        MobileIconButtonRP133("close",tr("Eliminar descarga","Remove download")) {remove=entry}
                    }}
                }
            }
        }
    }
}
