package com.nuvio.app.features.brunio

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.features.party.PartyMedia
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Nuvio's existing screen shell, backed exclusively by BruniO RP1. */
@Composable
internal fun BrunioPartyScreen(
    initialCode: String,
    onBack: () -> Unit,
    onWatch: (PartyMedia) -> Unit,
) {
    val state by BrunioPartyService.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var code by remember(initialCode) { mutableStateOf(initialCode.take(6)) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun action(block: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            try {
                block()
            } catch (e: Exception) {
                error = "No se pudo completar la solicitud. Revisa tu conexión."
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(initialCode) {
        if (initialCode.length == 6 && initialCode.all(Char::isDigit) &&
            state.roomCode.isBlank() && state.pendingCode.isBlank()
        ) {
            runCatching { BrunioPartyService.joinRoom(initialCode) }
                .onFailure { error = "No se pudo solicitar acceso a esa sala." }
        }
    }
    LaunchedEffect(state.roomCode, state.pendingCode) {
        while (isActive) {
            delay(3500)
            if (state.roomCode.isNotBlank()) {
                runCatching { BrunioPartyService.refresh() }
            } else if (state.pendingCode.isNotBlank()) {
                runCatching { BrunioPartyService.joinRoom(state.pendingCode) }
            } else break
        }
    }

    BrunioReadableSurface {
    NuvioScreen(modifier = Modifier.fillMaxSize()) {
        stickyHeader { NuvioScreenHeader(title = "Watch Party · BruniO", onBack = onBack) }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("Comparte una película o episodio con tus amigos", style = MaterialTheme.typography.titleMedium)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    when {
                        state.roomCode.isBlank() && state.pendingCode.isNotBlank() -> {
                            Text("Solicitud enviada a la sala ${state.pendingCode}. Esperando aprobación del anfitrión.")
                            OutlinedButton(onClick = { BrunioPartyService.reset() }) { Text("Cancelar solicitud") }
                        }
                        state.roomCode.isBlank() -> {
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it.filter(Char::isDigit).take(6) },
                                label = { Text("Código de seis dígitos") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { action { BrunioPartyService.joinRoom(code) } },
                                    enabled = !busy && code.length == 6) { Text("Solicitar acceso") }
                                OutlinedButton(onClick = { action { BrunioPartyService.createRoom() } },
                                    enabled = !busy) { Text("Crear Party") }
                            }
                        }
                        else -> {
                            Text("Sala ${state.roomCode}", style = MaterialTheme.typography.headlineMedium)
                            Text(if (state.host) "Eres el anfitrión" else "Conectado a la sala")
                            Text("Participantes: ${state.members.size}", style = MaterialTheme.typography.bodyMedium)
                            state.media?.let { media ->
                                Text("Contenido: ${media.title.ifBlank { media.metaId }}")
                                OutlinedButton(onClick = { onWatch(media) }) { Text("Abrir contenido") }
                            }
                            if (state.host && state.requests.isNotEmpty()) {
                                Text("Solicitudes de acceso", style = MaterialTheme.typography.titleMedium)
                                state.requests.forEach { request ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(request.nickname.ifBlank { "Invitado" }, modifier = Modifier.weight(1f))
                                        Button(onClick = { action { BrunioPartyService.approve(request.userId, true) } },
                                            enabled = !busy) { Text("Aceptar") }
                                        OutlinedButton(onClick = { action { BrunioPartyService.approve(request.userId, false) } },
                                            enabled = !busy) { Text("Rechazar") }
                                    }
                                }
                            }
                            Text("Chat y reacciones", style = MaterialTheme.typography.titleMedium)
                            state.messages.takeLast(40).forEach { event ->
                                Text("${event.sender.take(8)} · ${event.body}")
                            }
                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it.take(400) },
                                label = { Text("Mensaje") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(onClick = { action {
                                BrunioPartyService.message("chat", message)
                                message = ""
                            } }, enabled = !busy && message.isNotBlank()) { Text("Enviar") }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf("😂", "❤️", "🔥", "🍿").forEach { emoji ->
                                    OutlinedButton(onClick = { action {
                                        BrunioPartyService.message("emoji", emoji)
                                    } }, enabled = !busy) { Text(emoji) }
                                }
                            }
                            Text(
                                "Sincronización de reproducción TV/Mobile/PC pendiente de pruebas de interoperabilidad RP1.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            OutlinedButton(onClick = { action { BrunioPartyService.leave() } },
                                enabled = !busy) { Text("Salir de Party") }
                        }
                    }
                }
            }
        }
    }
    }
}
