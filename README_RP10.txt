BruniO TV RP10 — Continue Watching desde 5 segundos + LIDOM SplitScreen
Acumulativo: aplicar sobre RP6, RP7, RP8 o RP9, conservando todas las rutas.
Incluye los fixes completos de límites de add-ons y RP9 SplitScreen.
No hace falta subir los ZIPs intermedios.

Continue Watching:
Una película/episodio entra al guardar una posición >= 5000 ms, sin exigir 1 %.
La fila se actualiza directamente con los datos locales, sin pedir metadata otra
vez para ese título. Se mantiene una tarjeta por serie, últimos 10 títulos,
orden reciente y corte superior de 95 %. Finalizados y eliminados quedan fuera.
El reproductor revisa progreso cada segundo durante los primeros 5 segundos de
posición; después conserva el intervalo de 4 segundos. Al salir captura la
posición actual como antes. No se añaden sincronizaciones periódicas a Supabase;
se conserva la sincronización existente al salir del reproductor.
La regla usa posición de reproducción, no un nuevo contador de tiempo visto.

Validación RP10: 4 pruebas JUnit correctas (4999/5000 ms, finalizados y eliminados,
duración desconocida, orden/tope 10 y tarjeta única por serie).
Parser Kotlin sin errores nuevos frente a RP9. No se compiló Android ni se generó APK.
Prueba Node de controles embebidos YouTube de RP9 conservada y correcta.
Queda pendiente la compilación en CI y reproducción real en TV.
Sin SQL ni Vercel.

Detalle del SplitScreen incluido:
Cómo usar:
LIDOM > SplitScreen > equipo > fuente > señal/video.
Desde las fuentes de un equipo también puedes activar Abrir en SplitScreen
antes de escoger señal. El reproductor de señales LIDOM incluye el botón de
cuadrícula SplitScreen en la barra.
Dentro: Añadir LIDOM selecciona otro equipo/fuente; Añadir canal permite combinar
con Live TV. Dos señales se muestran lado a lado; tres o cuatro en cuadrícula.
OK en una señal: escuchar, reintentar, cambiar fuente/canal, ampliar o quitar.
Una señal tiene audio; las otras se silencian.
YouTube usa su reproductor embebido, sin extraer enlaces ni eludir restricciones.
Su reproducción depende de que el video permita embed y de Android WebView.
TV/HLS/DASH usan Media3 y conservan MIME explícito y headers del stream.
YouTube ampliado dispone de Volver a SplitScreen. Al ampliar se desmontan las
otras vistas; al regresar se preparan de nuevo, sin arrancar un quinto reproductor.
Salir devuelve a LIDOM cuando SplitScreen se inició allí.
Iniciar SplitScreen desde LIDOM deja la Watch Party actual, como el multiview.

Validación:
Parser Kotlin: cuatro archivos UI nuevos/modificados sin errores.
Prueba Node del JavaScript real embebido: silencio inicial, selección de audio,
silenciar al cambiar, pausa y errores. Rutas/definiciones nuevas presentes;
archivos espejo idénticos y paquete por debajo de 100 archivos.
NO se ha compilado Android ni generado APK. Falta probar reproducción simultánea,
foco del mando, consumo de memoria y compatibilidad de señales en TV.
No añade consultas Supabase periódicas ni requiere SQL o Vercel.
