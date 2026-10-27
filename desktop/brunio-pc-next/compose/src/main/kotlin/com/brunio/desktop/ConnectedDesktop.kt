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
fun ConnectedDesktop() {
    val scope = rememberCoroutineScope()
    val bridge = remember { BridgeClient() }
    val controller = remember { AppController(scope, bridge) }
    DisposableEffect(bridge) { onDispose { bridge.close() } }
    LaunchedEffect(controller) { controller.restore() }
    val account = controller.account
    when {
        account == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (controller.message == null) CircularProgressIndicator(color = Color.White)
            else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(controller.message ?: "")
                TextButton(onClick = { controller.message = null; controller.restore() }) { Text("Reintentar conexión local") }
            }
        }
        !account.optBoolean("signedIn") -> {
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            Box(Modifier.fillMaxSize().background(Color(0xFF080809)), contentAlignment = Alignment.Center) {
                Column(Modifier.width(420.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("BruniO", style = MaterialTheme.typography.displayMedium)
                    Text("Tu cuenta, tus perfiles y tu contenido.", color = Color.Gray)
                    OutlinedTextField(email, { email = it }, label = { Text("Correo") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp))
                    OutlinedTextField(password, { password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp))
                    Button(onClick = { controller.login(email.trim(), password); password = "" }, enabled = !controller.screen.busy && email.isNotBlank() && password.isNotEmpty(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) { Text("Entrar", Modifier.padding(8.dp)) }
                }
            }
        }
        controller.profilePicker -> ProfilePicker(controller.profiles(), { controller.selectProfile(it) }, { controller.message = "La edición de perfiles aún no está migrada." })
        else -> Box(Modifier.fillMaxSize().background(Color(0xFF080809))) {
            Column(Modifier.fillMaxSize().padding(start = 76.dp)) {
                if (controller.tab == DesktopTab.Calendar) Row(Modifier.fillMaxWidth().padding(horizontal = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { controller.changeMonth(-1) }) { Text("‹ Mes anterior") }
                    Text(controller.month.toString(), Modifier.weight(1f))
                    TextButton(onClick = { controller.changeMonth(1) }) { Text("Mes siguiente ›") }
                }
                DesktopScreen(controller.tab, controller.screen, controller::action, controller::search)
            }
            DesktopNavigation(controller.tab, { controller.select(it) }, controller::showProfiles)
        }
    }
    if (controller.addFriend) {
        var nickname by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { controller.addFriend = false }, title = { Text("Agregar amigo") },
            text = { OutlinedTextField(nickname, { nickname = it }, label = { Text("Nombre de usuario") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { controller.sendFriend(nickname) }, enabled = nickname.length >= 3) { Text("Enviar solicitud") } },
            dismissButton = { TextButton(onClick = { controller.addFriend = false }) { Text("Cancelar") } })
    }
    controller.detail?.let { value ->
        val item = value.getJSONObject("item")
        AlertDialog(onDismissRequest = { controller.detail = null }, title = { Text(item.optString("title")) },
            text = { Column(Modifier.widthIn(max = 620.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                RemoteArtwork(item.optString("backdrop"), Modifier.fillMaxWidth().height(220.dp))
                Text(item.optString("overview"))
                Text("El motor de reproducción todavía no está conectado a esta interfaz.", color = Color.Gray)
            } }, confirmButton = { TextButton(onClick = { controller.detail = null }) { Text("Cerrar") } })
    }
    controller.message?.takeIf { account != null }?.let { message ->
        AlertDialog(onDismissRequest = { controller.message = null }, title = { Text("BruniO") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = { controller.message = null }) { Text("Cerrar") } })
    }
}
