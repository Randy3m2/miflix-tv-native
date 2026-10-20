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
import com.miflix.native2.data.LocalCastServer
import com.miflix.native2.model.PartyRole
import java.net.Inet4Address
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

@Composable
internal fun CustomChannelsPanelRP45(state:AppState,close:()->Unit) {
    val context=LocalContext.current.applicationContext
    val owner=CustomChannelsRP45.owner(state)
    var revision by remember(owner) {mutableIntStateOf(0)}
    val rows=remember(owner,revision) {CustomChannelsRP45.list(context,owner)}
    var adding by remember(owner) {mutableStateOf(false)}
    var editing by remember(owner) {mutableStateOf<CustomChannelRP45?>(null)}
    val first=remember {FocusRequester()}
    val dismiss={if(adding) {adding=false;editing=null} else close()}
    PlayerMenuDialog(onDismissRequest=dismiss,fullScreen=true) {
        BoxWithConstraints(Modifier.fillMaxSize().consumeOpeningPress()) {
            Column(Modifier.align(Alignment.CenterEnd).fillMaxWidth(if(maxWidth<700.dp) .82f else .49f).fillMaxHeight(.92f)
                .background(Color(0xFC101010),RoundedCornerShape(18.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(tr("OTHERS · MIS CANALES","OTHERS · MY CHANNELS"),color=Muted,fontSize=12.sp)
                if(adding) {
                    CustomChannelPairingRP45(state,editing,{revision++},Modifier.weight(1f))
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        FocusButton(tr("Volver","Back")) {adding=false;editing=null}
                        editing?.let {channel ->FocusButton(tr("Eliminar","Delete"),destructive=true) {
                            CustomChannelsRP45.remove(context,owner,channel.id);revision++;adding=false;editing=null
                        }}
                    }
                } else {
                    Text(tr("Tus canales, a tu manera","Your channels, your way"),color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold)
                    Text(tr("OK para ver · Mantén OK para editar o eliminar","OK to watch · Hold OK to edit or delete"),color=Muted,fontSize=12.sp)
                    FocusButton(tr("+ Agregar canal por QR","+ Add channel by QR"),modifier=Modifier.fillMaxWidth().focusRequester(first)) {adding=true}
                    LaunchedEffect(Unit) {withFrameNanos {};runCatching {first.requestFocus()}}
                    LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(3.dp)) {
                        if(rows.isEmpty()) item {Text(tr("Escanea el QR con tu celular, escribe el nombre y pega la URL del stream. TV y celular deben estar en la misma red.","Scan the QR on your phone, enter a name and paste the stream URL. TV and phone must be on the same network."),color=Muted,fontSize=14.sp)}
                        items(rows,key={it.id}) {channel ->
                            var focused by remember {mutableStateOf(false)}
                            val play={if(state.partyRole!=PartyRole.GUEST && !state.kidsMode) {close();state.openLivePlayer(CustomChannelsRP45.request(channel))}}
                            val edit={editing=channel;adding=true}
                            Column(Modifier.fillMaxWidth().onFocusChanged {focused=it.isFocused}.focusable().tvPlaybackClick(play,edit).clickable(onClick=play)
                                .background(if(focused) Color(0xFF292929) else Color(0xFF181818),RoundedCornerShape(12.dp))
                                .border(if(focused) 2.dp else 1.dp,if(focused) Color.White else PanelBorder,RoundedCornerShape(12.dp)).padding(15.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                Text(channel.name,color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold)
                                Text(tr("CANAL PERSONAL · EN VIVO","PERSONAL CHANNEL · LIVE"),color=Muted,fontSize=10.sp)
                            }
                        }
                    }
                    FocusButton(tr("Cerrar","Close")) {close()}
                }
            }
        }
    }
}

@Composable
private fun CustomChannelPairingRP45(state:AppState,editing:CustomChannelRP45?,saved:()->Unit,modifier:Modifier) {
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
    DisposableEffect(owner,editing?.id,revision,state.appForeground,state.kidsMode,state.partyRole) {
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
                server=CustomChannelServerRP45(endpoint.address as Inet4Address,endpoint.prefixLength,editing?.name.orEmpty(),editing?.url.orEmpty()) {name,url ->
                    val latch=CountDownLatch(1);val accepted=AtomicBoolean(false)
                    main.post {
                        try {
                            if(alive.get() && state.appForeground && CustomChannelsRP45.owner(state)==owner && !state.kidsMode && state.partyRole!=PartyRole.GUEST) {
                                accepted.set(CustomChannelsRP45.save(context,owner,editing?.id,name,url))
                                if(accepted.get()) {currentSaved();message=tr("✓ Canal guardado. Pulsa Volver para verlo.","✓ Channel saved. Press Back to see it.")}
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
        Text(if(editing==null) tr("Agregar canal","Add channel") else tr("Editar canal","Edit channel"),color=Color.White,fontSize=22.sp)
        Text(tr("Escanea el QR en la misma red que tu TV. Escribe el nombre y pega la URL del stream.","Scan the QR on the same network as your TV. Enter a name and paste the stream URL."),color=Muted,fontSize=13.sp)
        receiver?.let {server ->
            if(now<server.until) {QrCode(server.url,180.dp);Text(tr("QR válido por ","QR valid for ")+((server.until-now)/1000).coerceAtLeast(0)+"s",color=Muted,fontSize=12.sp)}
            else Text(tr("QR vencido. Genera uno nuevo.","QR expired. Generate a new one."),color=Muted)
        }
        if(message.isNotBlank()) Text(message,color=Color.White,fontSize=13.sp)
        FocusButton(tr("Nuevo QR","New QR")) {revision++}
        Text(tr("Guardado en este perfil de esta TV. Los enlaces temporales pueden caducar.","Saved to this profile on this TV. Temporary links can expire."),color=Muted,fontSize=11.sp)
    }
}
