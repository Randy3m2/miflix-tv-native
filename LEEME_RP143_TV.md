# BruniO TV RP143

TV 2.0.0-rp143 / versionCode 22247. Parche de código sobre el repositorio actualizado con RP141 y RP142. No es un APK firmado. La versión móvil permanece en M6 / RP142.

## Cambios

- En Fire TV (fabricante Amazon o modelo AFT), el botón de micrófono abre un campo Android nativo con el teclado del sistema, sin intentar lanzar primero el reconocedor genérico de Android que fallaba. Con el campo seleccionado se puede usar el dictado si el teclado del Firestick lo ofrece. Se conservan Buscar, Cerrar y un acceso al QR existente del celular. Otros Android TV mantienen su reconocedor habitual; si no existe la actividad, se ofrece el teclado del sistema.
- La app recibe el texto del campo nativo al pulsar Buscar/Listo. No se añaden grabación de audio propia, permisos de micrófono, servicios de Google, SDK de Alexa ni servicios externos de transcripción. No se puede garantizar que todos los Fire OS remitan el botón físico de Alexa al campo: si abre la búsqueda global, quedan disponibles el teclado y el QR.
- LIDOM: una fuente de televisión, HLS o DASH abre directamente el primer enlace resuelto, en el orden de coincidencia existente. No muestra un segundo listado de enlaces. Se conservan las cabeceras del stream y la selección de SplitScreen. No se modifica el mapeo de canales de los equipos ni se elimina ninguna fuente.
- Las URLs directas de videos de YouTube conservan su reproducción directa. Una fuente configurada como página de canal de YouTube conserva su selector de videos; no se lanza automáticamente una grabación antigua ni se la presenta como un partido en vivo.
- Los errores globales de TV permanecen 8 segundos y tienen Cerrar/Dismiss. Se retiene una copia visible aunque otra operación limpie el error de origen. Cerrar o Atrás permiten quitarlos antes. El comportamiento del móvil sigue siendo de 5 segundos. Los errores locales del selector LIDOM permanecen hasta cerrarlos o elegir otra fuente.

## Aplicar

Copia los ocho archivos de código/configuración respetando sus rutas, después de RP142. Ejecuta el workflow existente **build-native-tv-apk.yml** y genera el APK de TV con los secretos y firma habituales. Instálalo sobre la app anterior, sin desinstalar ni borrar datos. No hay SQL ni secretos nuevos. No se genera un APK móvil para este parche.

## Prueba en el Firestick

1. En Buscar, pulsa el micrófono. Debe abrir el campo y el teclado del sistema, sin el aviso anterior de servicio de voz ausente.
2. Selecciona el campo y prueba dictado con el control o el micrófono del teclado, si está disponible. Pulsa Buscar/Listo y verifica el texto en BruniO. Si Alexa sigue capturando el botón físico, prueba la entrada con teclado o QR; esa compatibilidad debe comprobarse en el Firestick afectado.
3. Prueba fuentes TV/HLS/DASH de LIDOM tanto en pantalla completa como al agregarlas a SplitScreen: no debe aparecer la lista secundaria de enlaces.
4. Provoca un error de fuente o una operación fallida: verifica su permanencia, Cerrar y Atrás.

Pendiente de prueba física en el Firestick afectado. Esta sesión no publica un APK firmado ni demuestra dictado real del mando.

Referencias de compatibilidad: Android RecognizerIntent (ActivityNotFoundException si no existe una app que lo atienda) y las guías de entrada de texto de Amazon Fire TV:
https://developer.android.com/reference/android/speech/RecognizerIntent
https://developer.amazon.com/docs/fire-tv/design-and-user-experience-guidelines.html
