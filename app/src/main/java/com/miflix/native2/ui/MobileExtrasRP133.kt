package com.miflix.native2.ui

import android.app.DownloadManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.tv.material3.Text
import com.miflix.native2.model.*

/** Only complete files. Saving an HLS manifest is not an offline video download. */
internal object MobileDownloadRP133 {
    fun direct(url:String):Boolean = runCatching {
        val u=java.net.URI(url)
        u.scheme in listOf("https","http") && !u.host.isNullOrBlank() && u.userInfo==null &&
            u.path.lowercase(java.util.Locale.ROOT).let {p->listOf(".mp4",".mkv",".webm",".ts",".mov").any(p::endsWith)}
    }.getOrDefault(false)
    fun direct(stream:StreamChoice):Boolean = direct(stream.url) || runCatching {
        val uri=java.net.URI(stream.url)
        uri.scheme in listOf("https","http") && !uri.host.isNullOrBlank() && uri.userInfo==null &&
            !uri.path.lowercase().let {it.endsWith(".m3u8") || it.endsWith(".mpd")} &&
            stream.filename.substringAfterLast('.').lowercase() in listOf("mp4","mkv","webm","ts","mov")
    }.getOrDefault(false)
    fun enqueue(context:Context,title:String,stream:StreamChoice):Long {
        require(direct(stream)) {"This source is not a downloadable video file"}
        val ext=stream.filename.substringAfterLast('.').lowercase().takeIf {it in listOf("mp4","mkv","webm","ts","mov")} ?: java.net.URI(stream.url).path.substringAfterLast('.').lowercase(java.util.Locale.ROOT)
        val name=title.replace(Regex("[^\\p{L}\\p{N} ._-]"),"_").take(90).ifBlank {"BruniO"}+"-"+System.currentTimeMillis()+"."+ext
        val request=DownloadManager.Request(Uri.parse(stream.url)).setTitle(title.take(100))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        stream.requestHeaders.forEach {(key,value)->if(!key.contains('\n') && !value.contains('\n') && !value.contains('\r')) request.addRequestHeader(key,value)}
        request.setDestinationInExternalFilesDir(context,Environment.DIRECTORY_DOWNLOADS,name)
        return (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
    }
}

@Composable
internal fun MobilePlayerEnvironmentRP133(enabled:Boolean) {
    val activity=androidx.compose.ui.platform.LocalContext.current.playbackActivity()
    DisposableEffect(activity,enabled) {
        val previous=activity?.requestedOrientation
        val controller=activity?.window?.let {androidx.core.view.WindowCompat.getInsetsController(it,it.decorView)}
        val insets=activity?.window?.decorView?.let {androidx.core.view.ViewCompat.getRootWindowInsets(it)}
        val statusWasVisible=insets?.isVisible(androidx.core.view.WindowInsetsCompat.Type.statusBars()) ?: true
        val navigationWasVisible=insets?.isVisible(androidx.core.view.WindowInsetsCompat.Type.navigationBars()) ?: true
        val previousBehavior=controller?.systemBarsBehavior
        if(enabled) {
            activity?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller?.systemBarsBehavior=androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        onDispose {if(enabled) {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if(previous!=null) activity?.requestedOrientation=previous
            if(statusWasVisible) controller?.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            if(navigationWasVisible) controller?.show(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            if(previousBehavior!=null) controller?.systemBarsBehavior=previousBehavior
        }}
    }
}

@Composable
internal fun MobileTitleActionsRP133(state:AppState,item:MediaSummary,continuing:Boolean,close:()->Unit) {
    MobileSheetRP133(item.title,close) {
        MobileButtonRP133(tr("Ver detalles","View details"),"more",Modifier.fillMaxWidth()) {close();state.launch {state.open(item)}}
        MobileButtonRP133(tr("Reproducir manualmente","Play manually"),"link",Modifier.fillMaxWidth()) {close();state.launch {state.open(item);if(!state.selectedUpcoming) state.chooseCurrentSources()}}
        MobileButtonRP133(tr("Desde el principio","Start from beginning"),"play",Modifier.fillMaxWidth()) {close();state.launch {state.open(item);if(!state.selectedUpcoming) {if(item.type=="movie") state.playMovie(true) else state.playSeriesFromStart()}}}
        MobileButtonRP133(if(item.cloudId in state.favorites) tr("Quitar de mi lista","Remove from My list") else tr("Mi lista","My list"),"list",Modifier.fillMaxWidth()) {state.toggleFavorite(item);close()}
        MobileButtonRP133(if(continuing) tr("Quitar de Seguir viendo","Remove from Continue Watching") else tr("Marcar como visto","Mark watched"),"check",Modifier.fillMaxWidth()) {if(continuing) state.removeContinue(item) else state.markWatched(item);close()}
    }
}

@Composable
internal fun MobileEpisodesRP133(state:AppState,request:PlayerRequest,close:()->Unit) {
    MobileSheetRP133(tr("Episodios","Episodes"),close) {
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            if(com.miflix.native2.data.AbsoluteEpisodeRP102.enabled(request.item.id)) item {Text(tr("Serie completa","Complete series"),color=Color.White)} else items((1..(state.details?.seasonCount ?: 1).coerceAtLeast(1)).toList()) {season->MobileButtonRP133("S$season",selected=season==state.currentSeason) {state.launch {state.loadSeason(season)}}}
        }
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            items(state.episodes,key={"${it.season}:${it.episode}"}) {ep->
                MobileButtonRP133("${if(state.episodeWatched(request.item,ep)) "✓ " else ""}E${ep.absoluteEpisodeRP102.takeIf {it>0} ?: ep.episode} · ${ep.title}",modifier=Modifier.fillMaxWidth(),selected=ep.season==request.season && ep.episode==request.episode) {
                    close();state.launch {state.switchPlayerEpisode(ep)}
                }
            }
        }
    }
}

@Composable
internal fun MobileUpdateRP133(state:AppState,close:()->Unit) {
    val context=androidx.compose.ui.platform.LocalContext.current
    if(com.miflix.native2.BuildConfig.DEBUG) {
        MobileSheetRP133(tr("Versión de prueba","Preview version"),close) {
            Text(tr("Esta instalación Preview se mantiene separada de la versión firmada. La versión estable se genera con el workflow Build BruniO Mobile APK.","This Preview installation is separate from the signed app. Build the stable version with the Build BruniO Mobile APK workflow."),color=Color.White)
        }
        return
    }
    LaunchedEffect(Unit) {state.checkForUpdates()}
    MobileSheetRP133(tr("Actualizaciones","Updates"),close) {
        Text("BruniO Mobile · "+com.miflix.native2.BuildConfig.VERSION_NAME,color=Color.White)
        Text(when {
            state.updateChecking->tr("Buscando actualizaciones…","Checking for updates…")
            state.updateDownloading->tr("Descargando…","Downloading…")
            state.downloadedUpdate!=null->tr("Lista para instalar","Ready to install")
            state.updateInfo?.isNewer==true->tr("Disponible: ","Available: ")+state.updateInfo?.version
            state.error!=null->tr("No se pudo comprobar. Puedes reintentar.","Couldn't check. You can retry.")
            else->tr("BruniO está al día","BruniO is up to date")
        },color=Muted)
        if(state.updateDownloading) Text(if(state.updateTotal>0) "${state.updateBytes*100/state.updateTotal}%" else "${state.updateBytes/1024/1024} MB",color=Color.White)
        MobileButtonRP133(tr("Comprobar","Check"),"refresh",Modifier.fillMaxWidth(),enabled=!state.updateChecking && !state.updateDownloading) {state.launch {state.checkForUpdates()}}
        if(state.updateInfo?.isNewer==true && state.downloadedUpdate==null) MobileButtonRP133(tr("Descargar","Download"),"download",Modifier.fillMaxWidth(),enabled=!state.updateDownloading) {state.launch {state.downloadUpdate()}}
        if(state.downloadedUpdate!=null) MobileButtonRP133(tr("Instalar","Install"),"check",Modifier.fillMaxWidth()) {state.installUpdate(context)}
    }
}

@Composable
internal fun MobileYoutubeRP133(state:AppState,request:PlayerRequest,close:()->Unit) {
    val video=com.miflix.native2.data.LidomRepository.youtubeId(request.stream.url)
    androidx.activity.compose.BackHandler {close()}
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            MobileIconButtonRP133("back",tr("Volver","Back"),click=close)
            Text(request.item.title,color=Color.White,modifier=Modifier.weight(1f))
            MobileIconButtonRP133("grid","SplitScreen") {state.launch {state.leaveWatchParty();state.beginMultiView(request)}}
        }
        if(video!=null) LidomYoutubePlayer(video,{state.socialPlaybackStarted(request,0,0)},close,embedded=true,caption=request.item.title)
        else Text(tr("Selecciona un video de YouTube válido.","Select a valid YouTube video."),color=Color.White)
    }
}
