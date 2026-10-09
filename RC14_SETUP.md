# BruniO TV Native 2.0 RC14

Proyecto completo acumulativo, con las mejoras de reproducción de RC13.

## Enlaces

Torrentio, Comet y otros add-ons instalados se combinan y ordenan automáticamente: resolución de mayor a menor, después tamaño de mayor a menor. Reconoce 8K/4320, 4K/UHD/2160, 1440, 1080, 720, 576, 480 y 360. Usa videoSize cuando existe, o el peso GB/GiB/MB/MiB escrito en el título. Los datos desconocidos quedan después de los conocidos. No hace falta configurar el add-on.

El orden se usa tanto en la lista manual como para el primer enlace del Play rápido. Un 4K grande puede necesitar más red y un decodificador compatible que un 1080p; el orden no verifica la compatibilidad de cada formato con la TV. Playback links permite elegir otro.

## Diseño y márgenes

- El foco blanco exclusivo se mantiene. El menú superior añade un indicador que se desplaza en 150 ms y una elevación leve, sin retrasar la respuesta del control.
- Directores fuera de Home. En Collections aparecen solo sus nombres; la búsqueda de sus películas sigue disponible. Ya no se descargan fotos de directores al cargar Home.
- Margen inferior del contenido y límites laterales en los carruseles. Se conserva el scroll horizontal; puede verse parte de una carátula siguiente dentro del carrusel, pero el margen exterior permanece libre.
- Catálogos y resultados de búsqueda usan columnas adaptables al ancho de pantalla, en lugar de forzar cinco carátulas. No se corta la cuarta columna por una fila demasiado ancha.
- Las etiquetas del reproductor se muestran en una franja de ancho completo, con hasta dos líneas, en vez de estar restringidas al ancho del icono.

## Búsquedas

Busca sin distinguir acentos, puntuación o mayúsculas; admite palabras parciales y pequeños errores de escritura. Combina resultados de TMDB con títulos ya cargados y hasta dos consultas alternativas cuando hay pocos resultados. No es una búsqueda semántica por trama ni un catálogo offline completo.

Live TV conserva el texto, el proveedor y la categoría al volver desde el canal durante la sesión de la app. Si cierras/reinicias la app, no persiste ese filtro. El buscador principal también conserva el texto al navegar y volver.

## Party en el reproductor

- Nuevo icono Create Party abajo, antes de Volume. Si ya estás en una sala, abre My Party.
- Crea la sala con el contenido y posición actuales, sin salir ni reiniciar el player.
- QR/chat móvil en un diálogo sobre el vídeo.
- Solicitudes de acceso: popup modal global, sobre el reproductor y los menús. Aceptar recibe el foco: OK acepta; derecha + OK rechaza. Atrás lo deja pendiente en el apartado de solicitudes, sin volver a interrumpir por esa misma solicitud durante esa sala.
- Las solicitudes se consultan en segundo plano; el listado de actividad de amigos continúa con refresh manual.
- Acceso privado y aprobación del host se mantienen en Supabase.

## Subir y probar

Reemplazar los archivos del repositorio y ejecutar Build MiFlix Native TV APK. El workflow ejecuta ahora testDebugUnitTest antes de assembleDebug: incluye pruebas de orden de enlaces y coincidencias aproximadas.

No hay SQL nuevo, cambios de Trakt/Pages ni del paquete Android. El canal native-latest y el APK permanente MiFlix-TV-Native.apk siguen iguales: mismo Downloader.

## Verificación

Se validaron sintaxis Kotlin/XML/SQL, geometría de columnas adaptables en seis anchos, integridad del ZIP y las pruebas existentes de acceso privado/aceptar/rechazar/pausa/reanudación en la base de datos.

Las nuevas pruebas unitarias Kotlin se incluyen para ejecutarse en GitHub Actions; no se ejecutaron aquí. No hay Gradle ni Android SDK en este entorno. Compilación, aspecto final, foco, rendimiento de streams y comportamiento de popups deben comprobarse en la TV.

Comprobar: orden calidad/tamaño; Home sin directores; Collections con nombres y columnas completas; regreso de un canal con filtro conservado; nombres completos de los controles; crear Party y mostrar QR sin salir; recibir/aceptar/rechazar solicitudes tanto durante reproducción como sobre un menú abierto.
