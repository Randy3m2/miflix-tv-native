package com.miflix.native2.data

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/** One local catalog shared by the TV. Remote reads occur only on explicit refresh. */
internal object FinalCatalogRP97 {
    const val MANIFEST="brunio://final-rp97"
    private lateinit var app:Context
    private val refreshLock=Mutex()
    @Volatile private var cached:List<FinalEntryRP97>?=null
    @Volatile var revision=0L
        private set
    fun initialize(context:Context) {app=context.applicationContext}
    private fun prefs()=app.getSharedPreferences("brunio_final_catalog_rp97",Context.MODE_PRIVATE)
    private fun local()=AtomicFile(File(app.filesDir,"final_channels_rp97.m3u"))
    fun source()=prefs().getString("url","").orEmpty()
    fun setSource(url:String) {prefs().edit().putString("url",validateUrl(url)).apply()}
    fun updatedAt()=prefs().getLong("updated",0L)
    fun validateUrl(value:String):String {
        val u=URI(value.trim());require(u.scheme=="https" && !u.host.isNullOrBlank() && u.userInfo==null) {"Use an HTTPS Raw M3U URL"}
        return u.toASCIIString()
    }
    @Synchronized fun rows():List<FinalEntryRP97> {
        cached?.let {return it}
        val text=runCatching {local().openRead().bufferedReader().use {it.readText()}}.getOrNull()
        val parsed=text?.let {runCatching {FinalPlaylistRP97.parse(it)}.getOrNull()}
            ?: FinalPlaylistRP97.parse(app.assets.open("final_channels_rp97.m3u").bufferedReader().use {it.readText()})
        val artwork=runCatching {JSONObject(app.assets.open("final_artwork_rp97.json").bufferedReader().use {it.readText()})}.getOrDefault(JSONObject())
        val backups=FinalPlaylistRP97.parse(app.assets.open("lidom_backup_rp97.m3u").bufferedReader().use {it.readText()})
        return FinalPlaylistRP97.withLidomBackups(parsed,backups).map {row ->
            val previous=artwork.optString(FinalPlaylistRP97.nameKey(row.name)).takeIf {value ->
                value.isNotBlank() && (!value.startsWith("file:///android_asset/") || runCatching {app.assets.open(value.removePrefix("file:///android_asset/")).close();true}.getOrDefault(false))
            }
            val keepCustom=FinalPlaylistRP97.nameKey(row.name)=="dragonballz247"
            val logo=row.logo?.replace("https://www.themoviedb.org/t/p/","https://image.tmdb.org/t/p/")
            row.copy(logo=if(keepCustom && previous!=null) previous else logo ?: previous)
        }.also {cached=it}
    }
    fun categories():List<LiveCategory> {
        val groups=rows().map {it.category}.distinct()
        val ordered=groups.sortedBy {if(FinalPlaylistRP97.nameKey(it)=="republicadominicana") 0 else 1}
        return ordered.map {LiveCategory("tv",FinalPlaylistRP97.categoryId(it),it,genre=it,searchable=true)}+
            LiveCategory("tv","final-all","Todos",searchable=true)
    }
    fun channels(category:LiveCategory,skip:Int=0,search:String=""):List<LiveChannel> {
        val q=FinalPlaylistRP97.nameKey(search)
        return rows().asSequence().filter {(category.id=="final-all" || category.genre==it.category) &&
            (q.isBlank() || FinalPlaylistRP97.nameKey(it.name).contains(q) || it.number.toString()==search.trim())}
            .drop(skip.coerceAtLeast(0)).map {LiveChannel(it.id,"tv",it.name,it.logo,it.category,it.number,it.tvgId)}.toList()
    }
    fun streams(channel:LiveChannel):List<com.miflix.native2.model.StreamChoice> = rows().firstOrNull {it.id==channel.id}?.let {
        listOf(com.miflix.native2.model.StreamChoice(it.name,it.url,it.name,requestHeaders=it.headers))
    }.orEmpty()
    /** Validate first; AtomicFile retains the prior valid snapshot if a download/write fails. */
    suspend fun refresh(newUrl:String?=null):Int=refreshLock.withLock {withContext(Dispatchers.IO) {
        val url=validateUrl(newUrl ?: source());val connection=URL(FinalPlaylistRP97.refreshUrl(url,System.nanoTime())).openConnection() as HttpURLConnection
        val raw=try {
            connection.connectTimeout=12000;connection.readTimeout=15000;connection.useCaches=false
            connection.setRequestProperty("Cache-Control","no-cache");connection.setRequestProperty("Pragma","no-cache")
            require(connection.responseCode in 200..299) {"M3U HTTP ${connection.responseCode}"}
            connection.inputStream.use {input ->
                val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(true) {val count=input.read(buffer);if(count<0) break;out.write(buffer,0,count);require(out.size()<=4*1024*1024) {"M3U maximum: 4 MB"}}
                out.toString("UTF-8")
            }
        } finally {connection.disconnect()}
        val parsed=FinalPlaylistRP97.parse(raw)
        currentCoroutineContext().ensureActive()
        if(newUrl==null) check(source()==url) {"M3U source changed"}
        val atomic=local();val stream=atomic.startWrite()
        try {stream.write(raw.toByteArray(Charsets.UTF_8));atomic.finishWrite(stream)} catch(e:Exception) {atomic.failWrite(stream);throw e}
        prefs().edit().putString("url",url).putLong("updated",System.currentTimeMillis()).commit()
        synchronized(this@FinalCatalogRP97) {cached=null;revision++}
        rows().size
    }}
}
