# BruniO RP167 — TV y móvil

Parche de código fuente sobre RP166, commit a3e71055dc63d1b260b9c3df549206bb43ca84cb. Copia las carpetas del ZIP sobre tu repositorio. Compila e instala TV y móvil para usar el remoto. Versión/código: TV 2.0.0-rp167, móvil 1.0.8-m9-rp167, 22271. No se publicó en GitHub ni se desplegó el backend.

## Próximo episodio y Smart Prebuffer

Antes del final aparece una tarjeta con imagen, sinopsis y cuenta del tiempo restante. Puedes reproducir el próximo o abrir el selector de episodios sin salir del reproductor. El contador previo informa cuándo termina el episodio actual; el countdown de autoplay existente sigue ejecutándose después del final, sin cortar los últimos segundos. Ocultar la tarjeta no cambia tu preferencia de autoplay. No interfiere con Party, reproducción aleatoria, momentos ni una cola programada.

Smart Prebuffer es solo TV, opcional en Settings → Playback. Intenta una sola fuente, en los últimos 35 segundos, sin sonido ni historial anticipado. Objetivo de búfer: 4 segundos/8 MB; el reproductor, decodificador y segmentos tienen memoria adicional. Construye fuera del hilo de interfaz y reutiliza el mismo reproductor al pasar al siguiente episodio. Se omite en red medida, dispositivo de poca RAM, heap escaso, vídeo actual superior a 1080p o sin decodificador que reporte capacidad para dos instancias. Exige suficiente búfer del episodio actual; si se deteriora o cambias contenido/fuente, cancela y libera. No hay múltiples precargas ni reintentos de fuentes para esta función. Las fuentes incompatibles siguen el flujo normal. Los equipos limitados pueden omitirla siempre; el objetivo es proteger la reproducción actual.

## Remoto móvil

Botón de control remoto junto a Settings en la cabecera del móvil. En la TV abre avatar → Conectar teléfono / PC; allí ves IP y código de seis dígitos. Introduce ambos en el móvil. Mismo segmento de red local, TV en primer plano. El código de emparejamiento vence a los 2 minutos; genera un enlace/código nuevo si hace falta. La sesión de control dura 30 minutos y se invalida al cambiar red, cuenta/perfil o cerrar el receptor.

Incluye flechas, OK, Back, Home, Play/Pause, ±10 segundos, volumen multimedia y mute. La posibilidad de variar el volumen depende de la salida de audio del dispositivo. Fuentes y subtítulos abren sus menús en la TV; eliges con flechas y OK. El teclado del móvil permite escribir en el campo actualmente enfocado de la TV cuando admite InputConnection, o lanzar una búsqueda. No es un teclado de sistema para otras apps.

No usa Supabase para comandos ni transmite URLs de streams. Usa el receptor HTTP de LAN ya existente con código, token de sesión, lista de comandos y protección contra repetición. No funciona en Kids ni durante Watch Party. Debes reconectar si la TV pasa a segundo plano o cambia de perfil/red.

## Visual Seek Preview

Caché de imágenes de aproximadamente 12 MB que sobrevive al cierre de la tarjeta, lector reutilizado por fuente y cabeceras, menor espera al navegar y precarga de fotogramas vecinos. La tarjeta muestra la escena cercana, el punto seleccionado y si la imagen anterior todavía se está actualizando. No presenta una carátula como si fuera una escena real. Extracción fuera del hilo principal, un extractor a la vez y liberación tras inactividad. Algunas fuentes HLS/DASH/servidores no permiten extraer miniaturas o ignoran cancelación nativa: se conserva el tiempo y el aviso de indisponibilidad; no hay promesa de imágenes en todas las fuentes.

## Calendar y widgets

El widget abre una pantalla Calendar dedicada dentro de BruniO, incluso si la app estaba en Settings o en reproducción. El widget mensual abre hoy; los eventos y el pie de próximos eventos abren su fecha. Si falta elegir perfil, la intención espera a que lo selecciones. Se mantienen las restricciones del perfil.

## ICS móvil

Dentro de Calendar: Export / Subscribe · ICS.

- Guardar .ics: elige destino con el selector Android. Compartir/importar: usa el menú Android y una app compatible.
- Exporta los eventos cargados del calendario de BruniO: suscripciones deportivas, recordatorios personales y eventos compartidos. BruniO sigue siendo el origen principal. No genera de forma independiente todos los estrenos posibles del catálogo.
- Es una copia; volver a importar archivos depende de cómo cada calendario maneje duplicados. Los eventos tienen UID estable por cuenta/perfil, fechas completas o UTC y plegado/escape RFC 5545. Si no hay hora de final, los eventos con hora usan una duración de una hora.
- Para actualización automática instala el servicio HTTPS indicado en SETUP_ICS_RP167.md. La URL privada es de lectura y puede revocarse desde la app. La app publica al refrescar Calendar y mediante trabajo de fondo cada 6 horas, sujeto a Android. No vuelve a subir contenido sin cambios. Los eventos compartidos reflejan la última sincronización de la app; en segundo plano se refrescan las fuentes locales del perfil activo. Google/Outlook/Samsung controlan su propia frecuencia de consulta.

## Android TV Home

Settings → Appearance → Android TV Home. Publica Continue Watching, My List y recomendaciones, hasta 20 tarjetas por fila, con enlaces que abren sus detalles en BruniO. Puedes pedir al launcher que muestre cada fila o activarlas en Personalizar canales. El launcher decide su visibilidad y orden. Se actualizan desde la app, sin un sondeo adicional a Supabase. Kids no publica filas; el modo invitado no altera las filas de tu cuenta.

Solo launchers Android TV con API de canales/TvProvider. El launcher oficial de Fire TV no queda integrado: Amazon retiró “Recommended by your apps” y su vía actual de catálogo/personalización requiere una integración específica de Amazon. En esos equipos BruniO informa que no está disponible; no crea notificaciones fingiendo filas.

Referencias: https://developer.android.com/training/tv/discovery/recommendations-channel y https://community.amazondeveloper.com/t/recommended-by-your-apps-row-on-fire-tv/4244/8

## YouTube de equipos por QR

Live → LIDOM → equipo → Actualizar YouTube / fuente por QR. Escanea con el celular en la misma red y pega el canal oficial o un video. Un canal reemplaza la fuente de canal existente del equipo; los videos de partidos pueden seguir como fuentes separadas. Al cambiar canal se limpia el último video de esa fuente para no sugerir un juego del canal anterior. Mantiene el reproductor embebido y SplitScreen existentes, sujeto a las restricciones de YouTube.

## Prueba en dispositivos

Ver VALIDACION.txt para comprobaciones automáticas. No se probaron una TV física, el mando del celular en Wi-Fi real, los launchers de tus dispositivos ni un feed desplegado en tu cuenta. Revisa estas combinaciones antes de darlo por validado en tu equipo: final de episodio con y sin autoplay; selector de otro episodio; saltar atrás mientras precarga; emparejar/desconectar remoto; texto y menús; widget con app cerrada y abierta; importar ICS; revocar URL; activar filas en el launcher; cambiar canal por QR.
