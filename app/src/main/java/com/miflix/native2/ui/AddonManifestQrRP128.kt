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
import com.miflix.native2.data.AddonManifestServerRP128
import com.miflix.native2.data.LocalCastServer
import com.miflix.native2.model.PartyRole
import java.net.Inet4Address
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

@Composable
internal fun AddonManifestQrRP128(state:AppState,received:(String)->Unit,close:()->Unit) {
    val context=LocalContext.current.applicationContext
    val manager=remember {context.getSystemService(ConnectivityManager::class.java)}
    val main=remember {Handler(Looper.getMainLooper())}
    val owner=CustomChannelsRP45.owner(state)
    val onReceived by rememberUpdatedState(received)
    var receiver by remember {mutableStateOf<AddonManifestServerRP128?>(null)}
    var message by remember {mutableStateOf("")}
    var revision by remember {mutableIntStateOf(0)}
    var now by remember {mutableLongStateOf(System.currentTimeMillis())}
    LaunchedEffect(Unit) {while(true) {delay(1000);now=System.currentTimeMillis()}}
    DisposableEffect(owner,revision,state.appForeground,state.kidsMode,state.partyRole,state.screen,state.sessionNeedsSignIn) {
        val alive=AtomicBoolean(true)
        var server:AddonManifestServerRP128?=null
        receiver=null;message=""
        if(state.screen==Screen.SETTINGS && state.session!=null && !state.sessionNeedsSignIn && state.appForeground && !state.kidsMode && state.partyRole!=PartyRole.GUEST) try {
            val network=manager.activeNetwork
            val caps=network?.let {manager.getNetworkCapabilities(it)}
            val active=if(network!=null && caps!=null && !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)))
                manager.getLinkProperties(network)?.linkAddresses?.firstOrNull {it.address is Inet4Address && LocalCastServer.isPrivate(it.address.address)}
                    ?.takeIf {it.prefixLength in 8..30}?.let {com.miflix.native2.data.LocalQrNetworkRP129.Endpoint(it.address as Inet4Address,it.prefixLength)} else null
            val endpoint=(listOfNotNull(active)+com.miflix.native2.data.LocalQrNetworkRP129.find()).distinctBy {it.address.hostAddress}.firstOrNull()
            if(endpoint==null) message=tr("Conecta la TV y el celular a la misma red Wi-Fi o Ethernet.","Connect your TV and phone to the same Wi-Fi or Ethernet network.")
            else {
                server=AddonManifestServerRP128(endpoint.address,endpoint.prefixLength) {url ->
                    val latch=CountDownLatch(1);val accepted=AtomicBoolean(false)
                    main.post {
                        try {
                            if(alive.get() && state.screen==Screen.SETTINGS && state.session!=null && !state.sessionNeedsSignIn &&
                                state.appForeground && CustomChannelsRP45.owner(state)==owner && !state.kidsMode && state.partyRole!=PartyRole.GUEST) {
                                onReceived(url)
                                message=tr("✓ Enlace recibido. Vuelve al formulario para comprobar e instalar.","✓ Link received. Return to the form to check and install.")
                                accepted.set(true)
                            }
                        } finally {latch.countDown()}
                    }
                    latch.await(3,TimeUnit.SECONDS) && accepted.get()
                }
                receiver=server
            }
        } catch(_:Exception) {message=tr("No se pudo abrir el QR. Genera uno nuevo.","Could not open the QR. Generate a new one.")}
        else message=tr("Abre Ajustes con tu sesión iniciada para recibir el enlace.","Open Settings while signed in to receive the link.")
        onDispose {alive.set(false);server?.close()}
    }
    PlayerMenuDialog(onDismissRequest=close,fullScreen=true) {
        BoxWithConstraints(Modifier.fillMaxSize().consumeOpeningPress(),contentAlignment=Alignment.Center) {
            Column(Modifier.widthIn(max=620.dp).fillMaxWidth(.92f).heightIn(max=maxHeight*.94f)
                .background(Bg,RoundedCornerShape(24.dp)).verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(tr("Add-ons · QR local","Add-ons · Local QR"),color=Color.White,fontSize=24.sp,modifier=Modifier.weight(1f))
                    FocusButton("×",horizontalPadding=16.dp,onClick=close)
                }
                Text(tr("Escanea con tu celular y pega el enlace manifest.json. Ambos dispositivos deben estar en la misma red.","Scan with your phone and paste the manifest.json link. Both devices must use the same network."),color=Muted,fontSize=14.sp)
                receiver?.let {server ->
                    if(now<server.until) {
                        QrCode(server.url,190.dp)
                        Text(tr("Válido por ","Valid for ")+((server.until-now)/1000).coerceAtLeast(0)+"s",color=Muted,fontSize=12.sp)
                    } else Text(tr("QR vencido. Genera uno nuevo.","QR expired. Generate a new one."),color=Muted)
                }
                if(message.isNotBlank()) Text(message,color=Color.White,fontSize=14.sp)
                FocusButton(tr("Volver al formulario","Back to form"),onClick=close)
                FocusButton(tr("Nuevo QR","New QR")) {revision++}
                Text(tr("El QR solo rellena el enlace. La comprobación y la instalación se confirman en la TV.","The QR only fills the link. Checking and installation are confirmed on the TV."),color=Muted,fontSize=12.sp)
            }
        }
    }
}
