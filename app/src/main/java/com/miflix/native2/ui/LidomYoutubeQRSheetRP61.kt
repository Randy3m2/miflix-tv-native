package com.miflix.native2.ui

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.data.CustomChannelServerRP45
import com.miflix.native2.data.LidomRepository
import com.miflix.native2.data.LidomTeam
import com.miflix.native2.data.LidomSource
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.miflix.native2.data.LocalCastServer
import com.miflix.native2.model.PartyRole
import java.net.Inet4Address
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

@Composable
internal fun LidomYoutubeQRSheetRP61(state:AppState,repo:LidomRepository,team:LidomTeam,saved:()->Unit,close:()->Unit) {
    PlayerMenuDialog(onDismissRequest=close,fullScreen=true) {
        BoxWithConstraints(Modifier.fillMaxSize().consumeOpeningPress()) {
            Column(Modifier.align(Alignment.CenterEnd).fillMaxWidth(if(maxWidth<700.dp) .82f else .49f).fillMaxHeight(.94f)
                .background(Color(0xFC101010),RoundedCornerShape(18.dp)).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                LidomYoutubePairingRP61(state,repo,team,saved,Modifier.weight(1f))
                FocusButton(tr("Volver a fuentes","Back to sources")) {close()}
            }
        }
    }
}

@Composable
private fun LidomYoutubePairingRP61(state:AppState,repo:LidomRepository,team:LidomTeam,saved:()->Unit,modifier:Modifier) {
    val context=LocalContext.current.applicationContext
    val manager=remember {context.getSystemService(ConnectivityManager::class.java)}
    val main=remember {Handler(Looper.getMainLooper())}
    val owner=CustomChannelsRP45.owner(state)
    val currentSaved by rememberUpdatedState(saved)
    var receiver by remember {mutableStateOf<CustomChannelServerRP45?>(null)}
    var message by remember {mutableStateOf("")}
    var revision by remember {mutableIntStateOf(0)}
    var now by remember {mutableLongStateOf(System.currentTimeMillis())}
    LaunchedEffect(Unit) {while(true) {delay(1000);now=System.currentTimeMillis()}}
    DisposableEffect(owner,team.id,revision,state.appForeground,state.kidsMode,state.partyRole) {
        val alive=AtomicBoolean(true)
        var server:CustomChannelServerRP45?=null
        receiver=null;message=""
        if(state.appForeground && !state.kidsMode && state.partyRole!=PartyRole.GUEST) try {
            @Suppress("DEPRECATION")
            val endpoint=manager.allNetworks.sortedBy {it!=manager.activeNetwork}.mapNotNull {network ->
                val caps=manager.getNetworkCapabilities(network)
                if(caps==null || caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) || (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) null
                else manager.getLinkProperties(network)?.linkAddresses?.firstOrNull {it.address is Inet4Address && LocalCastServer.isPrivate(it.address.address)}
            }.firstOrNull()
            if(endpoint==null) message=tr("Conecta la TV a Wi-Fi o Ethernet.","Connect the TV to Wi-Fi or Ethernet.")
            else {
                server=CustomChannelServerRP45(endpoint.address as Inet4Address,endpoint.prefixLength,team.short+" · Live","") {name,url ->
                    val latch=CountDownLatch(1);val accepted=AtomicBoolean(false)
                    main.post {
                        try {
                            if(alive.get() && state.appForeground && CustomChannelsRP45.owner(state)==owner && !state.kidsMode && state.partyRole!=PartyRole.GUEST) {
                                accepted.set(runCatching {
                                    val path=java.net.URI(url).path.orEmpty().lowercase(java.util.Locale.ROOT)
                                    val kind=when {
                                        LidomRepository.youtubeId(url)!=null || LidomRepository.youtubeChannel(url) -> "YOUTUBE"
                                        path.endsWith(".m3u8") -> "HLS"
                                        path.endsWith(".mpd") -> "DASH"
                                        else -> error(tr("Usa un enlace de YouTube, HLS (.m3u8) o DASH (.mpd).","Use a YouTube, HLS (.m3u8) or DASH (.mpd) link."))
                                    }
                                    val id=repo.sources(team.id).firstOrNull {it.url==url}?.id ?: "qr-source:"+team.id+":"+java.util.UUID.randomUUID().toString()
                                    repo.save(LidomSource(id,team.id,name.ifBlank {team.short+" · Live"},kind,url))
                                    true
                                }.getOrElse {message=it.message.orEmpty();false})
                                if(accepted.get()) {currentSaved();message=tr("✓ Fuente guardada para este equipo.","✓ Source saved for this team.")}
                            }
                        } finally {latch.countDown()}
                    }
                    latch.await(3,TimeUnit.SECONDS) && accepted.get()
                }
                receiver=server
            }
        } catch(_:Exception) {message=tr("No se pudo abrir el QR. Vuelve a intentarlo.","Could not open the QR. Try again.")}
        onDispose {alive.set(false);server?.close()}
    }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(team.short+" · "+tr("Agregar fuente","Add source"),color=Color.White,fontSize=22.sp)
        Text(tr("Escanea el QR y pega YouTube, HLS (.m3u8) o DASH (.mpd). Agrega varias fuentes y reutilízalas en pantalla completa o SplitScreen.","Scan the QR and paste YouTube, HLS (.m3u8) or DASH (.mpd). Add several sources and reuse them in full screen or SplitScreen."),color=Muted,fontSize=13.sp)
        receiver?.let {server ->
            if(now<server.until) {QrCode(server.url,180.dp);Text(tr("QR válido por ","QR valid for ")+((server.until-now)/1000).coerceAtLeast(0)+"s",color=Muted,fontSize=12.sp)}
            else Text(tr("QR vencido. Genera uno nuevo.","QR expired. Generate a new one."),color=Muted)
        }
        if(message.isNotBlank()) Text(message,color=Color.White,fontSize=13.sp)
        FocusButton(tr("Nuevo QR","New QR")) {revision++}
        Text(tr("Guardado en este perfil de esta TV. Los enlaces temporales pueden caducar.","Saved to this profile on this TV. Temporary links can expire."),color=Muted,fontSize=11.sp)
    }
}
