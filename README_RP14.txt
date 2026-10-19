BruniO Android TV - RP14 - P2P

Aplicar este parche SOBRE RP13, conservando todas las rutas y reemplazando
los archivos existentes. Incluye el hotfix del test AddonHttpRP12Test.
Version: 2.0.0-rp14 / 22118.

ACTIVAR
Settings > Playback > P2P (Peerflix / Torrentio).
Apagado por defecto; interruptor independiente por perfil en esta TV.
Requiere Android 9+ y ABI ARM, ARM64, x86 o x86_64 compatible.
Los enlaces HTTP siguen usando su transporte habitual.

FUNCIONAMIENTO
Los complementos pueden devolver infoHash, fileIdx y trackers, o magnets v1.
El selector muestra estas fuentes con P2P. Se conserva el orden de idioma,
los filtros y los lotes de 7 enlaces. Sin fileIdx se elige el video mas grande,
excluyendo archivos sample. No se modifica el JSON de tus complementos.

Se descarga el archivo elegido mientras se reproduce. Los saltos priorizan
las piezas necesarias; cada pieza se verifica antes de entregarla a Media3.
Compatible con torrents v1 y con hash v1 de torrents hibridos; no v2-only.
La disponibilidad y velocidad dependen de los peers.

CACHE
Limite por archivo: 2, 4 (predeterminado), 8, 16 o 32 GiB en esta TV.
No es una cache circular: el archivo debe caber dentro del limite y del
espacio libre disponible, con reserva para piezas de frontera y sistema.
Si no cabe, se muestra error y se respeta el ajuste de siguiente enlace.
El motor comparte piezas mientras el player esta abierto (subida limitada
a 64 KiB/s). Se pausa al ir a segundo plano; al cerrar el player se detiene
la sesion y se elimina su carpeta temporal. Tras cierre forzado del proceso,
los restos se eliminan en el siguiente inicio de la app.

VALIDACION
38 pruebas JVM aprobadas, sin el modulo jdk.httpserver.
Motor Kotlin y adaptador compilados contra libtorrent4j y Media3 reales.
Prueba nativa Linux con torrent sintetico local: lectura inicial, saltos
adelante/atras, final del archivo y limpieza al cerrar, aprobados.
Estas verificaciones no sustituyen probar la APK en una TV real.
No se ejecuto Gradle Android ni se genero APK. Watch Party con P2P no se ha
validado. No requiere SQL ni Vercel. El motor no consulta Supabase; el
guardado de progreso y demas funciones existentes conservan su logica.
