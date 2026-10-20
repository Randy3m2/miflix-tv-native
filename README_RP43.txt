BruniO RP43 — aplicar sobre RP42 conservando rutas.

F1
Se amplía la búsqueda Nauta con Movistar y DAZN. Incluye F1, Fórmula 1, MovistarF1, DAZNF1 y canales generales DAZN (sin asumir que siempre emitan F1). Consulta también las listas IPTV ya integradas, priorizando España, con un máximo de dos listas simultáneas. Se reutiliza la caché IPTV de seis horas; no hay polling ni nuevas llamadas a Supabase. Mantiene origen, encabezados y selección del primer enlace. En la verificación de Nauta aparecieron Formula 1 TV (F1), DAZN HD y DAZN 2; no se encontró Movistar F1 en esas respuestas. La disponibilidad depende del proveedor y puede cambiar.

REAPERTURAS Y REPRODUCTORES
El manifiesto anterior no definía launchMode: el modo estándar podía permitir más de una actividad principal. RP43 usa singleTask y una defensa para lanzamientos duplicados antiguos. MainActivity monta Compose una sola vez; enlaces de Watch Party actualizan el código mediante estado y onNewIntent, conservando la composición existente. Se mantiene AppUsageTracker. La liberación del reproductor queda en finally, incluso si falla otro paso de limpieza. Los pools de SplitScreen ya liberaban los reproductores al salir; sus varias sesiones simultáneas son intencionales.
Esto es una revisión y corrección del código, no una confirmación de instancias duplicadas en la TV del usuario: no hay acceso al dispositivo ni a adb en este entorno. Tras actualizar, Forzar detención y reabrir una vez permite probar desde un estado limpio sin borrar datos.

SPLITSCREEN
Los videos aprovechan la pantalla; las herramientas pasan a un panel lateral oscuro y compacto con secciones Añadir, Pantallas activas, Audio y Opciones. Se puede ocultar/reabrir. Opciones de cada canal en un panel lateral parcial. El selector al añadir/cambiar canales se abre en modo compacto (60% de ancho, 88% de alto en pantallas amplias), conservando el selector normal de Live TV con altura completa. Audio, cambio de fuentes, ampliar/quitar y LIDOM se conservan. Al salir vuelve al hub Live TV.

M3U
Se retira la categoría de Others y su lector. HubM3URP42.kt queda vacío de funcionalidad; el asset anterior se reemplaza por una lista vacía, sin URLs. No se modifica ni se convierte el archivo original subido por el usuario.

VALIDACIÓN
12 tests JVM de coincidencias F1/DAZN, listas y fechas; 4 comprobaciones de MainActivity con Android/Compose simulados (creación, reapertura, enlace y guardia de lanzador duplicado). Revisión sintáctica estática de Kotlin, XML del manifiesto, una sola llamada setContent y archivos espejo. Sin Gradle, build ni APK. Pendiente validar UI, navegación, ciclo de vida y disponibilidad real de los canales en Android TV.
Sin SQL ni Vercel.
