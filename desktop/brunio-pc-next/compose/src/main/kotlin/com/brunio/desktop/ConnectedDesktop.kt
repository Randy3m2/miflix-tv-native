package com.brunio.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ConnectedDesktop(onFullscreen: () -> Unit, onPip: () -> Unit, onPlayerClosed: () -> Unit) {
    val scope = rememberCoroutineScope()
    val bridge = remember { BridgeClient() }
    val controller = remember { AppController(scope, bridge) }
    val utilities = remember { Utilities(scope, bridge) }
    DisposableEffect(bridge) { onDispose { bridge.close() } }
    LaunchedEffect(controller) { controller.restore() }
    CompositionLocalProvider(LocalUiLanguage provides controller.uiLanguage) {
    val account = controller.account
    LaunchedEffect(account?.optString("scope")) { utilities.reset() }
    fun action(id: String) {
        when(id) {
            "party:open" -> utilities.open("party")
            "calendar:subscriptions" -> utilities.open("subscriptions")
            "settings:Conectar cuenta" -> utilities.open("account")
            "settings:Administrar perfiles" -> utilities.open("profiles")
            "settings:Carpeta de descargas" -> utilities.openDownloadsFolder()
            "settings:Limpiar caché de imágenes" -> {ArtworkCache.clear();clearAnimatedArtwork();controller.message="Caché de imágenes limpiada"}
            "settings:Buscar actualización" -> utilities.open("updates")
            "settings:Administrar complementos", "settings:Prioridad de complementos" -> utilities.open("addons")
            else -> if(id.startsWith("join:")){utilities.open("party");utilities.join(id.substringAfter(":"))}else controller.action(id)
        }
    }
    val playback = controller.playback
    LaunchedEffect(playback) { utilities.playbackActive=playback!=null }
    LaunchedEffect(utilities.party) { utilities.party?.let { controller.applyParty(it) } }
    when {
        controller.multiview.isNotEmpty() -> MultiViewScreen(controller,onFullscreen)
        playback != null -> PlayerScreen(playback, { if(utilities.party?.isNull("room")==false)utilities.leave();controller.closePlayer();onPlayerClosed() }, onFullscreen, { position, duration -> controller.saveProgress(playback, position, duration) }, { controller.nextEpisode(playback) }, { if(it=="pip")onPip() else if(it in listOf("scores","stats"))utilities.open("scores") else if(it=="queue")controller.queueDialog=true else if(it=="channels"){controller.channelPicker=true;controller.loadMulti(if(playback.item.optString("liveProvider")=="sports")"sports" else "tv")} else if(it=="episodes")controller.action(playback.item.optString("cloudId")) else if(it=="multiview"){if(utilities.party?.isNull("room")==false)controller.message="Sal de la sala antes de abrir Multiview" else{onPlayerClosed();controller.beginMulti()}} else if(it in listOf("party","chat","reactions")) utilities.open("party") else controller.message = "La función $it aún no está integrada." }, party=utilities.party, onPartyTick={position,playing->utilities.tick(playback,position,playing,controller::applyParty)}, onRequestPlaying=utilities::requestPlaying,hasQueued=controller.queue.isNotEmpty())
        account == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (controller.message == null) CircularProgressIndicator(color = Color.White)
            else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(controller.message ?: "")
                TextButton(onClick = { controller.message = null; controller.restore() }) { Text(ui("Reintentar conexión local")) }
            }
        }
        !account.optBoolean("signedIn") -> {
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            Box(Modifier.fillMaxSize().background(Color(0xFF080809)), contentAlignment = Alignment.Center) {
                Column(Modifier.width(420.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(ui("BruniO"), style = MaterialTheme.typography.displayMedium)
                    Text(ui("Tu cuenta, tus perfiles y tu contenido."), color = Color.Gray)
                    OutlinedTextField(email, { email = it }, label = { Text(ui("Correo")) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp))
                    OutlinedTextField(password, { password = it }, label = { Text(ui("Contraseña")) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp))
                    Button(onClick = { controller.login(email.trim(), password); password = "" }, enabled = !controller.screen.busy && email.isNotBlank() && password.isNotEmpty(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) { Text(ui("Entrar"), Modifier.padding(8.dp)) }
                }
            }
        }
        controller.profilePicker -> ProfilePicker(controller.profiles(), { controller.selectProfile(it) }, { utilities.open("profiles") })
        else -> Box(Modifier.fillMaxSize().background(Color(0xFF080809))) {
            Column(Modifier.fillMaxSize().padding(start = 76.dp)) {
                if (controller.tab == DesktopTab.Calendar) Row(Modifier.fillMaxWidth().padding(horizontal = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { controller.changeMonth(-1) }) { Text(ui("‹ Mes anterior")) }
                    Text(controller.month.toString(), Modifier.weight(1f))
                    TextButton(onClick={utilities.open("subscriptions")}){Text(ui("Suscripciones"))}
                    TextButton(onClick = { controller.changeMonth(1) }) { Text(ui("Mes siguiente ›")) }
                }
                if(controller.tab == DesktopTab.Downloads) DownloadsScreen(utilities) { data ->
                    controller.playback = PlaybackSession(data.getJSONObject("item"), data.getJSONArray("streams").objects(), data.getString("scope"), data.optInt("season"), data.optInt("episode"))
                } else DesktopScreen(controller.tab, controller.screen, ::action, controller::search)
            }
            DesktopNavigation(controller.tab, { controller.select(it) }, controller::showProfiles)
        }
    }
    if(controller.channelPicker)ChannelPicker(controller)
    if(utilities.pinned!=null && (playback!=null || controller.multiview.isNotEmpty()))PinnedScore(utilities)
    if(controller.queueDialog) {
        AlertDialog(
            onDismissRequest={controller.queueDialog=false},
            title={Text(ui("Cola de reproducción"))},
            text={
                Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState())) {
                    if(controller.queue.isEmpty())Text(ui("Añade películas o episodios desde sus detalles."))
                    controller.queue.toList().forEachIndexed { index,entry ->
                        Row {
                            Text(entry.item.optString("title")+(if(entry.season>0)" · T${entry.season} E${entry.episode}" else ""),Modifier.weight(1f))
                            TextButton(onClick={controller.playQueued(index)},enabled=utilities.party?.isNull("room")!=false || utilities.party?.optBoolean("host")==true){Text(ui("Reproducir"))}
                            TextButton(onClick={controller.queue.removeAt(index)}){Text(ui("Quitar"))}
                        }
                    }
                }
            },
            confirmButton={TextButton(onClick={controller.queueDialog=false}){Text(ui("Cerrar"))}}
        )
    }
    UtilityDialog(utilities,controller::accountChanged)
    controller.settings?.let { SettingsDialog(it, controller::saveSetting, { controller.settings = null }) }
    if (controller.addFriend) {
        var nickname by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { controller.addFriend = false }, title = { Text(ui("Agregar amigo")) },
            text = { OutlinedTextField(nickname, { nickname = it }, label = { Text(ui("Nombre de usuario")) }, singleLine = true) },
            confirmButton = { TextButton(onClick = { controller.sendFriend(nickname) }, enabled = nickname.length >= 3) { Text(ui("Enviar solicitud")) } },
            dismissButton = { TextButton(onClick = { controller.addFriend = false }) { Text(ui("Cancelar")) } })
    }
    controller.detail?.let { value ->
        val item=value.getJSONObject("item")
        val guest=utilities.party?.isNull("room")==false && utilities.party?.optBoolean("host")!=true
        DetailsSheet(value,controller.episodes,canPlay=!guest,onAction={event->
            when(event) {
                DetailAction.Close->controller.detail=null
                is DetailAction.Episodes->controller.chooseEpisodes(item,event.season)
                is DetailAction.Play->if(!guest)controller.play(item,event.season,event.episode)
                is DetailAction.Download->utilities.transfer(item,event.season,event.episode,event.fullSeason)
                is DetailAction.Queue->controller.queue.add(QueueEntry(item,event.season,event.episode))
                is DetailAction.Saved->controller.detailMutation("catalog.favorite",org.json.JSONObject().put("enabled",event.enabled))
                is DetailAction.Rate->controller.detailMutation("catalog.rate",org.json.JSONObject().put("score",event.score))
                is DetailAction.Mark->controller.detailMutation("catalog.mark",org.json.JSONObject().put("watched",event.watched).put("season",event.season).put("episode",event.episode))
                is DetailAction.Person->if(playback==null)controller.openPerson(event.id,event.name)else controller.message="Cierra el reproductor para explorar el reparto."
            }
        })
    }
    if(utilities.dialog == null && utilities.error != null) AlertDialog(onDismissRequest={utilities.error=null},title={Text(ui("BruniO"))},text={Text(utilities.error?:"")},confirmButton={TextButton(onClick={utilities.error=null}){Text(ui("Cerrar"))}})
    controller.message?.takeIf { account != null }?.let { message ->
        AlertDialog(onDismissRequest = { controller.message = null }, title = { Text(ui("BruniO")) }, text = { Text(ui(message)) },
            confirmButton = { TextButton(onClick = { controller.message = null }) { Text(ui("Cerrar")) } })
    }
    }
}
