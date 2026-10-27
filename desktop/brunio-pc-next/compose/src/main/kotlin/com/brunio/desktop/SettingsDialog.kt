package com.brunio.desktop
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import org.json.JSONObject

@Composable
fun SettingsDialog(settings: JSONObject, onSave: (String, Any) -> Unit, onClose: () -> Unit) {
    var limit by remember(settings) { mutableStateOf(settings.optDouble("limit", 0.0).toString()) }
    AlertDialog(onDismissRequest = onClose, title = { Text("Reproducción y preferencias") },
        text = { Column(Modifier.widthIn(min = 360.dp).heightIn(max = 600.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf("autoplay" to "Siguiente episodio automático", "retry" to "Probar otra fuente si falla").forEach { (key, title) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f)); Switch(checked = settings.optBoolean(key, true), onCheckedChange = { onSave(key, it) })
                }
            }
            listOf("audio" to "Audio", "sub" to "Subtítulos").forEach { (key, title) ->
                Text(title, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (if(key == "sub") listOf("es", "en", "off") else listOf("es", "en")).forEach { value ->
                        FilterChip(selected = settings.optString(key) == value, onClick = { onSave(key, value) }, label = { Text(when(value) { "es" -> "Español"; "en" -> "English"; else -> "Desactivar" }) })
                    }
                }
            }
            OutlinedTextField(limit, { limit = it }, label = { Text("Límite por archivo · GB (0 = sin límite)") }, singleLine = true)
            TextButton(onClick = { limit.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..500.0 }?.let { onSave("limit", it) } }, enabled = limit.toDoubleOrNull()?.let { it.isFinite() && it in 0.0..500.0 } == true) { Text("Guardar límite para PC") }
        } }, confirmButton = { TextButton(onClick = onClose) { Text("Cerrar") } })
}
