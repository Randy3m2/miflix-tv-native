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
    val rc59Changes=listOf(
        "Friends & Party del player: panel compacto, actualización automática al abrir, sin Refresh ni Calendar." to "Player Friends & Party: compact panel, automatic refresh on opening, no Refresh or Calendar buttons.",
        "Carga de reproducción con imagen del episodio/póster hasta el primer fotograma, sin mensajes intermedios." to "Episode artwork/poster stays visible until the first video frame, with no intermediate loading messages.",
        "Tiempo de destino visible al adelantar, retroceder o arrastrar; el arrastre se aplica al soltar." to "Live seek timestamp while moving forward/backward or dragging; drag seeks commit on release.",
        "Portadas completas en recomendaciones y My List en la navegación superior, junto a Live TV." to "Full recommendation posters and My List in the top navigation beside Live TV.",
        "Episodio aleatorio desde la ficha de una serie o el selector de episodios; continúa en modo aleatorio con Autoplay." to "Random episode from series details or the episode picker; continues in shuffle mode with Autoplay.",
        "My List: Last Added y Oldest Added, con paginación de 25. Last Added también en la web." to "My List: Last Added and Oldest Added with 25-title paging. Last Added is also available on the web.",
        "Connect phone / PC abre tu catálogo de Vercel conectado a la TV: QR/enlace sin código manual y botón Play on TV." to "Connect phone / PC opens your Vercel catalog connected to the TV: QR/link with no manual code and a Play on TV button."
    )
    val changes=listOf(
        "Selección global entre todos los add-ons: español latino/castellano/España primero; inglés como respaldo, antes de priorizar resolución o tamaño." to "Global selection across all add-ons: Latin American/Castilian/Spain Spanish first, English fallback, before resolution or file size.",
        "Mejor lectura de etiquetas LAT/España, descripciones y audio declarado; VOSE/subtítulos no cuentan como audio español." to "Better LAT/Spain labels, descriptions and declared-audio handling; VOSE/subtitles do not count as Spanish audio.",
        "Home: romance, dramas coreanos y clásicos; Para ti combina historial y puntuaciones con caché local." to "Home: romance, K-dramas and classics; For you combines viewing history and ratings with local caching.",
        "Modo sencillo opcional por perfil: texto mayor, títulos visibles, menos filas y accesos claros en Inicio." to "Optional per-profile Simple Home: larger text, visible titles, fewer rows and clear shortcuts.",
        "Motor nativo: búsqueda adicional por título traducido, latino y castellano; español primero e inglés de respaldo." to "Native engine: extra searches by translated title, Latino and Castilian releases; Spanish first with English fallback.",
        "Presencia social por eventos: inicio, cambio, salida y fin del contenido, sin heartbeat. Cola local y vencimiento para cierres inesperados; Watch Party conserva su sincronización." to "Event-driven social presence: start, change, exit and playback end, with no heartbeat. Durable outbox and crash expiry; Watch Party synchronization stays unchanged.",
        "Sports → LIDOM: seis equipos, logos, favoritos, calendario directo, fuentes por equipo, YouTube oficial y URLs HLS/DASH configurables." to "Sports → LIDOM: six teams, logos, favorites, direct schedule, team sources, official YouTube player and configurable HLS/DASH URLs.",
        "Amigos visibles debajo de Create party y Refresh requests en el panel del reproductor, con estado, actividad y acciones." to "Friends visible below Create party and Refresh requests in the player panel, with status, activity and actions.",
        "En Home, pulsa Atrás dos veces para cerrar la app; aviso breve en la primera pulsación." to "On Home, press Back twice to close the app; a brief prompt appears on the first press.",
        "Return to player desde el party al explorar; pausa automática y posición guardada al salir del reproductor." to "Return to player from the party while browsing; automatic pause and saved position when leaving the player.",
        "Mantén pulsada una temporada para marcar sus episodios estrenados como vistos, desde un menú pequeño." to "Long-press a season to mark its released episodes as watched from a compact menu.",
        "Player nativo estándar para canales; diagnóstico local de errores internos. Fondo de Home detrás del menú, pósteres limpios y expansión sin mover las tarjetas vecinas." to "Standard native player for channels; local runtime error details. Home artwork behind navigation, clean posters and expansion without shifting neighboring cards.",
        "Regreso al party, sincronización más precisa, tarjetas redondeadas, transición superior suave y control de repeticiones del mando." to "Return to party, tighter playback synchronization, rounded cards, blended header and steadier held remote navigation.",
        "Compatibilidad HTTP de canales en el reproductor y multiview; errores HTTP más claros." to "Live HTTP compatibility in the player and multiview; clearer HTTP errors.",
        "Avisos breves de pausa/reanudación junto a los avatares: nombre del miembro, sin avisar por buffering ni sincronización automática." to "Brief pause/resume notices beside the avatars: member name, with no notices for buffering or automatic synchronization.",
        "Participantes del party en avatares compactos, visibles solo con los controles del player." to "Compact party member avatars, visible only with player controls.",
        "Panel lateral de Friends & Party con código, QR, miembros e invitaciones a amigos." to "Friends & Party side panel with code, QR, members and friend invitations.",
        "Share my stream opcional, apagado por defecto y limitado a miembros admitidos." to "Optional Share my stream, off by default and restricted to admitted members.",
        "Fuente del host visible para invitados e identificada en Playback links." to "Host source visible to guests and identified in Playback links.",
        "Switch party to the current title y Watch with the party desde el reproductor." to "Switch party to the current title and Watch with the party from the player.",
        "Control exclusivo del host opcional; los invitados pueden pausar o buscar localmente y volver al party." to "Optional host-only controls; guests can pause or seek locally and rejoin the party.",
        "Información de reproducción transparente sobre el video, con archivo, tamaño, video, audio y subtítulos." to "Transparent playback information over the video, with file, size, video, audio and subtitle details.",
        "Sinopsis, fondo y logo en una zona fija superior al enfocar títulos; expansión lateral después de 3 segundos." to "Synopsis, backdrop and logo in a fixed upper area as titles gain focus; sideways expansion after 3 seconds.",
        "Navegación rápida con repeticiones verticales reguladas y menos cambios de layout por fila." to "Fast navigation with coalesced vertical repeats and fewer per-row layout changes.",
        "Se conservan solicitudes manuales y avatar cambiado como máximo cada 4 minutos." to "Manual requests and the 4-minute changed-avatar limit are preserved."
    )
    val rc62Changes=listOf(
        "Solicitudes de acceso a Party: consulta manual con Refresh requests en Friends y en el reproductor." to "Party access requests: manual lookup with Refresh requests in Friends and in the player.",
        "Presencia cada 5 minutos; avatar cambiado como máximo cada 4 minutos, sin escrituras si no cambió." to "Presence every 5 minutes; changed avatar at most every 4 minutes, with no writes if unchanged.",
        "Auriculares: salida PCM con mezcla estéreo real; reinicio de audio al conectar/desconectar conservando posición y pausa." to "Headphones: PCM output with real stereo mixing; audio restarts on connection changes while preserving position and pause.",
        "Recuperación automática limitada para fallos AudioTrack al usar auriculares." to "Bounded automatic recovery for AudioTrack errors with headphones.",
        "Auriculares en la TV: detección de salida, preferencia estéreo y gestión de foco de audio; sincronización mediante Media3." to "TV headphones: output detection, stereo preference and audio focus handling; synchronization through Media3.",
        "Conexión local por Wi-Fi o Ethernet en la misma subred, sin códigos manuales." to "Local connection over Wi-Fi or Ethernet on the same subnet, without manual codes.",
        "Now Playing en la web con estado y tiempo de la TV, consultado por la red local." to "Web Now Playing with TV status and playback time, fetched over the local network.",
        "Fichas web con imagen superior, sinopsis, Play on TV, My List, recomendar, compartir y eventos." to "Web title profiles with hero artwork, synopsis, Play on TV, My List, recommend, share and events.",
        "Enlaces cortos para compartir títulos y Recientes del historial sincronizado del perfil, de 25 en 25." to "Short title sharing links and Recents from the profile’s synced history, paged in groups of 25."
    )
    val previousChanges=rc62Changes+rc59Changes+listOf(
        "Menú de amigos: Watch, View Details, Recommendations y Close; detalles abre la ficha sin iniciar reproducción." to "Friend menu: Watch, View Details, Recommendations and Close; details opens the title without starting playback.",
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
                item {androidx.tv.material3.Text("RC57–RC62",color=Accent,fontSize=20.sp)}
                items(UpdateNotes.previousChanges.size) {index -> val change=UpdateNotes.previousChanges[index];androidx.tv.material3.Text("• "+tr(change.first,change.second),color=Muted,fontSize=15.sp,modifier=androidx.compose.ui.Modifier.fillMaxWidth().contentFocusBorder().focusable().padding(10.dp)) }
            }
            FocusButton(tr("Cerrar","Close"),modifier=androidx.compose.ui.Modifier.focusRequester(focus)) {state.markNoticesRead(UpdateNotes.id);close()}
        }
    }
}
