BruniO RP121 / versionCode 22225
Aplicar sobre RP120 conservando las rutas. Parche de código; no APK. Sin SQL ni Vercel.

NAVEGACIÓN Y DISEÑO
- Live > NBA / MLB / F1 / LIDOM ahora tienen Games / Table / Stats / Players, localizados según idioma.
- Estilo glass; foco blanco/negrita; tarjetas de tamaño fijo, retratos circulares y mini logos.
- Seguir abre Suscripciones directamente en esa liga, reutilizando RP120.
- Conserva la barra superior, seis tarjetas principales, TV Channels, Others y restricciones del Modo fácil. No añade tabs avanzados a Easy mode.
- Recuerda tab y fecha por cuenta/perfil durante la sesión y después de volver del reproductor. Cambiar tabs mantiene el estado guardable de los filtros.

GAMES
- Usa los canales/eventos realmente publicados por el add-on Sports configurado. Mantiene imágenes, long press manual, autoplay y fallback existentes. No inventa enlaces a partir del calendario.
- Selector de día para NBA/MLB/F1 y para los enlaces LIDOM del add-on. LiveRepository ahora conserva released/startTime/startsAt/date o timestamps Unix del catálogo, sin alterar descripción ni URL.
- Convierte timestamps con zona a la fecha local y muestra hora de 12 horas cuando se publica.
- Si el add-on NO publica fecha, ese enlace solo aparece en Hoy y se indica en pantalla. No hay archivo histórico ni eventos futuros si la fuente no los entrega; no se deducen fechas solo por los nombres de los equipos.
- F1 conserva sus canales continuos de TV, disponibles también en otras fechas, identificados como canales.
- LIDOM: Add-on de deportes para sus eventos y Canales de equipos para el calendario/fuentes asignadas previos, incluidos QR/YouTube y fuentes de TV. No elimina ninguna asignación.
- Caché de la primera página en memoria por bloques de 5 minutos, con Actualizar manual. No guarda credenciales ni enlaces del add-on en disco.

TABLE
- Tabla de RP120 integrada dentro de cada deporte, sin abrir otra pantalla.
- NBA conferencias; MLB divisiones; LIDOM; F1 pilotos/constructores. Temporada actual y anteriores con sus controles existentes.
- El acceso de Others > Tabla de posiciones se conserva.

STATS
- Líderes de temporada: NBA puntos, rebotes, asistencias, tiros de campo, triples, tapones y robos.
- MLB/LIDOM: bateo (AVG, HR, RBI, SB, hits) y pitcheo (ERA, K, victorias, WHIP), según categorías publicadas. Los grupos se consultan y validan por separado para no confundir promedios de lanzadores con bateadores.
- F1: puntos y victorias de pilotos. Los campeonatos completos están en Table.
- Top 5 con retrato, nombre, equipo/logo y valor; empates conservan el rango compartido. No reutiliza estadísticas de una temporada anterior bajo otro título.

PLAYERS
- Fotos, búsqueda local, filtro de equipo, ficha ligera y carga visual de 40 en 40.
- NBA: filtra explícitamente active=true del directorio; elegir equipo carga su plantilla confirmada. Cuando el directorio global no publica club/posición, muestra NBA en vez de inventarlos.
- MLB: personas activas de clubes MLB según la fuente; activo no equivale a estar disponible médicamente para jugar hoy.
- LIDOM: plantillas activas de sus seis clubes; toma el equipo de la plantilla dominicana y no el club MLB del jugador.
- F1: pilotos de la última parrilla publicada, identificado así en pantalla. No presenta como vigente a todo piloto que sumó puntos alguna vez en la temporada.
- Destacados es una selección editorial de nombres conocidos que existen en la lista activa; después se ordena alfabéticamente. No es un ranking oficial de popularidad.

RENDIMIENTO / RED
- JSON, caché, descarga e índice de búsqueda fuera del hilo principal; UI lazy con claves estables y dimensiones fijas.
- Retratos limitados por tamaño de pantalla; solo se componen las tarjetas necesarias. No usa blur ni reordena el diseño cuando llega una imagen.
- Cliente OkHttp independiente sin cabeceras de cuenta; cancelable al cambiar de tab, 4 MB máximo y 12 s de timeout total. Sin retries de red ocultos.
- Stats y Players se consultan solo al abrirlos, con caché pública local de 6 horas y actualización manual. Los rosters de LIDOM usan como máximo dos descargas simultáneas.
- Cambiar filtros no vuelve a descargar el listado MLB/LIDOM/F1 ya guardado. No publica una lista de otro equipo mientras termina el nuevo filtro.
- No añade llamadas a Supabase ni analytics para estas pestañas. No modifica las funciones sociales existentes.

VALIDACIÓN
- 29 pruebas JUnit nuevas: filtros de fecha/zona, separación de deportes, activo/inactivo, bateo/pitcheo, empates, temporadas, fotos/modelos, límite de respuesta, cancelación real de socket y ausencia de Authorization.
- Compilación Kotlin/JVM de los módulos de datos con adaptadores de contexto/preferencias para pruebas; LiveRepository real también compilado y comprobado para fecha de catálogo y compatibilidad del constructor anterior.
- Parsers comprobados con payloads reales descargados: NBA 8 categorías/40 líderes y 524 activos; MLB 5 categorías de bateo + 4 de pitcheo; LIDOM 3 + 4 y plantilla Licey; F1 22 pilotos de última carrera y líderes de puntos/victorias. Estas cantidades NO están hardcodeadas.
- Parser Kotlin sin errores de sintaxis. NO es una compilación Gradle/Compose/Android ni una prueba de FPS, foco o reproducción en TV. Esa validación sigue pendiente; entorno sin Android SDK.

FUENTES
ESPN: API NBA líderes v3, atletas v3 y plantillas; MLB StatsAPI: líderes, jugadores y plantillas LIDOM; Jolpica: F1 resultados y clasificaciones. Logos y calendario reutilizan RP120. La disponibilidad de datos/enlaces depende de esas fuentes.
