package com.miflix.native2.ui

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import com.miflix.native2.data.LocalCastServer
import java.net.Inet4Address
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/** Receiver exists only after explicitly opening pairing, while BruniO is foregrounded. */
@Composable
internal fun LocalCastHost(state: AppState) {
    val context=LocalContext.current.applicationContext
    val manager=remember {context.getSystemService(ConnectivityManager::class.java)}
    val main=remember {Handler(Looper.getMainLooper())}
    var enabled by remember {mutableStateOf(false)}
    var revision by remember {mutableIntStateOf(0)}
    var receiver by remember {mutableStateOf<LocalCastServer?>(null)}
    var message by remember {mutableStateOf("")}
    var command by remember {mutableStateOf<Job?>(null)}
    var networkRevision by remember {mutableIntStateOf(0)}
    LaunchedEffect(state.localCastPairing) {if(state.localCastPairing) enabled=true}
    DisposableEffect(enabled,state.appForeground) {
        val callback=object: ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {main.post {networkRevision++}}
            override fun onLost(network: Network) {main.post {networkRevision++}}
            override fun onLinkPropertiesChanged(network: Network,properties: android.net.LinkProperties) {main.post {networkRevision++}}
        }
        if(enabled && state.appForeground) runCatching {manager.registerNetworkCallback(NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),callback)}
        onDispose {runCatching {manager.unregisterNetworkCallback(callback)}}
    }
    DisposableEffect(enabled,state.appForeground,state.activeProfile.id,state.session?.userId,revision,networkRevision,state.kidsMode) {
        var server: LocalCastServer?=null
        val owner=state.activeProfile.id
        val account=state.session?.userId
        if(enabled && state.appForeground && !state.kidsMode) {
            try {
                val endpoint=manager.allNetworks.mapNotNull {network ->
                    val caps=manager.getNetworkCapabilities(network)
                    if(caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)!=true || caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) null
                    else manager.getLinkProperties(network)?.linkAddresses?.firstOrNull {it.address is Inet4Address && LocalCastServer.isPrivate(it.address.address)}
                }.firstOrNull()
                if(endpoint==null) message=tr("Conecta la TV al Wi-Fi de tu casa.","Connect the TV to your home Wi-Fi.")
                else {
                    val page=context.assets.open("brunio-local-cast.html").use {it.readBytes()}
                    server=LocalCastServer(endpoint.address as Inet4Address,endpoint.prefixLength,page) {cloud,action,season,episode ->
                        main.post {
                            if(state.appForeground && state.activeProfile.id==owner && state.session?.userId==account && !state.kidsMode && state.watchParty==null && command?.isActive!=true) {
                                command=state.launch {state.socialAction {
                                    val title=state.tmdb.byCloudId(cloud) ?: error(tr("Título no disponible","Title unavailable"))
                                    state.localCastPairing=false;state.contextTitle=null;state.sourceSelection=null;state.miniPlayer=false
                                    state.open(title)
                                    if(action=="play" && !state.selectedUpcoming) {
                                        if(title.type=="movie") state.playMovie() else {
                                            val ep=state.tmdb.episodeInfo(title,season,episode) ?: error(tr("Episodio no disponible","Episode unavailable"))
                                            state.playEpisode(ep)
                                        }
                                    }
                                } }
                            }
                        }
                    }
                    message=""
                }
            } catch(e: Exception) {message=e.message ?: "Unable to start local receiver"}
        }
        receiver=server
        onDispose {server?.close();receiver=null;command?.cancel()}
    }
    if(state.localCastPairing) {
        var now by remember {mutableLongStateOf(System.currentTimeMillis())}
        LaunchedEffect(Unit) {while(true) {delay(1000);now=System.currentTimeMillis()} }
        Dialog(onDismissRequest={state.localCastPairing=false}) {
            val first=remember {FocusRequester()}
            LaunchedEffect(Unit) {withFrameNanos {};runCatching {first.requestFocus()} }
            Column(Modifier.widthIn(max=620.dp).heightIn(max=510.dp).background(Panel,RoundedCornerShape(24.dp)).padding(24.dp).verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(tr("Conectar teléfono / PC","Connect phone / PC"),color=Color.White,fontSize=24.sp)
                Text(tr("Solo en el mismo Wi-Fi. BruniO debe permanecer abierto. No disponible durante un Party ni en Kids.","Same Wi-Fi only. Keep BruniO open. Unavailable during a Party or in Kids."),color=Muted,fontSize=14.sp)
                receiver?.let {server ->
                    val expired=now>server.pairUntil
                    if(!expired) {
                        QrCode(server.url+"/#code="+server.code,170.dp)
                        Text(server.url,color=Color.White,fontSize=17.sp)
                        Text(tr("Código: ","Code: ")+server.code+" · "+((server.pairUntil-now)/1000).coerceAtLeast(0)+"s",color=Accent,fontSize=24.sp)
                    } else Text(tr("Código caducado. Genera uno nuevo.","Code expired. Generate a new one."),color=Muted)
                    Text(tr("Escanea el QR o introduce la dirección y el código en Send to TV de la web. La conexión dura hasta 30 minutos.","Scan the QR or enter the address and code in the website's Send to TV. Connection lasts up to 30 minutes."),color=Muted,fontSize=13.sp)
                }
                if(message.isNotBlank()) Text(message,color=Color.White)
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    FocusButton(tr("Nuevo código","New code"),modifier=Modifier.focusRequester(first)) {revision++}
                    FocusButton(tr("Desconectar","Disconnect")) {enabled=false;state.localCastPairing=false}
                    FocusButton(tr("Cerrar","Close")) {state.localCastPairing=false}
                }
            }
        }
    }
}
