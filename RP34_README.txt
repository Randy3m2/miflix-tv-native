BruniO TV RP34 — Selector de canales dentro del player
Aplicar sobre RP33 conservando rutas. Versión 2.0.0-rp34 / 22138.

Flecha derecha mientras se ve Live TV con controles ocultos o con el foco en la superficie del video abre el selector. Si el foco está en botones del reproductor, derecha conserva la navegación normal. Solo después del primer fotograma; no intercepta VOD, volumen o Atrás. Conserva las restricciones de cambio de canal para invitados de Watch Party.

El botón Channels y el selector de añadir/reemplazar canales de SplitScreen comparten el mismo menú. Los flujos especiales de fuentes/juegos LIDOM siguen usando su selector específico.

Menú a la izquierda sobre el video, negro/gris translúcido y foco blanco. Categorías verticales, canales a la derecha con logo, número, nombre y corazón. El video actual continúa detrás mientras se navega. Enfocar una categoría durante 180ms cambia su lista sin pulsar OK; se cancela la petición anterior. Flecha derecha desde categorías pasa a los canales; izquierda desde un canal vuelve a su categoría. Atrás o Cerrar vuelve al video. Buscar y cargar más permanecen disponibles.

Mantener OK sobre un canal añade/quita favorito, sin sintonizarlo. OK corto selecciona el canal o lo entrega al flujo SplitScreen. Se conserva proveedor/id para reproducir y guardar favoritos. Misma lista de favoritos por perfil que ya existe. Incluye categorías originales de Nauta y listas por país de IPTV; deportes se incluye cuando se abre desde ese proveedor. Se mantienen duplicados entre proveedores.

Caché de categorías en memoria 3h, caché local al menú para listas ya visitadas, máximo 4 consultas concurrentes para descubrir categorías. No añade polling ni consultas a Supabase.

Verificación estática: sintaxis Kotlin sin nuevos diagnósticos respecto a base; conexiones del atajo, Channels, SplitScreen, favoritos y guards de peticiones revisadas; espejos verificados. No se ejecutó Gradle Android ni se generó APK. Falta validar navegación, foco, long press y fuentes reales en TV. Sin SQL ni Vercel.
