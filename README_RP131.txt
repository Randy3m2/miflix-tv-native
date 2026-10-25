BruniO RP131 — SplitScreen Stats + F1
Version: 2.0.0-rp131 / 22235

APLICAR
Parche acumulativo: incluye RP130 completo. Aplicar sobre RP129 o RP130,
conservando las rutas. 13 archivos. Sin SQL ni APK.

DISEÑO / CONTROLES
- 2 videos: Stats independiente; resumen glass encima de cada video.
- 3 videos: cuarto cuadro para Stats, selector de pantalla y tabla navegable.
- 1/4 videos o YouTube expandido: no se inserta este panel de estadísticas.
- SplitScreen en un pie reservado, sin cubrir estadísticas superiores.
- Botones nuevos con un único clickable, foco blanco/negrita y cierre X.
- X/Atrás cierran Stats antes de salir; los refrescos no solicitan foco.
- No recrea reproductores ni cambia audio/calidad al abrir o cerrar Stats.
- En pantallas sin altura suficiente, se ofrece panel lateral.

F1 NUEVO (EN SPLITSCREEN)
Fuente: https://site.api.espn.com/apis/site/v2/sports/racing/f1/scoreboard
Posiciones, pilotos, sesión y vuelta general cuando estén publicadas.
Sesión: Auto, prácticas, clasificación, sprint/carrera si están en el feed.
Auto prioriza una sesión en curso, después la última terminada y después la próxima.
Final y Programada se identifican; nunca se etiquetan como En vivo.
Vuelta general: campo period de ESPN. Vueltas individuales e intervalos solo si
ESPN devuelve esas estadísticas; en otro caso —. La respuesta real consultada tenía
posiciones y vuelta general, sin intervalos ni vueltas individuales.
No promete telemetría ni intervalos completos como F1 TV.

Detecta F1 por nombre/descripción (Sky Sports F1, DAZN F1, Fórmula 1...).
Título ambiguo: OK sobre el video → Stats · F1.
Para volver: opciones del canal → Stats · NBA / MLB / LIDOM.
El modo manual es por pantalla durante esta sesión SplitScreen.
NBA/MLB/LIDOM y selección de partido permanecen como RP130.
No añade F1 al reproductor normal de una sola pantalla en esta entrega.

RENDIMIENTO
Una petición compartida por paneles F1; caché 10 s; parseo Dispatchers.Default.
Refresco F1: 15 s en vivo según ESPN, 30 s fuera de sesión; backoff ante errores.
Conserva último dato con Sin actualizar si falla. Cancela al cerrar Stats/salir/fondo.
Sin peticiones por piloto, backend nuevo ni consultas/presencia/logs de Supabase.
Datos pueden adelantarse al video; dependemos del ritmo de publicación de ESPN.
Reutiliza recursos existentes para logos F1/LIDOM.

VERIFICACIÓN
HTTP 200 real de ESPN: cinco sesiones y 22 pilotos por sesión.
OpenF1 sin credenciales devolvió HTTP 401; no se usa como fallback silencioso.
38 pruebas JVM: fixture real ESPN, estados Final/En vivo/Programada, campos ausentes,
selección, caché compartida, cancelación y regresión NBA/MLB/LIDOM.
PSI de todos los Kotlin: cero errores.
Compilación parcial Kotlin/Compose 2.3.21 + Compose Android 1.11.4: paneles F1/Stats,
políticas y cuerpo modificado de MultiViewScreen; servicios/reproductores ajenos
sustituidos por stubs de verificación, no incluidos.

PENDIENTE
CI Android/Gradle completo, mando/TV y prueba durante sesión F1 en curso.
El fixture real consultado era Final; la rama En vivo usa fixtures controlados.
No se ha validado la latencia real de ESPN durante una carrera en este entorno.
