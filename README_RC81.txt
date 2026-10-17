BruniO TV RC81 — Home layout (sobre RC80)

Aplicación:
1. Extrae el ZIP y reemplaza sus 18 archivos sobre la raíz del proyecto RC80 en GitHub, conservando las rutas.
2. Ejecuta el build habitual de Android y actualiza la TV. Versión 2.0.0-rc81, versionCode 22080.
3. No requiere SQL, Edge Functions ni redespliegue de Vercel.

Cambios:
- Las tarjetas expanden su ancho real después de 3 segundos y desplazan las vecinas.
- Continue Watching utiliza el formato vertical del resto, con el título dentro.
- Bloque superior de Home más compacto y adaptable a la altura.
- Cada grupo de Home ocupa el espacio debajo del bloque superior; el desplazamiento alinea encabezado y tarjetas juntos al subir y bajar.
- Streaming conserva sus logos y elimina los nombres externos redundantes.
- Accesos Collections y Oscars junto al encabezado Genres para aprovechar el espacio.
- Changelog RC81 incluido. Conserva funciones y cambios previos de RC80.

Verificación:
Cinco verificaciones automatizadas de estructura, geometría y regresión aprobadas. No se pudo compilar el APK en este entorno: no hay SDK de Android/Gradle wrapper. Pendiente validar en TV real el enfoque, desplazamiento sostenido y ajuste visual.
