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
import com.miflix.native2.data.M3UListServerRP61
import com.miflix.native2.data.ChannelMetadataRP50
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
internal fun M3UListsPanelRP61(state:AppState,close:()->Unit) {
    val context=LocalContext.current.applicationContext
    val owner=CustomChannelsRP45.owner(state)
    val revision by CustomChannelsRP45.changesRP48.collectAsState()
    var adding by remember(owner) {mutableStateOf(true)}
    val lists=remember(owner,revision) {M3UListsRP61.lists(context,owner)}
    PlayerMenuDialog(onDismissRequest=close,fullScreen=true) {
        BoxWithConstraints(Modifier.fillMaxSize().consumeOpeningPress()) {
            Column(Modifier.align(Alignment.CenterEnd).fillMaxWidth(if(maxWidth<700.dp) .82f else .49f).fillMaxHeight(.94f)
                .background(Color(0xFC101010),RoundedCornerShape(18.dp)).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(tr("LIVE TV · TUS LISTAS","LIVE TV · YOUR PLAYLISTS"),color=Muted,fontSize=12.sp)
                if(adding) M3UPairingRP61(state,{},Modifier.weight(1f))
                else LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    items(lists,key={it.first}) {list ->
                        Text("${list.second} · ${list.third}",color=Color.White,fontSize=17.sp)
                        FocusButton(tr("Eliminar lista","Delete playlist"),destructive=true) {M3UListsRP61.remove(context,owner,list.first)}
                    }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    FocusButton(if(adding) tr("Mis listas","My playlists") else tr("+ Agregar lista","+ Add playlist")) {adding=!adding}
                    FocusButton(tr("Cerrar","Close")) {close()}
                }
            }
        }
    }
}

@Composable
private fun M3UPairingRP61(state:AppState,saved:()->Unit,modifier:Modifier) {
    val context=LocalContext.current.applicationContext
    val manager=remember {context.getSystemService(ConnectivityManager::class.java)}
    val main=remember {Handler(Looper.getMainLooper())}
    val owner=CustomChannelsRP45.owner(state)
    val currentSaved by rememberUpdatedState(saved)
    var receiver by remember {mutableStateOf<M3UListServerRP61?>(null)}
    var message by remember {mutableStateOf("")}
    var revision by remember {mutableIntStateOf(0)}
    var now by remember {mutableLongStateOf(System.currentTimeMillis())}
    LaunchedEffect(Unit) {while(true) {delay(1000);now=System.currentTimeMillis()}}
    DisposableEffect(owner,revision,state.appForeground,state.kidsMode,state.partyRole) {
        val alive=AtomicBoolean(true)
        var server:M3UListServerRP61?=null
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
                server=M3UListServerRP61(endpoint.address as Inet4Address,endpoint.prefixLength,"","") {name,url,playlist ->
                    val prepared=try {M3UListsRP61.prepare(name,url,playlist)} catch(e:Exception) {
                        main.post {if(alive.get()) message=e.message ?: "No se pudo importar / Import failed"}
                        null
                    }
                    if(prepared==null) false else {
                    val latch=CountDownLatch(1);val accepted=AtomicBoolean(false)
                    main.post {
                        try {
                            if(alive.get() && state.appForeground && CustomChannelsRP45.owner(state)==owner && !state.kidsMode && state.partyRole!=PartyRole.GUEST) {
                                accepted.set(alive.get())
                                
                            }
                        } finally {latch.countDown()}
                    }
                    val ok=latch.await(3,TimeUnit.SECONDS) && accepted.get() && alive.get() && M3UListsRP61.save(context,owner,prepared)
                    if(ok) main.post {if(alive.get()) {currentSaved();message=tr("✓ Lista guardada en Live TV.","✓ Playlist saved in Live TV.")}}
                    ok
                    }
                }
                receiver=server
            }
        } catch(_:Exception) {message=tr("No se pudo abrir el QR. Vuelve a intentarlo.","Could not open the QR. Try again.")}
        onDispose {alive.set(false);server?.close()}
    }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(tr("Agregar lista M3U","Add M3U playlist"),color=Color.White,fontSize=22.sp)
        Text(tr("Escanea el QR en la misma red que tu TV. Elige un archivo M3U o pega su URL y ponle un nombre.","Scan the QR on the same network as your TV. Choose an M3U file or URL and name your list."),color=Muted,fontSize=13.sp)
        receiver?.let {server ->
            if(now<server.until) {QrCode(server.url,180.dp);Text(tr("QR válido por ","QR valid for ")+((server.until-now)/1000).coerceAtLeast(0)+"s",color=Muted,fontSize=12.sp)}
            else Text(tr("QR vencido. Genera uno nuevo.","QR expired. Generate a new one."),color=Muted)
        }
        if(message.isNotBlank()) Text(message,color=Color.White,fontSize=13.sp)
        FocusButton(tr("Nuevo QR","New QR")) {revision++}
        Text(tr("Guardado en este perfil de esta TV. Los enlaces temporales pueden caducar.","Saved to this profile on this TV. Temporary links can expire."),color=Muted,fontSize=11.sp)
    }
}
