BruniO RP42 — aplicar sobre RP41 conservando rutas.

- Nueve tarjetas más grandes: se elimina el límite de ancho central, se reducen márgenes y separaciones; las nueve siguen visibles sin scroll y conservan foco suave.
- Others: categorías oscuras con iconos, borde de foco blanco y tarjetas con gradiente y efecto ligero. Se retiran los accesos independientes TNT y TBS. M3U es una categoría propia y carga el archivo proporcionado, incluido en assets/others_rp42.m3u.
- IMPORTANTE M3U: el archivo tiene una sola entrada llamada Stream 1 y su URL es una página HTML de Sportsbite (la misma página de TNT), no un enlace multimedia. Se identifica como Página web y se abre en WebView; no se pasa a Media3. No se ha convertido en stream por estar en una lista M3U. El bloqueo 403 visto en RP41 no se elude. El lector también admite entradas multimedia directas con los encabezados de la lista.
- F1 combina los eventos de los catálogos F1/motor de Live Sports ya configurado con los canales Nauta que coinciden con F1/Formula 1. Consultas separadas; un fallo de deportes no elimina las tarjetas de Nauta ni viceversa. Conserva proveedor, primer enlace y encabezados originales. Nauta se consulta con F1 y Formula 1, filtrando coincidencias falsas y duplicados dentro del proveedor.
- LIDOM abre directamente el calendario y equipos, con Anterior/Hoy/Siguiente, fecha AST y seis tarjetas de equipos. Cada partido abre las fuentes del equipo local. Cada equipo abre sus propias fuentes existentes: conserva la configuración, alias, Nauta/IPTV, YouTube y SplitScreen del selector original. Ajustes abre solo el diálogo de configuración; ya no navega a la pantalla vieja. No se agregan ni se presuponen nuevas transmisiones.

VALIDACIÓN
8 tests JVM: detección de F1 y alias, exclusión de coincidencias falsas, distinguir HTML de video, lectura M3U, cambio de mes/año y año bisiesto. Revisión sintáctica estática y comprobación de rutas espejo. Sin Gradle, build ni APK. Falta validar visualmente y reproducir en TV real, incluida disponibilidad de las fuentes y WebView.
Sin SQL, Vercel ni nuevas consultas/polling a Supabase.
