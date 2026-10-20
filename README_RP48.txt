BruniO RP48 — Canales personales compartidos + plataformas
Aplicar sobre RP47. Versión 2.0.0-rp48 (22152).

Canales personalizados:
- Others muestra los canales guardados directamente como tarjetas, además del acceso Mis canales para añadir más por QR.
- Mantén OK sobre un canal personal para editar por QR o Eliminar canal. La misma opción está en Mis canales y Live TV.
- Live TV incluye una categoría Others con los mismos canales del perfil.
- SplitScreen → Agregar canal o Reemplazar → Others muestra la misma lista. Puedes añadir/reemplazar con tus canales personales.
- Actualizar fuentes en una ventana personal toma la URL guardada de ese canal, conservando formato HLS/DASH y sus parámetros. No consulta Nauta para esos canales.
- Guardar/editar/borrar actualiza las tres listas locales. La URL del canal se conserva completa. No se modifica el formulario QR corregido en RP47.
- La separación por cuenta/perfil permanece. Borrar de la lista no interrumpe un stream que ya esté reproduciéndose; deja de aparecer en los selectores.

Streaming:
- Paramount+ y Hulu: logos de portada + GIF al enfocar; respaldo estático si falla la animación.
- AMC+: logo oficial estático.
- Recursos incluidos dentro de la app. Hulu aparece una sola vez.
- Paramount+ y AMC+ usan el catálogo normal de plataformas (Top 10, películas, series y géneros). Se conserva AppState de RP31 con solo dos entradas adicionales en el mapa de proveedores.

Validación:
3 tests JUnit de categoría/identidad/MIME y pruebas JVM del código real de almacenamiento con SharedPreferences simuladas: lista compartida, emisión de cambios, edición, borrado, perfil/cuenta y fallo de guardado.
Sintaxis de 10 fuentes Kotlin, GIF con múltiples cuadros, rutas duplicadas e integridad de las adiciones de plataformas comprobadas.
No se ejecutó Gradle ni se generó APK. Falta validar en TV la navegación, los logos al enfocar y reproducción real de varias ventanas.
No requiere SQL ni Vercel. Copiar conservando rutas.

Fuentes de recursos:
https://github.com/luckynumb3rs/stremio-perfect-setup/tree/main/collections/streaming (Paramount+, Hulu)
https://images.amcnetworks.com/amcplus.com/wp-content/uploads/2020/08/amclogo@2x.png (AMC+ oficial)
https://github.com/jellyfin/TMDbLib/blob/master/TMDbLib/Objects/Discover/WatchProvider.cs (identificadores de proveedores)
