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
    val rp131Changes=listOf(
        "RP131: estadísticas F1 en SplitScreen con posiciones, vuelta publicada y selector de sesión; mismo diseño glass de RP130." to "RP131: F1 SplitScreen stats with positions, published lap and session picker; same RP130 glass design.",
        "ESPN: actualización automática, sesiones Final/Programada diferenciadas y campos no publicados como —; selección F1 manual desde las opciones del canal." to "ESPN: automatic updates, distinct Final/Scheduled sessions and unavailable fields shown as —; manual F1 mode from channel options."
    )
    val rp130Changes=listOf(
        "RP130: Stats glass por pantalla en SplitScreen de dos videos; con tres, el cuarto cuadro muestra estadísticas del juego elegido." to "RP130: per-screen glass Stats for two videos; with three, the fourth cell shows stats for the selected game.",
        "NBA, MLB y LIDOM: selección manual/automática y actualizaciones mientras Stats está abierto; botón SplitScreen en el pie, sin tapar datos." to "NBA, MLB and LIDOM: manual/automatic selection and updates while Stats is open; SplitScreen button in the footer without covering data."
    )
    val rp129Changes=listOf(
        "RP129: Seguir viendo se publica primero y conserva su lugar al iniciar Home, sin tener que bajar para encontrarlo." to "RP129: Continue Watching publishes early and retains its place at Home startup without scrolling to find it.",
        "Ajustes: sin espaciadores duplicados y con recuperación del encabezado al enfocar los primeros controles." to "Settings: removed duplicate spacers and reveal clipped headings when early controls receive focus.",
        "QR locales: detección Ethernet/Wi-Fi y compatibilidad con clientes de otras subredes privadas comunicadas por el router." to "Local QR: Ethernet/Wi-Fi detection and support for routed private-network clients."
    )
    val rc59Changes=listOf(
        "RP123: calendario NBA por mes, logo oficial de F1 y LIDOM Games directo a canales de equipos." to "RP123: monthly NBA calendar, official F1 logo and LIDOM Games opens team channels directly.",
        "RP122: calendario con múltiples suscripciones, flechas juntas y días centrados; tabla de posiciones compacta." to "RP122: multi-subscription calendar, adjacent arrows and centered weekdays; compact standings table.",
        "RP121: Games, Table, Stats y Players para NBA, MLB, F1 y LIDOM; líderes y jugadores con fotos, filtros y caché local." to "RP121: Games, Table, Stats and Players for NBA, MLB, F1 and LIDOM; leaders and players with photos, filters and local caching.",
        "RP120: suscripciones por equipo/liga, calendario con logos y avisos; posiciones NBA, MLB, F1 y LIDOM en Others." to "RP120: team/league subscriptions, calendar logos and alerts; NBA, MLB, F1 and LIDOM standings in Others.",
        "RP119: OK restaura categoría, búsqueda, posición y foco del canal en reproducción." to "RP119: OK restores category, search, position and focus on the playing channel.",
        "RP118: sin importar M3U en TV Channels ni panel; tarjetas enfocadas completas y más espacio para canales." to "RP118: no M3U import in TV Channels or browser; fully revealed focused cards and more channel space.",
        "RP117: búsqueda sin campo duplicado arriba; consulta solo a la derecha y teclado más alto." to "RP117: removed duplicate top search field; query only at right and keyboard moved upward.",
        "RP116: búsqueda desde 2 letras, resultados completos con spinner y teclado simplificado con micrófono arriba." to "RP116: search from 2 letters, complete result batches with spinner and simplified keyboard with microphone above.",
        "RP115: buscador glass más ligero, teclado con foco rectangular y búsquedas agrupadas cada 3 caracteres." to "RP115: lighter glass search, rectangular keyboard focus and searches batched every 3 characters.",
        "RP114: tarjetas glass con imagen del evento y marcador Viendo ahora en el selector de juegos." to "RP114: glass event cards with artwork and Now playing marker in the game picker.",
        "RP113: selector de juegos de todos los deportes en el reproductor y cambio arriba/abajo con enlaces automáticos." to "RP113: all-sports game picker in the player and up/down game switching with automatic sources.",
        "RP112: opciones de canal al cumplir 1.75 segundos manteniendo OK, sin esperar a soltar." to "RP112: channel options open at 1.75 seconds while holding OK, without waiting for release.",
        "RP111: eliminado el botón de importar listas M3U del selector de canales; actualización de la lista principal desde Ajustes." to "RP111: removed M3U playlist import from the channel browser; main playlist refresh remains in Settings.",
        "RP108: seis tarjetas grandes en Live, reintento deportivo único, botones glass y foco corregido en navegación/Stats." to "RP108: six large Live tiles, one sports retry, glass buttons and corrected navigation/Stats focus.",
        "RP107: Dragon Ball Z envía siempre temporada 1 y episodio absoluto a las fuentes, también en Modo fácil y precarga." to "RP107: Dragon Ball Z always sends season 1 and absolute episode numbers to sources, including Easy mode and prewarm.",
        "RP106: Stats con foco persistente y scroll estable; menú centrado con efecto glass, engranaje de ajustes y Seguir viendo primero." to "RP106: persistent Stats focus and stable scrolling; centered glass navigation, settings gear and Continue Watching first.",
        "RP105: Stats NBA/MLB como tabla compacta, cabecera fija, navegación por todas las filas actualización cada 10 s y sin robo de foco del temporizador del video." to "RP105: compact NBA/MLB box-score tables, fixed headers, complete row navigation 10-second updates and no focus stealing from the video timer.",
        "RP104: selector de canales con 10 entradas iniciales y tandas de 10." to "RP104: channel picker starts with 10 entries and publishes batches of 10.",
        "RP103: Naruto Shippuden también muestra sus 500 episodios en una lista continua, conservando el progreso." to "RP103: Naruto Shippuden also shows its 500 episodes in one continuous list while preserving progress.",
        "RP102: Dragon Ball Z y Naruto original con lista continua de episodios; conserva progreso y busca numeración absoluta si faltan enlaces." to "RP102: continuous episode lists for Dragon Ball Z and original Naruto; preserves progress and tries absolute numbering when links are missing.",
        "RP101: el selector de canales siempre abre en Favoritos, sin cambiar el canal que se reproduce." to "RP101: the channel picker always opens Favorites without changing the playing channel.",
        "RP100: selector de canales con categorías preparadas en segundo plano, caché por perfil y logos ligeros." to "RP100: channel picker with background category indexing, profile-scoped cache and lightweight logos.",
        "RP99: corrige el filtro de grabaciones cuando aún no hay una categoría guardada." to "RP99: fixes the recordings filter when no category has been saved yet.",
        "RP98: Live TV inicia con Noticias (7 canales), o la categoría más pequeña si cambia la lista; conserva el contexto del canal al volver." to "RP98: Live TV initially opens News (7 channels), or the smallest group after playlist changes; preserves channel context when returning.",
        "RP97: lista definitiva de canales con categorías/logos, actualización M3U local por URL/QR, canales LIDOM por nombre y flechas laterales para mostrar controles." to "RP97: final channel catalog/categories/artwork, local M3U updates by URL/QR, LIDOM channel name matching and left/right revealing controls.",
        "RP96: píldora de amigos más pequeña y transparente, lista compacta con avatares y precarga desde 250 ms con hasta 15 segundos de búfer." to "RP96: smaller translucent watched-friends pill, compact avatar list, and prewarm from 250 ms with up to 15 seconds buffered.",
        "RP95: estadísticas NBA y MLB en un panel lateral del reproductor, selector de partido y equipo, y vídeo visible a la izquierda." to "RP95: NBA and MLB stats in a player sidebar, game and team selectors, and video remaining visible on the left.",
        "RP94: pruebas de precarga compatibles con los nombres de hilo decorados por coroutines en CI." to "RP94: warm-construction tests support coroutine-decorated thread names in CI.",
        "RP93: avatares de amigos que ya vieron el contenido en Details, caché de 15 minutos y privacidad por perfil." to "RP93: friends who watched avatars in Details, 15-minute cache and per-profile sharing privacy.",
        "RP92: construcción de la precarga fuera del hilo de interfaz y lectura ligera de ajustes de pistas para reducir las pausas en detalles; hasta cinco fuentes de precarga con reintentos silenciosos." to "RP92: builds the warm player off the UI thread and captures lightweight track-setting snapshots to reduce details-screen stalls; up to five warm sources with silent retries.",
        "RP91: preparación real de vídeo tras 1 segundo: una fuente, búfer limitado y reutilización del reproductor al pulsar Play, sin sonido ni presencia anticipada." to "RP91: real video preparation after 1 second: one source, bounded buffering and player handoff on Play, without sound or early presence.",
        "RP90: precarga opcional por perfil desde Settings → Reproducción en los detalles del modo normal; Play reutiliza la búsqueda y la caché, sin reproducir vídeo ni publicar presencia antes de tiempo." to "RP90: optional per-profile source prefetch in normal-mode details from Settings → Playback; Play reuses lookup/cache without starting video or publishing presence early.",
        "RP89: barra para adelantar/retroceder en Modo fácil, siguiente episodio al terminar con enfoque directo y selección/búsqueda de subtítulos en español." to "RP89: Easy mode seek bar, focused next-episode action at the end, and Spanish subtitle selection/lookup.",
        "RP88: update unificado: regreso de amigos al party, protección al cambiar fuentes rápidamente y foco de categorías a canales en Modo fácil; incluye subtítulos en español de RP87." to "RP88: unified update: bring friends back to the party, rapid source-switch protection and category-to-channel focus in Easy mode; includes RP87 Spanish subtitles.",
        "RP87: subtítulos en español por defecto en Modo fácil, separados por perfil y de las preferencias del modo normal." to "RP87: Spanish subtitles by default in Easy mode, separated by profile and from normal-mode preferences.",
        "RP86: corrige el import de launch en detalles de Modo fácil para el desplazamiento de la cabecera." to "RP86: fixes the launch import in Easy details used to restore the header scroll position.",
        "RP85: Live abre el hub y solo la tarjeta Live TV inicia un canal; detalles de Modo fácil sin cabecera recortada, episodios horizontales con Visto, teclado ES/EN sin sugerencias inferiores y fila Películas de La Vega." to "RP85: Live opens the hub and only the Live TV tile starts a channel; Easy details restore the full header, horizontal episode cards with Watched, an ES/EN keyboard without lower suggestions, and La Vega movies.",
        "RP84: nueva fuente Live Sports de fábrica y enlaces ordenados por resolución descendente, luego bitrate; conserva el orden recibido cuando faltan datos." to "RP84: updated default Live Sports source and links ordered by descending resolution, then bitrate; preserves provider order when metadata is missing.",
        "RP83: salida de Modo fácil en 15 s con cuenta regresiva alternativa; cinco filas; continuación y episodios vistos; español verificado opcional; respaldo por cuenta y perfil; Live TV inicia con panel lateral." to "RP83: 15-second Easy mode exit with alternate countdown; five rows; episode resume and watched marks; optional verified Spanish; account/profile settings backup; Live TV starts with its channel panel.",
        "RP82: Modo fácil muestra carátula y sinopsis antes de reproducir, también desde Seguir viendo. No muestra ni guarda historial de búsqueda en ese modo." to "RP82: Easy mode shows poster and synopsis before playback, including Continue Watching. Search history is hidden and not recorded in this mode.",
        "RP81: búsqueda por actores/directores y títulos en español o inglés en ambos modos; dictado latino con selector ES/EN por perfil y transcripciones alternativas." to "RP81: actor/director and Spanish/English title search in both modes; Latin Spanish voice input with profile-scoped ES/EN selection and alternate transcriptions.",
        "RP80: Modo fácil con buscador original ampliado, 26 filas más Seguir viendo, MLB con el add-on de deportes y salida manteniendo BruniO 60 segundos." to "RP80: Easy mode restores larger original search, 26 rows plus Continue Watching, MLB through the sports add-on and a 60-second BruniO hold to exit.",
        "RP79: Modo fácil por perfil con Home, Buscar, Mi lista y Live TV; texto grande, controles básicos y tareas sociales desactivadas." to "RP79: per-profile Easy mode with Home, Search, My List and Live TV; large text, basic controls and social background work disabled.",
        "RP78: Korean/한국어 en audio principal, secundario y subtítulos; prioridad coreana en el motor nativo y enlaces de add-ons; subtítulos preferidos por perfil." to "RP78: Korean/한국어 in primary/secondary audio and subtitles; Korean priority for the native engine and add-on sources; profile-scoped subtitle preferences.",
        "RP77: motor TorBox con prioridad latino según Playback audio, filtro opcional Solo español y tamaño ascendente/descendente dentro de cada calidad; preferencias de audio y filtros por perfil." to "RP77: TorBox engine prioritizes Latino based on Playback audio, with optional Spanish-only filtering and ascending/descending size within each quality; audio and filter preferences are profile-scoped.",
        "RP76: TorBox experimental y opcional solo en Settings, sin configuración al inicio; QR local para pegar la API key sin activar el motor automáticamente." to "RP76: experimental optional TorBox in Settings only, without startup setup; local QR pastes the API key without automatically enabling the engine.",
        "RP75: configuración inicial de TorBox; modo Solo motor nativo para buscar VOD sin consultar otros add-ons, con clave cifrada en esta TV." to "RP75: initial TorBox setup; Native engine only mode finds VOD without querying other add-ons, with an encrypted key on this TV.",
        "RP74: aviso de error de canal sin bloquear la navegación; panel de canales con catálogo local en segundo plano y búsqueda/favoritos optimizados." to "RP74: channel error notice keeps navigation available; channel panel loads the local catalog in the background and caches search/favorites work.",
        "RP73: 477 entradas adicionales integradas en Tirso TV, conservando el catálogo anterior y sus enlaces." to "RP73: 477 additional entries integrated into Tirso TV, preserving the existing catalog and links.",
        "RP72: Sport Recaps y grabaciones deportivas con pausa, barra de progreso y saltos de diez segundos, sin cambiar el control de los directos." to "RP72: Sport Recaps and sports recordings get pause, a timeline and ten-second skips while live channel controls stay separate.",
        "RP71: menú Seguir viendo renovado; Nauta Source en Others; Nuvio Test retirado; deportes con QR, fuente premium predeterminada y hasta cinco intentos automáticos." to "RP71: refreshed Continue Watching menu; Nauta Source folder in Others; Nuvio Test removed; sports QR, default premium source and up to five automatic attempts.",
        "RP70: evita bloquear el foco al entrar en filas diferidas del Home; cierre de Más opciones con una X sin texto." to "RP70: avoids blocking focus when entering lazy Home rows; More options closes with an icon-only X.",
        "RP69: corrige el import de Text en Más opciones para usar TV Material, la dependencia existente del proyecto." to "RP69: fixes the More options Text import to use the project's existing TV Material dependency.",
        "RP68: menú Más opciones de detalles renovado, negro, con portada, grupos de acciones e iconos; solo la acción enfocada se rellena de blanco." to "RP68: redesigned black details More options panel with artwork, action groups and icons; only the focused action is filled white.",
        "RP67: Split Screen comparte el primer canal en la actividad de amigos; navegación vertical restaura la tarjeta elegida por fila sin enfocar tarjetas vecinas transitorias." to "RP67: Split Screen shares the first channel in friend activity; vertical navigation restores each row's chosen card without transient neighbouring-card focus.",
        "RP66: corrige compilación de My List al usar una animación local de foco de 80 ms en lugar de FastMotion privado." to "RP66: fixes My List compilation by using a local 80 ms focus animation instead of the private FastMotion value.",
        "RP65: My List con siete pósteres por fila, imagen completa en la tarjeta y título sobre degradado como Home; conserva carga de 15 y Cargar 15 más." to "RP65: My List with seven poster cards per row, edge-to-edge artwork and titles over a Home-style gradient; keeps 15 initial titles and Load 15 more.",
        "RP64: botones de acción blancos solo al tener foco, sin cambiar la barra superior; My List con cinco tarjetas por fila, 15 títulos iniciales y Cargar 15 más; márgenes reducidos y títulos visibles al enfocar toda la tarjeta." to "RP64: white action buttons only while focused, with the top bar unchanged; My List with five cards per row, 15 initial titles and Load 15 more; reduced margins and titles included in card focus.",
        "RP63: My List con 4 tarjetas por fila y carga de 16; perfiles y paneles sociales renovados; Request Access desde amigos; fondos Top 10 al administrar y crear perfiles; hora de finalización en formato de 12 horas." to "RP63: My List with four cards per row and batches of 16; refreshed profiles and social panels; Request Access from friends; Top 10 backgrounds when managing and creating profiles; 12-hour playback end time.",
        "RP62: Mandanguita TV de fábrica para VOD: primera con audio español y segunda en otros idiomas. Idioma secundario por perfil. P2P retirado y navegación vertical un poco más rápida." to "RP62: bundled Mandanguita TV for VOD: first for Spanish audio, second for other languages. Per-profile secondary language. P2P removed and slightly faster vertical browsing.",
        "RP12: búsqueda inicial más ágil en español e inglés; 7 enlaces visibles por add-on y Load more. Fábrica: Peerflix, Addon Latam y Torrentio free." to "RP12: faster initial Spanish/English lookup; 7 visible links per add-on and Load more. Defaults: Peerflix, Addon Latam and free Torrentio.",
        "RP11: PIN Live score NBA, MLB y LIDOM; píldora con logos, movible con mando o arrastre, por perfil y sin Supabase." to "RP11: NBA, MLB and LIDOM live score pin; team-logo pill, movable by remote or drag, per-profile with no Supabase requests.",
        "RP10: Continue Watching desde los 5 segundos, con actualización local inmediata de la fila." to "RP10: Continue Watching from five seconds, with an immediate local shelf update.",
        "RP9: SplitScreen para LIDOM con hasta cuatro señales, fuentes por equipo, YouTube y audio seleccionable." to "RP9: LIDOM SplitScreen with up to four broadcasts, team source selection, YouTube and selectable audio.",
        "RP8: Addon Latam y Nuve+ conservan sus enlaces sin tamaño aunque haya límite de GB; tamaños conocidos respetan el límite." to "RP8: Addon Latam and Nuve+ unknown-size links remain visible under a GB cap; known sizes still respect the cap.",
        "RP7: límites de enlaces por add-on, diagnóstico de consultas y filtros, y headers de reproducción de proveedores." to "RP7: per-add-on link limits, request/filter diagnostics and provider playback headers.",
        "RP6: audio latino primero, España después y etiquetas castellano a continuación; todo el español antes de inglés." to "RP6: Latin American audio first, Spain next, then Castilian labels; all Spanish before English.",
        "RP5: español latinoamericano por perfil; títulos y sinopsis en es-MX, con imágenes disponibles en el catálogo." to "RP5: per-profile Latin American Spanish; es-MX titles and synopses with available catalog artwork.",
        "RP4: cuatro add-ons incluidos por defecto, eliminables individualmente desde Settings." to "RP4: four default add-ons, individually removable from Settings.",
        "RP3: add-on MG incluido por defecto y eliminable desde Settings; recuerda la eliminación en esta TV." to "RP3: MG add-on included by default and removable from Settings; removal is remembered on this TV.",
        "RP2: prioridad de add-ons por idioma, orden personalizable por perfil y audio; información de reproducción y detalles de enlace a pantalla completa sobre el vídeo." to "RP2: language-based add-on priority, editable order per profile and audio language; full-screen playback information and source details over the video.",
        "RP1: selector All o add-on en enlaces; agota las fuentes identificadas en español antes de inglés. Usuario, amigos y Watch Party independientes por perfil; requiere SQL RP1." to "RP1: All or individual add-on link filter; exhausts identified Spanish sources before English. Independent per-profile usernames, friends and Watch Party; requires RP1 SQL.",
        "RC105: ajustes nuevos en el negro del tema existente; listas con fondos negros y tarjeta Crear lista compacta para evitar texto cortado." to "RC105: new settings match the existing black theme; black list backgrounds and a compact Create list card to prevent clipped text.",
        "RC104: fila de listas personales con nombre, cantidad y collage de carátulas; acceso directo a cada colección y administrador visual renovado." to "RC104: personal-list shelf with names, counts and poster collages; direct access to each collection and a refreshed visual manager.",
        "RC103: ajustes locales con tarjetas, iconos e interruptores; mejor foco para TV, ayuda a la derecha al enfocar o pasar el cursor y selección visual de filas de Home." to "RC103: local settings with cards, icons and switches; clearer TV focus, right-side help on focus or hover and visual Home row selection.",
        "RC102: selección visual de gustos por perfil, recomendaciones con aprendizaje local, novelas cerca del inicio de Home, moods con fotos y opción de add-ons propios al crear perfiles." to "RC102: visual per-profile taste setup, recommendations with local learning, an early Home telenovela shelf, photo-backed moods and optional independent profile add-ons.",
        "RC101: corrige el choque JVM de stillLimit y añade Canal anterior en la barra de Live TV, con historial por perfil y actualización solo al reproducir." to "RC101: fixes the stillLimit JVM clash and adds Previous channel to the Live TV control bar, with per-profile history updated only after playback starts.",
        "RC100: funciones locales opcionales: temporizador, ¿Sigues viendo?, listas personales, ocultar recomendaciones, filtros persistentes, memoria de pistas por serie, panel técnico y backup JSON. Idioma de interfaz independiente por perfil." to "RC100: optional local features: sleep timer, still-watching prompt, personal lists, recommendation hiding, persistent filters, per-series track memory, technical panel and JSON backup. Independent interface language per profile.",
        "RC99: opción para activar o desactivar el siguiente enlace automático; mostrar u ocultar filas de Home por perfil, conservando su orden." to "RC99: toggle automatic next-link retries; show or hide Home rows per profile while preserving their order.",
        "RC98: Your Mood · Your Picks conserva el ánimo elegido y permite cargar 25 títulos más al final del catálogo, sin duplicados." to "RC98: Your Mood · Your Picks keeps the selected mood and loads 25 more titles at the end of its catalog without duplicates.",
        "RC97: revertida la selección por compatibilidad de códec de audio; conserva prioridad de idioma, calidad y reintentos anteriores." to "RC97: reverted audio-codec compatibility source selection; preserves earlier language priority, quality ordering and retries.",
        "RC96: Dazzle Me sigue Pick your vibe; sin selección mezcla el catálogo. Ventana de tráileres persistente durante los cambios, con controles nativos compactos." to "RC96: Dazzle Me follows Pick your vibe; without a selection it mixes the catalog. Persistent trailer window between titles with compact native controls.",
        "Friends & Party del player: panel compacto, actualización automática al abrir, sin Refresh ni Calendar." to "Player Friends & Party: compact panel, automatic refresh on opening, no Refresh or Calendar buttons.",
        "Carga de reproducción con imagen del episodio/póster hasta el primer fotograma, sin mensajes intermedios." to "Episode artwork/poster stays visible until the first video frame, with no intermediate loading messages.",
        "Tiempo de destino visible al adelantar, retroceder o arrastrar; el arrastre se aplica al soltar." to "Live seek timestamp while moving forward/backward or dragging; drag seeks commit on release.",
        "Portadas completas en recomendaciones y My List en la navegación superior, junto a Live TV." to "Full recommendation posters and My List in the top navigation beside Live TV.",
        "Episodio aleatorio desde la ficha de una serie o el selector de episodios; continúa en modo aleatorio con Autoplay." to "Random episode from series details or the episode picker; continues in shuffle mode with Autoplay.",
        "My List: Last Added y Oldest Added, con paginación de 25. Last Added también en la web." to "My List: Last Added and Oldest Added with 25-title paging. Last Added is also available on the web.",
        "Connect phone / PC abre tu catálogo de Vercel conectado a la TV: QR/enlace sin código manual y botón Play on TV." to "Connect phone / PC opens your Vercel catalog connected to the TV: QR/link with no manual code and a Play on TV button."
    )
    val legacyChanges=listOf(
        "RC95: al bajar a una fila desplazada, Home primero confirma el foco y después vuelve a su primera tarjeta; elimina la cancelación que podía bloquear la entrada. Las transiciones barra superior/contenido cancelan tareas anteriores al invertir la dirección." to "RC95: when entering a horizontally scrolled row, Home commits focus before returning to its first card, removing the cancellation that could block entry. Header/content transitions cancel older work when direction changes.",
        "RC94: paneles laterales sin recortes al subir y turnos de la ruleta al inicio. Controles compactos de tráileres y controles nativos de YouTube desactivados; selecciones y tráileres rotatorios con historial local. Long press uniforme de 750 ms. Audio compatible con los decodificadores de la TV dentro de la prioridad de idioma, verificación de pistas reales y recuperación ante errores de audio." to "RC94: side-panel focus scrolling without top clipping and movie-night turns reset to the top. Compact trailer controls with YouTube playback controls disabled; rotating picks and trailers with local history. Consistent 750 ms long press. TV decoder-compatible audio within language priority, real-track verification and audio-error recovery.",
        "RC93: superficie de vídeo de tráileres a tamaño completo y controles directos del mando; barra Previous/Next/Details/Watch Title. Moods rediseñados y ampliados. Orden de Home por perfil, fuente adaptada al ancho de banda, ruleta local, estadísticas manuales y guías curadas de sagas. Afinación de gustos y búsquedas descriptivas por metadatos. X-Ray con IA queda pendiente." to "RC93: full-size trailer video surface and direct remote shortcuts; Previous/Next/Details/Watch Title bar. Redesigned expanded moods. Per-profile Home ordering, bandwidth-aware source selection, local movie-night roulette, manual stats and curated saga journeys. Metadata-based taste tuning and descriptive search. AI X-Ray remains pending.",
        "RC92: selector de ánimo, sugerencias por hora o temporada y Surprise me. Dazzle me reproduce tráileres de YouTube dentro de BruniO, con navegación horizontal, avances de hasta 60 s y acceso al título completo. Búsqueda por frases de ánimo en español e inglés, con filtros Kids y sin polling social adicional." to "RC92: mood selector, time/season picks and Surprise me. Dazzle me plays YouTube trailers inside BruniO with horizontal navigation, previews up to 60 s and access to the full title. English/Spanish mood-phrase search, Kids filters and no extra social polling.",
        "RC91: Home mantiene un único movimiento vertical continuo al cambiar de fila; evita reiniciar el reloj de animación y reduce correcciones laterales al cerrarse una tarjeta expandida." to "RC91: Home keeps one continuous vertical motion when focus changes rows, avoiding animation-clock restarts and reducing sideways corrections as expanded cards close.",
        "RC90: aceleración gradual al mantener el mando, movimiento vertical de Home con resorte amortiguado y transferencia de velocidad entre filas, sin cola de pulsaciones." to "RC90: progressive held-remote acceleration and damped spring scrolling in Home, carrying velocity between rows without queued key presses.",
        "RC89: navegación sostenida con cadencia estable, enfoque horizontal anclado, desplazamiento vertical más gradual, enfoque suave y logos animados solo al detenerse sobre ellos." to "RC89: steady held-remote navigation, anchored horizontal focus, gentler vertical scrolling, soft focus transitions and streaming logo animations after focus settles.",
        "RC88: nuevas tarjetas Load more y Shared moments; momentos recibidos con avatar, portada disponible y tiempos claros. Inglés confirmado primero en selección automática y reintentos; búsqueda más tolerante a errores y variantes limitadas." to "RC88: refreshed Load more and Shared moments cards; received moments with avatars, available artwork and clear timing. Identified English audio first for automatic selection/retries; more typo-tolerant search with bounded fallback queries.",
        "RC87: hasta diez fuentes automáticas, recuperación ante carga inicial o buffering prolongado; detecta errores tempranos y limita la preparación de cada fuente. Christmas Movies cerca del inicio de Collections, ordenadas por valoración y con Load more de 25. Home: acceso al catálogo completo al final de cada fila, con paginación de 25." to "RC87: up to ten automatic sources, recovery from startup or prolonged buffering, early-error detection and bounded source preparation. Best-rated Christmas Movies near the top of Collections with Load more of 25. Home: full-catalog access at the end of each row, with 25-title paging.",
        "RC86: Playback links identifica el enlace actual con REPRODUCIENDO, mantiene su resaltado y abre la lista en esa fuente." to "RC86: Playback links labels the current source NOW PLAYING, keeps it highlighted and opens the list at that source.",
        "RC85: si falla una fuente, prueba automáticamente el siguiente enlace también en inglés; aviso durante la carga y posición conservada. Hasta cinco intentos con la lista cargada." to "RC85: failed sources automatically advance to the next link in English too; loading notice and playback position preserved. Up to five attempts from the loaded list.",
        "RC84: corrige el fallo AudioProcessor/flush que impedía iniciar películas y episodios; conserva la mezcla de audio para auriculares." to "RC84: fixes the AudioProcessor/flush failure preventing movies and episodes from starting; preserves headphone audio mixing.",
        "RC83: al volver de los detalles, Home y Collections recuperan la fila, su desplazamiento horizontal y la tarjeta enfocada, sin regresar al inicio." to "RC83: returning from details restores the Home/Collections row, horizontal scroll and focused card instead of jumping to the top.",
        "RC82: desplazamiento horizontal restaurado en Streaming y Browse by genre de Home, conservando encabezados y tarjetas completos." to "RC82: restored horizontal scrolling in Home Streaming and Browse by genre, preserving full headings and cards.",
        "Home: cabecera compacta y filas completas al navegar arriba/abajo; las tarjetas expandidas empujan a las siguientes; Continue Watching uniforme con título dentro; logos sin nombre repetido." to "Home: compact hero and complete shelves when navigating up/down; expanded cards push neighbors; uniform Continue Watching covers with titles inside; streaming logos without duplicate captions.",
        "Etiquetas globales con fechas reales y ventana de 14 días; Navidad visible con respaldo y reintento; premios 2026 y colección Oscar 2020–2026 por ceremonia." to "Global tags based on real dates over 14 days; visible Christmas row with fallback and retry; 2026 awards and an Oscar collection by ceremony year, 2020–2026.",
        "Géneros: imágenes distintas por título y nombre integrado dentro de la tarjeta, sobre un degradado oscuro." to "Genres: distinct title artwork and genre names integrated inside the cards over a dark gradient.",
        "Home: Christmas Movies debajo de Horror y Because you watched con sugerencias del último título visto; caché y separación por perfil." to "Home: Christmas Movies below Horror and Because you watched recommendations from your latest watched title; cached and scoped to your profile.",
        "Home: Ojalá olvidarlas para verlas otra vez, con películas y series de puntuación 8 o mayor, reutilizando el catálogo cargado." to "Home: Make me forget so I can rewatch, mixing movies and series rated 8 or above from the loaded catalog.",
        "Carátulas: etiquetas compactas Recién agregado y Nuevo episodio; primera carga sin falsos nuevos, fechas en caché y sin consultas a Supabase." to "Covers: compact Just Added and New Episode tags; first-load baseline, cached dates and no Supabase queries.",
        "Top 10: puesto integrado dentro de la carátula; nuevas filas Oscar y Emmy con etiquetas verificadas y año de ceremonia." to "Top 10: rank integrated into cover artwork; new Oscar and Emmy rows with verified labels and ceremony year.",
        "LIDOM: fuentes combinadas de IPTV-org y Nauta por canal, conservando YouTube; Toros con Coral 39 y Teleantillas, Gigantes con Telemedios y Estrellas con Teleantillas." to "LIDOM: combined IPTV-org and Nauta sources per channel, keeping YouTube; Toros with Coral 39 and Teleantillas, Gigantes with Telemedios and Estrellas with Teleantillas.",
        "LIDOM: calendario con logos de ambos equipos, hora dominicana y transmisiones anunciadas; canal por confirmar cuando no se publica." to "LIDOM: schedule with both team logos, Dominican start times and listed broadcasts; unannounced channels show To be confirmed.",
        "Menús del reproductor en paneles a la derecha: enlaces, audio, subtítulos, información, episodios, cola, chat y momentos; fondo visible y entrada breve." to "Right-side player panels: links, audio, subtitles, info, episodes, queue, chat and moments; visible video and a short entrance animation.",
        "Compartir momentos muestra tarjetas de amigos con avatar y nombre, reutilizando la información del perfil social." to "Moment sharing shows friend cards with avatars and names, reusing social profile data.",
        "Reproducción automática: hasta tres fuentes españolas antes de recurrir a inglés; cinco intentos totales, conservando el minuto y usando la lista ya cargada." to "Automatic playback: up to three Spanish sources before English fallback; five total attempts, preserving position and reusing loaded links.",
        "Recuperación limitada ante errores de fuente o carga inicial sin video durante 20 segundos; sin cambiar Live TV ni la sincronización del Party." to "Bounded recovery from source errors or 20 seconds without an initial video frame; Live TV and Party synchronization stay unchanged.",
        "Momentos compartidos: marca inicio y final en el reproductor y envíalos a un amigo, sin subir video." to "Shared moments: mark start/end in the player and share with a friend without uploading video.",
        "Friends → Momentos: recepción exclusivamente con Refresh manual; reproduce desde el inicio marcado y pausa al final, con Repetir o Continuar." to "Friends → Moments: manual Refresh only; plays from the marked start and pauses at the end, with Replay or Continue.",
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
    val rp124Changes=listOf(
        "RP124: las notificaciones de actualización muestran las novedades de la versión instalada." to "RP124: update notifications show release notes for the installed version.",
        "El historial de versiones anteriores queda separado y muestra primero las revisiones RP más recientes." to "Earlier release history is separate and lists the latest RP revisions first."
    )
    val rp125Changes=listOf(
        "RP125: Table sin selector de temporada ni Refresh, con más espacio para las posiciones." to "RP125: Table removes season controls and Refresh, leaving more room for standings.",
        "Games sin selector de fecha: muestra lo publicado por SS. Stats y Players sin Refresh." to "Games shows published SS events without a date selector. Stats and Players remove Refresh."
    )
    val rp126Changes=listOf(
        "RP126: nuevo administrador de Add-ons en Ajustes, con logos y tarjetas glass." to "RP126: new Add-ons manager in Settings with artwork and glass cards.",
        "Agregar con manifest.json validado, límites por fuente, prioridades y eliminación; conserva la configuración por perfil." to "Install validated manifest.json links, manage per-source limits, priorities and removal; preserves profile configuration."
    )
    val rp127Changes=listOf(
        "RP127: ajustes con mayor separación entre controles y filas que se adaptan al espacio disponible." to "RP127: roomier settings controls and button rows that adapt to available space.",
        "Pantalla de actualizaciones renovada: versión, estado, descarga y detalles de instalación organizados con diseño glass." to "Refreshed updates screen: version, status, download and installation details organized in a glass layout."
    )
    val rp128Changes=listOf(
        "RP128: instala add-ons enviando su manifest.json desde el celular con un QR local y confirmación en la TV." to "RP128: install add-ons by sending manifest.json from your phone using a local QR and TV confirmation.",
        "Corregida la compatibilidad del parser JSON de add-ons con Android." to "Fixed add-on JSON parser compatibility with Android."
    )
    val rp132Changes=listOf(
        "RP132: selectores y marcadores de equipos con logos, sin nombres visibles; F1 con fotos reales de pilotos." to "RP132: logo-only team selectors and scoreboards; F1 with real driver portraits.",
        "F1 reutiliza la tabla de Stats normal: cabecera fija, filas compactas y navegación estable, también con dos pantallas." to "F1 reuses the normal Stats table: fixed header, compact rows and stable navigation, also with two screens."
    )
    val rp133Changes=listOf(
        "RP133: BruniO Mobile M1: nueva interfaz táctil para Android con Home, búsqueda, detalles, perfiles y ajustes glass." to "RP133: BruniO Mobile M1: new Android touch interface with Home, search, details, profiles and glass settings.",
        "Live TV, deportes, calendario, amigos, reproducción y SplitScreen adaptados al celular; instalación y actualizaciones separadas de TV." to "Live TV, sports, calendar, friends, playback and SplitScreen adapted for phones; installation and updates separate from TV."
    )
    val rp134Changes=listOf(
        "RP134: Mobile M2 — reproducción horizontal automática, pantalla completa y controles táctiles compactos." to "RP134: Mobile M2 — automatic landscape playback, full screen and compact touch controls.",
        "Descargas en el celular con portadas, temporadas, progreso y reproducción local; límite de tamaño independiente de TV." to "Phone downloads with covers, seasons, progress and local playback; size limit independent from TV.",
        "AMC+ usa su logo local. SplitScreen añade superficie de video móvil, respuesta al toque y reintento de señal." to "AMC+ uses its bundled logo. SplitScreen adds a mobile video surface, touch response and stream retry.",
        "Calendario móvil con eventos compartidos de TV y sincronización de suscripciones por cuenta y perfil; requiere SQL RP134 y ambas apps actualizadas." to "Mobile calendar includes shared TV events and subscription sync per account/profile; requires RP134 SQL and both apps updated."
    )
    val rp135Changes=listOf(
        "RP135: compatibilidad de fechas y APIs de Java en Fire TV antiguos al abrir deportes, también en modo fácil." to "RP135: date and Java API compatibility on older Fire TV devices when opening sports, including Easy mode.",
        "Diagnóstico local del último cierre al volver a abrir la app; no envía datos a ningún servidor." to "Local last-crash diagnostics on the next launch; no data is sent to a server."
    )
    val rp136Changes=listOf("RP136: teclado del celular mediante QR local en el buscador; escritura, borrado y búsquedas desde dos letras. Compatible con Ethernet y Wi-Fi." to "RP136: phone keyboard through local QR in Search; typing, deletion and searches from two letters. Supports Ethernet and Wi-Fi.")
    val rp137Changes=listOf("RP137: deportes en vivo cambia automáticamente al siguiente enlace si falla o queda cargando; consulta una vez enlaces nuevos y avisa cuando no quedan disponibles." to "RP137: live sports automatically tries the next link on failure or stalled loading; resolves fresh links once and reports when none remain.")
    val rp138Changes=listOf("RP138: Ajustes con scroll continuo y foco estable; filtros de grabaciones sin cortar tarjetas. MotoGP en calendario y en F1, logos locales y nuevos avatares Friends, VisionQuest y Loki." to "RP138: continuous Settings scrolling and stable focus; recording filters no longer clip cards. MotoGP in Calendar and F1, bundled logos and new Friends, VisionQuest and Loki avatars.")
    val mobileRP139Changes=listOf(
        "RP139: Mobile M3 · actualización con los cambios de TV RP135–RP138 adaptados al celular." to "RP139: Mobile M3 · updated with TV RP135–RP138 changes adapted for phones.",
        "Deportes: recuperación automática de enlaces y acciones táctiles cuando se agotan; grabaciones por deporte con controles desplazables." to "Sports: automatic source recovery and touch actions when sources run out; recordings by sport with scrolling controls.",
        "MotoGP y eventos especiales en suscripciones, logos locales y calendario con varias ligas por día." to "MotoGP and special events in subscriptions, bundled logos and multiple leagues per calendar day.",
        "Avatares por grupos, incluidos Friends, VisionQuest y Loki; edición de perfil desplazable y audio secundario claro." to "Grouped avatars including Friends, VisionQuest and Loki; scrollable profile editing and clear secondary audio."
    )
    val mobileRP140Changes=listOf(
        "RP140: Mobile M4 · historial de búsqueda y selector de add-on con Mandanguita TV separado." to "RP140: Mobile M4 · search history and add-on picker with separate Mandanguita TV results.",
        "Cast visible arriba del reproductor, sin botón de mute. Compartir carátula y sinopsis con enlace de BruniO o IMDb." to "Cast visible at the top of the player, with no mute button. Share artwork and synopsis with a BruniO or IMDb link.",
        "Tráiler con aviso cuando no está disponible; Watch Party abre la sala al crearla." to "Trailer feedback when unavailable; Watch Party opens the room after creation."
    )
    private val mobileReleaseRP135=com.miflix.native2.BuildConfig.APPLICATION_ID.startsWith("com.brunio.mobile")
    val rp141Changes=listOf(
        "RP141: Filmografía del reparto corregida en móvil y TV: consulta por identificador, sin mezclar búsquedas anteriores, entrevistas ni apariciones como él mismo." to "RP141: Cast filmography fixed on mobile and TV: exact person ID, without previous search results, talk shows or self appearances."
    )
    val mobileRP142Changes=listOf(
        "RP142: Mobile M6 · Recordar último perfil disponible en Ajustes → Inicio y perfiles." to "RP142: Mobile M6 · Remember last profile available in Settings → Startup and profiles."
    )
    val rp143Changes=listOf(
        "RP143: Teclado del sistema para dictado en Fire TV, fuente de TV de LIDOM con reproducción directa y errores visibles durante 8 segundos con Cerrar." to "RP143: System keyboard input on Fire TV, direct playback for LIDOM TV sources and 8-second dismissible error notices."
    )
    val rp144Changes=listOf(
        "RP144: Reintentos limitados del mismo canal IPTV/LIDOM ante HTTP 404, en pantalla completa y SplitScreen, sin reiniciar los demás cuadros." to "RP144: Bounded same-channel IPTV/LIDOM retries on HTTP 404 in full screen and SplitScreen, without restarting other tiles."
    )
    val rp145Changes=listOf(
        "RP145: Buscador con teclado original; retirado el diálogo Fire TV. QR con dirección de red activa, regeneración y redes alternativas cuando estén disponibles." to "RP145: Original search keyboard restored; Fire TV dialog removed. QR uses the active LAN address, regeneration and alternative interfaces when available."
    )
    val rp146Changes=listOf(
        "RP146: Teclado Bluetooth opcional en el buscador y guía de dictado con el teclado del celular en la página QR local." to "RP146: Optional Bluetooth keyboard shortcut in Search and phone keyboard dictation guidance on the local QR page."
    )
    val rp147Changes=listOf(
        "RP147: Naruto y Naruto Shippuden vuelven a sus temporadas originales para evitar el error de numeración. DBZ conserva sus 291 episodios unificados." to "RP147: Naruto and Naruto Shippuden use original seasons again to avoid the numbering error. DBZ retains its unified 291 episodes."
    )
    val rp148Changes=listOf(
        "RP148: Francés y coreano en la interfaz por perfil; prioridad de audio francés y subtítulos franceses. Los textos sin traducción conservan inglés." to "RP148: Profile-scoped French and Korean interface options; French audio priority and subtitles. Untranslated text retains English."
    )
    val rp149Changes=listOf(
        "RP149: Hotfix de compilación: incluye el controlador de reintentos HTTP 404 y la detección del código HTTP que necesita el reproductor." to "RP149: Build hotfix: bundles the HTTP 404 retry controller and HTTP status detection required by the player."
    )
    val rp150Changes=listOf(
        "RP150: V1 de descubrimiento: línea temporal de franquicias, BruniO AI con filtros locales y Gemini opcional, colecciones por perfil, selección por tiempo y sesión de invitado en memoria." to "RP150: Discovery V1: franchise timeline, BruniO AI with local filters and optional Gemini, profile collections, time-based picks and an in-memory guest session."
    )
    val rp151Changes=listOf(
        "RP151: Timeline TV con foco y progreso; Discovery AI/Gemini opcional, colecciones y modo invitado. Vota qué ver en Party, busca pistas en sinopsis de episodios, activa miniaturas al buscar y recibe avisos poscréditos cuando hay marcas disponibles." to "RP151: Focused TV timeline and progress; Discovery AI/optional Gemini, collections and guest mode. Vote on party playback, search episode synopsis clues, enable seek thumbnails and receive post-credits notices when markers are available."
    )
    val rp152Changes=listOf(
        "RP152: BruniO AI con tarjetas de inspiración y resultados con carátulas, micrófono junto al buscador y Gemini en Settings → BruniO AI. Miniaturas del reproductor siempre activadas." to "RP152: BruniO AI with visual inspiration and poster results, a microphone beside Search and Gemini under Settings → BruniO AI. Player scene previews are always enabled."
    )
    val rp153Changes=listOf(
        "RP153: Guardado de Gemini compatible con claves AQ. y claves largas. Verifica el cifrado y la escritura antes de confirmar; muestra errores seguros en TV y celular. AI integrada bajo la barra superior, fondo negro, teclado solo al pulsar y separación de controles." to "RP153: Gemini saving supports AQ. and longer keys. Verifies encryption and persistence before confirmation; displays safe errors on TV and phone. AI under the top navigation, black background, press-to-edit input and spaced controls."
    )
    val changes=if(mobileReleaseRP135) rp148Changes else rp153Changes
    val previousChanges=(if(mobileReleaseRP135) mobileRP142Changes+rp141Changes+mobileRP140Changes+mobileRP139Changes+rp134Changes else rp152Changes+rp151Changes+rp150Changes+rp149Changes+rp148Changes+rp147Changes+rp146Changes+rp145Changes+rp144Changes+rp143Changes+rp141Changes+rp138Changes+rp137Changes+rp136Changes+rp135Changes+rp134Changes)+rp133Changes+rp132Changes+rp131Changes+rp130Changes+rp129Changes+rp128Changes+rp127Changes+rp126Changes+rp125Changes+rp124Changes+rc59Changes+legacyChanges+rc62Changes+listOf(
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
                item {androidx.tv.material3.Text(tr("Versiones anteriores","Earlier versions"),color=Accent,fontSize=20.sp)}
                items(UpdateNotes.previousChanges.size) {index -> val change=UpdateNotes.previousChanges[index];androidx.tv.material3.Text("• "+tr(change.first,change.second),color=Muted,fontSize=15.sp,modifier=androidx.compose.ui.Modifier.fillMaxWidth().contentFocusBorder().focusable().padding(10.dp)) }
            }
            FocusButton(tr("Cerrar","Close"),modifier=androidx.compose.ui.Modifier.focusRequester(focus)) {state.markNoticesRead(UpdateNotes.id);close()}
        }
    }
}
