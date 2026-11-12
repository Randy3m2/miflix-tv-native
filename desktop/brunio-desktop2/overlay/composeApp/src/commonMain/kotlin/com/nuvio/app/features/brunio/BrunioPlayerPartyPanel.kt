package com.nuvio.app.features.brunio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.AuthState
import com.nuvio.app.features.party.PartyMedia
import com.nuvio.app.features.player.PlayerDialogButton
import com.nuvio.app.features.player.PlayerPanelHeader
import com.nuvio.app.features.player.PlayerSidePanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Native in-player BruniO RP1 controls. Nuvio watch_party_* tables are never
 * used. Sources are selected locally: the room only synchronizes TMDB identity,
 * play/pause and position. No stream URL or add-on token is shared.
 */
@Composable
internal fun BrunioPlayerPartyPanel(
    visible: Boolean,
    width: Dp,
    modal: Boolean,
    media: PartyMedia,
    positionMs: Long,
    playing: Boolean,
    onOpenSources: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val session by AuthRepository.state.collectAsStateWithLifecycle()
    val party by BrunioPartyService.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    fun action(block: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            error = ""
            try {
                block()
            } catch (e: Exception) {
                error = "No se pudo completar la operación de Party. Revisa tu conexión."
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(visible, party.roomCode, party.pendingCode) {
        if (!visible) return@LaunchedEffect
        while (isActive && (party.roomCode.isNotEmpty() || party.pendingCode.isNotEmpty())) {
            delay(3500)
            runCatching {
                if (party.roomCode.isNotEmpty()) BrunioPartyService.refresh()
                else BrunioPartyService.joinRoom(party.pendingCode)
            }
        }
    }

    PlayerSidePanel(
        visible = visible,
        onDismiss = onDismiss,
        width = width,
        modal = modal,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(22.dp)) {
            PlayerPanelHeader(title = "Watch Party · BruniO") {
                PlayerDialogButton(label = "Close", onClick = onDismiss)
            }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                when {
                    session !is AuthState.Authenticated ||
                        (session as? AuthState.Authenticated)?.isAnonymous != false -> {
                        Text("Inicia sesión en BruniO para usar Watch Party.")
                    }
                    party.roomCode.isEmpty() && party.pendingCode.isNotEmpty() -> {
                        Text("Esperando aprobación de la sala: " + party.pendingCode)
                        OutlinedButton(onClick = { BrunioPartyService.reset() }) {
                            Text("Cancelar solicitud")
                        }
                    }
                    party.roomCode.isEmpty() -> {
                        Text("Mira con tus amigos. Cada persona elige su propia fuente.",
                            style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.filter(Char::isDigit).take(6) },
                            label = { Text("Código de Party") },
                            singleLine = true,
                        )
                        Button(
                            onClick = { action { BrunioPartyService.joinRoom(code) } },
                            enabled = !busy && code.length == 6,
                        ) { Text("Unirse") }
                        OutlinedButton(
                            onClick = { action {
                                BrunioPartyService.createRoom()
                                BrunioPartyService.publishPlayerState(media, !playing, positionMs)
                            } },
                            enabled = !busy,
                        ) { Text("Crear sala con este contenido") }
                    }
                    else -> {
                        Text("Sala " + party.roomCode,
                            style = MaterialTheme.typography.headlineSmall)
                        Text(if (party.host) "Anfitrión" else "Invitado")
                        Text("Participantes: " + party.members.size)
                        Button(
                            onClick = {
                                onDismiss()
                                onOpenSources()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Cambiar mi fuente / Source picker") }
                        Text("Tu cambio de fuente no cambia la fuente de los demás.",
                            style = MaterialTheme.typography.bodySmall)
                        Text("Control de reproducción",
                            style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (party.host) {
                                OutlinedButton(onClick = { onSeekBy(-10_000L) }) {
                                    Text("−10 s")
                                }
                            }
                            Button(onClick = {
                                if (party.host) onTogglePlayback()
                                else action {
                                    BrunioPartyService.requestPlayerControl(
                                        media, !playing,
                                    )
                                }
                            }, enabled = !busy) {
                                Text(if (party.host) {
                                    if (playing) "Pausar" else "Reproducir"
                                } else {
                                    if (playing) "Solicitar pausa" else "Solicitar reproducción"
                                })
                            }
                            if (party.host) {
                                OutlinedButton(onClick = { onSeekBy(10_000L) }) {
                                    Text("+10 s")
                                }
                            }
                        }
                        if (party.host && party.requests.isNotEmpty()) {
                            Text("Solicitudes de acceso")
                            party.requests.forEach { request ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(request.nickname.ifBlank { "Invitado" },
                                        modifier = Modifier.weight(1f))
                                    OutlinedButton(
                                        onClick = { action {
                                            BrunioPartyService.approve(request.userId, true)
                                        } },
                                        enabled = !busy,
                                    ) { Text("Aceptar") }
                                    OutlinedButton(
                                        onClick = { action {
                                            BrunioPartyService.approve(request.userId, false)
                                        } },
                                        enabled = !busy,
                                    ) { Text("Rechazar") }
                                }
                            }
                        }
                        Text("Chat", style = MaterialTheme.typography.titleMedium)
                        party.messages.takeLast(35).forEach { event ->
                            Text(event.sender.take(8) + ": " + event.body)
                        }
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it.take(400) },
                            label = { Text("Mensaje") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Button(
                            onClick = { action {
                                BrunioPartyService.message("chat", message)
                                message = ""
                            } },
                            enabled = !busy && message.isNotBlank(),
                        ) { Text("Enviar") }
                        OutlinedButton(
                            onClick = { action { BrunioPartyService.leave() } },
                            enabled = !busy,
                        ) { Text("Salir de la sala") }
                    }
                }
            }
        }
    }
}
