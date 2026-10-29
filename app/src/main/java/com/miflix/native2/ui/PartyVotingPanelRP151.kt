package com.miflix.native2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.miflix.native2.data.*
import com.miflix.native2.model.PartyRole
import kotlinx.coroutines.*

@Composable
internal fun PartyVotingPanelRP151(state:AppState,close:()->Unit) {
    val scope=rememberCoroutineScope()
    val room=state.watchParty ?: return
    var voter by remember(room.roomCode) {mutableStateOf("")}
    var busy by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    var now by remember {mutableLongStateOf(System.currentTimeMillis())}
    val host=state.partyRole==PartyRole.HOST
    val ballot=state.currentBallotRP151()
    LaunchedEffect(room.roomCode) {voter=state.ballotVoterRP151();while(isActive) {now=System.currentTimeMillis();delay(1000)}}
    fun send(body:String) {if(busy) return;busy=true;scope.launch {try {state.sendBallotRP151(body);message=tr("Enviado. Esperando confirmación del party…","Sent. Waiting for party confirmation…")} catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo enviar. Reintenta.","Could not send. Retry.")} finally {busy=false}}}
    PlayerSideSheet(tr("¿Qué vemos juntos?","What shall we watch together?"),close) {
        item {CinemaBanner("BRUNIO · WATCH TOGETHER",tr("Tu grupo decide","Your group decides"),tr("Un voto por persona; puedes cambiarlo. El host confirma qué reproducir.","One vote per person; change it anytime. The host confirms playback."))}
        if(ballot!=null) {
            val active=ballot.active(now)
            item {Text(if(active) tr("Votación abierta","Voting open") else tr("Votación cerrada","Voting closed"),color=Accent,fontSize=18.sp)}
            ballot.options.forEachIndexed {index,pick ->item {
                val count=ballot.votes.values.count {it==index}
                FocusButton((if(ballot.votes[voter]==index) "✓ " else "")+pick.title+if(pick.season>0) " · S${pick.season} E${pick.episode} · $count" else " · $count",primary=ballot.votes[voter]==index,modifier=Modifier.fillMaxWidth()) {if(active && !busy) send(PartyVotingRP151.vote(ballot.token,index))}
            }}
            val winners=ballot.winners()
            if(host && active && winners.isNotEmpty()) {
                if(winners.size>1) item {Text(tr("Empate: el host elige entre los más votados.","Tie: the host chooses among the winners."),color=Muted)}
                winners.forEach {index ->item {
                    FocusButton(tr("Reproducir: ","Play: ")+ballot.options[index].title,modifier=Modifier.fillMaxWidth()) {
                        if(!busy) {busy=true;scope.launch {try {
                            val pick=ballot.options[index]
                            val media=state.tmdb.byCloudId(pick.cloud) ?: error("Title unavailable")
                            if(pick.type=="series") {
                                val ep=state.tmdb.season(pick.id,pick.season).firstOrNull {it.episode==pick.episode} ?: error("Episode unavailable")
                                if(ep.airDate.isBlank() || ep.airDate>java.time.LocalDate.now().toString()) error("Episode unavailable")
                                state.open(media);check(state.selected?.cloudId==media.cloudId);state.playEpisode(ep)
                            } else {state.open(media);check(state.selected?.cloudId==media.cloudId);state.playMovie()}
                            state.sendBallotRP151(PartyVotingRP151.close(ballot.token));close()
                        } catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo iniciar. Prueba otra opción.","Could not start playback. Try another option.")} finally {busy=false}}}
                    }
                }}
            }
            if(host && active) item {FocusButton(tr("Cerrar votación","Close voting")) {send(PartyVotingRP151.close(ballot.token))}}
        }
        if(host && (ballot==null || !ballot.active(now))) item {
            FocusButton(if(busy) tr("Preparando…","Preparing…") else tr("Nueva votación · 3 opciones","New vote · 3 choices"),primary=true,modifier=Modifier.fillMaxWidth()) {
                if(!busy) {busy=true;message="";scope.launch {try {
                    val request=state.playerRequest
                    val options=mutableListOf<PartyPickRP151>()
                    if(request!=null && request.item.type=="series" && !request.live) {
                        state.nextEpisode(request)?.let {options+=PartyPickRP151(request.item.id,"series",request.item.title,it.season,it.episode)}
                    }
                    val pool=(state.continueWatching.toList()+state.discoveryRun("happy")).filter {it.type=="movie"}.distinctBy {it.cloudId}.take(12)
                    val allowed=if(state.kidsMode) state.tmdb.kidsFilter(pool) else pool
                    options+=allowed.take(3-options.size).map {PartyPickRP151(it.id,it.type,it.title)}
                    require(options.size>=2)
                    state.sendBallotRP151(PartyVotingRP151.start(options));message=tr("Votación enviada. Esperando confirmación…","Vote sent. Waiting for confirmation…")
                } catch(e:CancellationException) {throw e} catch(_:Exception) {message=tr("No se pudo crear la votación. Reintenta.","Could not create a vote. Retry.")} finally {busy=false}}}
            }
        }
        if(!host && ballot==null) item {Text(tr("El host puede abrir una votación desde este panel.","The host can open a vote from this panel."),color=Muted)}
        if(message.isNotBlank()) item {Text(message,color=Muted,fontSize=13.sp)}
        item {Text(tr("Se actualiza con el party existente. Usa la misma versión en todos los equipos. La votación vence a los 10 minutos.","Updates with the existing party connection. Use the same version on all devices. Voting expires after 10 minutes."),color=Muted,fontSize=12.sp)}
    }
}
