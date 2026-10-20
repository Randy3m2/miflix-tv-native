BruniO RP50 — Others: canales numerados y nombre automático
Aplicar sobre RP49. Versión 2.0.0-rp50 (22154).

- Incluye 200 variantes exactas del enlace de TVClub indicado, del 1.m3u8 al 200.m3u8, en Others. No modifica otras partes de la URL.
- Un escaneo único al preparar este parche encontró 83 manifiestos HLS válidos con nombre legible; las 117 variantes restantes conservan el nombre provisional «Canal N». No se afirma que los 200 reproduzcan. Puede haber diferencias por dispositivo, ubicación, caducidad o disponibilidad.
- Los números son los IDs de la URL, no la posición de la tarjeta. El 43 se identificó como Animaniacs; el 42 como AnimalPlanet HD.
- El catálogo está empaquetado en un asset y no se escanea automáticamente desde la TV. No hay tareas de refresco, proxy de video ni nuevas llamadas a Supabase.
- Los canales personales existentes se conservan. Si una URL ya está guardada, no se duplica; su nombre personalizado se conserva. Se permiten hasta 100 canales personales adicionales; los 200 incluidos no consumen ese cupo.
- Las ediciones y eliminaciones son locales por cuenta/perfil. Eliminar un canal incluido lo mantiene oculto al volver a entrar: no se vuelve a insertar automáticamente.
- Los canales aparecen en Others, en la categoría Others de Live TV y en el selector compartido para SplitScreen.
- En reproducción, OK/centro o derecha abre el panel lateral de categorías/canales, enfoca el canal actual y muestra su estado Now playing. Desde los canales locales, carga Others sin esperar ni consultar a Nauta. Arriba/abajo con controles ocultos conserva el cambio al siguiente/anterior canal local.
- Búsqueda por nombre o número desde el panel. Pulsación larga conserva las opciones de edición/eliminación.
- Al agregar por QR, deja el nombre vacío para detectarlo. Un nombre escrito manualmente tiene prioridad. Se consulta solo el manifiesto indicado, en un worker, con límite de 64 KiB y 2 segundos. Nunca descarga segmentos para detectar nombres. Si falla, usa Canal N o Canal personal. Conserva URL, query y token originales.
- Detecta nombres desde la ruta get_ts/.../NombreDelCanal/segmento.ts o del título explícito HLS. No interpreta títulos de segmentos/episodios como nombres oficiales. El número se obtiene de N.m3u8.
- Para usar el nuevo formulario, genera y escanea un QR nuevo después de actualizar.

Logos:
- Importa 214 asociaciones de logos del archivo M3U aportado, sin importar sus 220 canales ni modificar su categoría. Coinciden 40 de los 200 canales por URL o nombre normalizado no ambiguo. Los demás conservan el texto como respaldo. Las imágenes se cargan desde las URLs originales usando la caché existente de Coil.
- Logos en tarjetas de Others/Live TV, panel lateral y Mis canales. Fondo oscuro y ajuste Fit sin deformarlos. Al fallar una imagen de la tarjeta se muestra el nombre.

Validación:
9 pruebas JUnit (6 nuevas y 3 de regresión) de extracción de nombre/número, query, Unicode, títulos HLS, fallback y respeto del nombre manual. Compilación JVM del detector con OkHttp real, del almacenamiento con adaptadores Android y del servidor Java real. Integración de almacenamiento: 200 entradas, identidad estable, deduplicación, edición/eliminación por perfil, formato HLS, cambio al siguiente y escrituras fallidas. Prueba HTTP local: detección de nombre y guardado por QR con nombre vacío; se conservan controles de origen y secreto del formulario. Sintaxis Kotlin y espejos comprobados.
No se ejecutó Gradle ni se generó APK. Falta validación real en Android TV del panel, foco, reproducción y SplitScreen. La respuesta del manifiesto no garantiza reproducción de sus segmentos.
Sin SQL ni Vercel. Copiar conservando rutas; menos de 100 archivos.

La lista «Lista de Magma TV- Titan2022_XD.m3u» se revisó sin importar sus canales, según tu elección «Solo verificar». Después autorizaste reutilizar sus logos: solo se incluyen las asociaciones de imágenes. No sustituye el catálogo. Muestra remota: 9/12 manifiestos válidos, 2 respuestas 404 y 1 respuesta 520; no equivale a reproducción verificada.
