package com.miflix.native2.ui

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.data.LocalCastServer
import com.miflix.native2.data.TorBoxKeyServerRP76
import com.miflix.native2.model.PartyRole
import java.net.Inet4Address
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

@Composable
internal fun TorBoxKeyQrRP76(state:AppState,received:(String)->Unit,close:()->Unit) {
    val context=LocalContext.current.applicationContext
    val manager=remember {context.getSystemService(ConnectivityManager::class.java)}
    val main=remember {Handler(Looper.getMainLooper())}
    val owner=CustomChannelsRP45.owner(state)
    val onReceived by rememberUpdatedState(received)
    var receiver by remember {mutableStateOf<TorBoxKeyServerRP76?>(null)}
    var message by remember {mutableStateOf("")}
    var revision by remember {mutableIntStateOf(0)}
    var now by remember {mutableLongStateOf(System.currentTimeMillis())}
    LaunchedEffect(Unit) {while(true) {delay(1000);now=System.currentTimeMillis()}}
    DisposableEffect(owner,revision,state.appForeground,state.kidsMode,state.partyRole,state.screen,state.sessionNeedsSignIn) {
        val alive=AtomicBoolean(true)
        var server:TorBoxKeyServerRP76?=null
        receiver=null;message=""
        if(state.screen==Screen.SETTINGS && state.session!=null && !state.sessionNeedsSignIn && state.appForeground && !state.kidsMode && state.partyRole!=PartyRole.GUEST) try {
            @Suppress("DEPRECATION")
            val endpoint=manager.allNetworks.sortedBy {it!=manager.activeNetwork}.mapNotNull {network ->
                val caps=manager.getNetworkCapabilities(network)
                if(caps==null || caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) || (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) null
                else manager.getLinkProperties(network)?.linkAddresses?.firstOrNull {it.address is Inet4Address && LocalCastServer.isPrivate(it.address.address)}
            }.firstOrNull()
            if(endpoint==null) message=tr("Conecta la TV a Wi-Fi o Ethernet.","Connect the TV to Wi-Fi or Ethernet.")
            else {
                server=TorBoxKeyServerRP76(endpoint.address as Inet4Address,endpoint.prefixLength) {key ->
                    val latch=CountDownLatch(1);val accepted=AtomicBoolean(false)
                    main.post {
                        try {
                            if(alive.get() && state.screen==Screen.SETTINGS && state.session!=null && !state.sessionNeedsSignIn && state.appForeground &&
                                CustomChannelsRP45.owner(state)==owner && !state.kidsMode && state.partyRole!=PartyRole.GUEST && TorBoxKeyServerRP76.valid(key)) {
                                onReceived(key)
                                message=tr("✓ Clave recibida. Vuelve y pulsa Verificar y guardar.","✓ Key received. Go back and select Verify and save.")
                                accepted.set(true)
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
    PlayerMenuDialog(onDismissRequest=close,fullScreen=true) {
        BoxWithConstraints(Modifier.fillMaxSize().consumeOpeningPress()) {
            Column(Modifier.align(Alignment.Center).widthIn(max=620.dp).fillMaxWidth(.9f).heightIn(max=maxHeight*.94f)
                .background(Color(0xFC101010),RoundedCornerShape(22.dp)).verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Text(tr("TorBox · API key por QR","TorBox · API key via QR"),color=Color.White,fontSize=24.sp)
                Text(tr("Escanea y pega tu clave desde el celular. Ambos dispositivos deben estar en la misma red local.",
                    "Scan and paste your key from your phone. Both devices must use the same local network."),color=Muted,fontSize=14.sp)
                receiver?.let {server ->
                    if(now<server.until) {
                        QrCode(server.url,190.dp)
                        Text(tr("Válido por ","Valid for ")+((server.until-now)/1000).coerceAtLeast(0)+"s",color=Muted,fontSize=12.sp)
                    } else Text(tr("QR vencido. Genera uno nuevo.","QR expired. Generate a new one."),color=Muted)
                }
                if(message.isNotBlank()) Text(message,color=Color.White,fontSize=14.sp)
                FocusButton(tr("Nuevo QR","New QR")) {revision++}
                FocusButton(tr("Volver a Settings","Back to Settings"),onClick=close)
                Text(tr("Solo rellena el campo de la TV. No guarda ni activa el motor hasta que tú lo decidas. No usa Supabase.",
                    "Only fills the TV field. It does not save or enable the engine until you choose to. No Supabase calls."),color=Muted,fontSize=12.sp)
            }
        }
    }
}
