package com.miflix.native2.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miflix.native2.BuildConfig

/** Release notes ship with the APK. No API calls or repeated startup notifications. */
internal object UpdateNotes {
    val id: String get()="update:${BuildConfig.VERSION_CODE}"
    val changes=listOf(
        "Enlaces de Comet y Torrentio con texto ordenado e insignias de Dolby/HDR según sus metadatos." to "Clean Comet and Torrentio link metadata with small Dolby/HDR badges when reported by the source.",
        "Envío local a TV por Wi-Fi, con enlace y código de conexión; sin reproducción web." to "Send titles to TV over local Wi-Fi with a connection link and code; no web playback.",
        "Filmografía con pósteres y canales favoritos en cuadrículas." to "Actor filmography with posters and favorite channels in a grid.",
        "Temas de color para el selector y botones en forma de píldora." to "Focus color themes and pill-shaped buttons.",
        "La tarjeta y la sinopsis se expanden tras 3 segundos de enfoque." to "Cards and synopsis expand after 3 seconds of focus.",
        "Pantalla de carga con fondo y logo del título cuando está disponible." to "Playback loading artwork with the title logo when available.",
        "Mi Lista separada en películas y series." to "My List filters for movies and series.",
        "Temporadas seleccionables al enfocar, sin flechas laterales, también en el player." to "Focus-to-select season rails without arrows, including in the player.",
        "Emojis compactos con una pulsación; frases al mantener pulsado." to "Compact emojis on press; phrases on long press.",
        "Más acciones al mantener Play o un episodio: enlaces manuales y marcar visto." to "Long-press Play or an episode for more actions, manual links and mark watched.",
        "Rate independiente, reparto en fila completa y comentarios debajo; sin tráileres." to "Separate Rate button, full-width cast and reviews below; trailers removed.",
        "Menú de Seguir viendo: detalles, enlace manual, desde el principio y quitar." to "Continue Watching menu: details, manual links, start from beginning and remove.",
        "Notificaciones con marcar leído y novedades de la actualización." to "Mark notifications as read and view update release notes.",
        "Nuevos avatares de Formula 1 y opción de recordar el último perfil." to "New Formula 1 avatars and a remember last profile setting.",
        "Top 10 con ranking destacado y salvapantallas tras 5 minutos en menús." to "Distinctive Top 10 ranking and a screensaver after 5 minutes in menus.",
        "Friends simplificado; administrar perfil bajo el menú de perfil." to "Simplified Friends; manage profile moved to the profile menu.",
        "Retirados X y Restore size de la barra superior del reproductor." to "Removed X and Restore size from the top player toolbar."
    )
}

@androidx.compose.runtime.Composable
internal fun ChangelogDialog(state: AppState,close: ()->Unit) {
    val focus=androidx.compose.runtime.remember {androidx.compose.ui.focus.FocusRequester()}
    androidx.compose.runtime.LaunchedEffect(Unit) {androidx.compose.runtime.withFrameNanos {};runCatching {focus.requestFocus()} }
    androidx.compose.ui.window.Dialog(onDismissRequest=close) {
        androidx.compose.foundation.layout.Column(
            androidx.compose.ui.Modifier.widthIn(max=680.dp).heightIn(max=480.dp).background(Panel,androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).padding(24.dp),
            verticalArrangement=androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)) {
            androidx.tv.material3.Text("Changelog / Details · ${BuildConfig.VERSION_NAME}",color=androidx.compose.ui.graphics.Color.White,fontSize=24.sp)
            androidx.compose.foundation.lazy.LazyColumn(androidx.compose.ui.Modifier.weight(1f,false),verticalArrangement=androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                items(UpdateNotes.changes.size) {index -> val change=UpdateNotes.changes[index];androidx.tv.material3.Text("• "+tr(change.first,change.second),color=Muted,fontSize=15.sp,modifier=androidx.compose.ui.Modifier.fillMaxWidth().contentFocusBorder().focusable().padding(10.dp)) }
            }
            FocusButton(tr("Cerrar","Close"),modifier=androidx.compose.ui.Modifier.focusRequester(focus)) {state.markNoticesRead(UpdateNotes.id);close()}
        }
    }
}
