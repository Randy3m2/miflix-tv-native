BruniO RP41 — aplicar sobre RP40 conservando todas las rutas.

LIVE HUB
9 tarjetas en cuadrícula 3x3, fondo negro y sin scroll: Live TV, NBA/Basketball, MLB/Baseball, F1, LIDOM, Live Scores, Fútbol y NFL, Others. Arte de referencia suministrado por el usuario, compartido como un solo recurso; foco con escala suave, borde blanco y brillo ligero.
Live TV abre el último canal exitoso de Nauta/IPTV del perfil, resolviendo un enlace nuevo. Si no existe o falla, intenta hasta 5 canales aleatorios de Nauta. El panel combinado Nauta/IPTV, sus categorías, favoritos y navegación RP40 se conservan.
NBA, MLB, F1, Fútbol, NFL y Others reorganizan los catálogos del manifiesto Live Sports YA CONFIGURADO: no se agrega otra fuente de eventos. Deportes dedicados muestran únicamente tarjetas de eventos, agrupando catálogos del mismo deporte. Others conserva pestañas de categorías para los deportes no asignados. Se excluyen feeds globales mezclados (All/Live Now) de Others para no repetir todos los deportes.
LIDOM usa el calendario, logos de equipos y fuentes del equipo local ya existentes; tarjetas de partidos. Selección automática de la primera fuente disponible; la programación real de un canal puede variar. Los feeds de videos de YouTube no se toman como prueba de un partido en vivo; solo videos directos configurados pueden abrirse automáticamente.

PLAYER LIVE
Se quitan Play/Pausa, subtítulos, audio, enlaces, Queue, Info y Channels de los controles visibles de LIVE. Los de películas/series se conservan. OK sobre el video abre el listado sin pausar; arriba/abajo sin controles siguen siguiente/anterior de la lista. Flecha derecha conserva acceso al panel. Atrás desde el video vuelve de una vez al menú de nueve tarjetas; los paneles abiertos se cierran primero. Corazón y herramientas restantes se conservan. Pausa por teclas multimedia se ignora en live.

SPORTSBITE
Others incluye TNT Sports y TBS Sports, con logos. Las URLs suministradas son páginas HTML, no enlaces multimedia. Se abren dentro de la app mediante WebView, identificadas como WEB; nunca se pasan a Media3. Ambas páginas respondieron HTTP 403 en la comprobación de este entorno: NO se ha validado reproducción nativa ni reproducción de la página. El acceso depende del sitio y del WebView de la TV. No se eluden sus bloqueos.

VALIDACIÓN
8 tests JVM de clasificación de deportes, con LiveCategory de prueba; 4 verificaciones JVM del código real de último canal con Context/red/reproductor simulados: inicio aleatorio, enlace fresco del canal recordado, separación por perfil, cambio de perfil durante resolución. Revisión sintáctica estática de los Kotlin modificados, XML de logos y rutas espejo.
Sin Gradle, build ni APK; pendiente compilar/probar en Android TV y validar foco, imágenes, fuente de deportes real y WebView. Sin SQL ni Vercel, sin nuevas consultas ni polling de Supabase. El manifiesto de deportes predeterminado devolvió 403 desde este entorno; se respeta el manifiesto configurado en cada instalación.
