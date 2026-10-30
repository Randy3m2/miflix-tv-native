# BruniO RP159 · Details AI y asistente durante reproducción

Paquete acumulativo de fuentes RP153–RP159. No es APK. Copia `app` sobre el repositorio, conserva los demás archivos y ejecuta tu workflow habitual. Versión 2.0.0-rp159 / versionCode 22263 / Build 159.

## Details: Failed requirement

La espera entre consultas y el límite diario local estaban combinados en una validación que mostraba «Failed requirement». Ahora tienen errores específicos.

- Si sólo faltan segundos del intervalo de 20 segundos, la herramienta muestra un contador y continúa automáticamente. Cerrar el panel cancela la espera.
- Si se agotaron las 20 llamadas diarias locales de BruniO, lo explica y conserva ese límite; no lo confunde con la cuota de Google. Se restablece al cambiar la fecha local del dispositivo.
- Los errores de autorización, cuota de Google (429), conexión y respuesta vacía tienen mensajes propios. No se muestran cuerpos de error privados ni claves.
- El panel permite Reintentar sin tener que salir de Details. Las respuestas vacías no se guardan como análisis válidos.
- Todas las instancias del cliente Gemini comparten la exclusión de consultas para impedir ráfagas por abrir y cerrar paneles. Una espera local no consume otra llamada.

## Reproductor: respuesta oculta y foco

Había una ruta que dibujaba el buscador antes de la capa del video a pantalla completa. La respuesta podía quedar detrás del reproductor.

Las respuestas de búsqueda iniciadas desde el reproductor ahora se muestran en un panel negro a la derecha, sobre el video, con una ventana de foco propia. El reproductor actual conserva su composición y su solicitud de reproducción. El panel se puede cerrar con Back o Close.

La entrada rápida de voz/teclado también tiene su propia ventana de foco sobre la pantalla actual. El reproductor de fondo no recibe las flechas ni el OK destinados a esa entrada. Al cancelar se cierra la entrada y se vuelve a la ventana anterior. Se mantiene micrófono en TV y teclado inicial en Fire TV.

## Comprobación en TV

1. Desde Details pulsa AI después de una búsqueda reciente: debe mostrar la espera restante y continuar, o explicar un límite diario si corresponde.
2. Desde el reproductor abre AI, dicta una búsqueda de películas y comprueba que los resultados quedan visibles encima del video. Navega las tarjetas y pulsa Back para cerrar el panel.
3. Abre AI de nuevo y cancela el dictado; comprueba que puedes seguir usando los controles del reproductor.

No se usó la clave personal de Gemini ni se reprodujo el fallo en una TV física. La corrección de capas/foco se validó mediante compilación; la validación del control remoto y el comportamiento real de Android TV/Firestick sigue pendiente.
