# BruniO RP79 — Modo fácil por perfil

Versión: `2.0.0-rp79` · versionCode `22183`.
Parche sobre RP78, con las mejoras anteriores conservadas. Copia el contenido respetando las rutas. No incluye APK, SQL ni cambios para Vercel.

## Uso

En el modo normal: **Settings → System → Modo fácil → Activar para este perfil**.
El ajuste se guarda por cuenta/perfil en esta TV. No modifica otros perfiles ni sincroniza el interruptor entre dispositivos. Kids conserva su interfaz y restricciones propias; este modo se activa en perfiles de adulto.

Al activarlo, arriba quedan únicamente **Home, Buscar, Mi lista y En vivo**, junto al logo de BruniO. No aparece menú de perfil, notificaciones ni ajustes completos. Las rutas a pantallas avanzadas también se bloquean en AppState y en el contenedor principal.

Para desactivarlo, mantén OK sobre **BruniO** en la barra y confirma **Desactivar modo fácil**. El primer botón de la confirmación mantiene el modo activo para evitar una salida accidental. Es una interfaz simplificada, no un bloqueo con PIN.

## Pantallas y reproducción

- Home sencillo: Seguir viendo, Películas y Series. Sin recomendaciones sociales, moods, plataformas ni configuración inicial de gustos.
- Búsqueda de películas y series con la corrección aproximada de títulos existente; sin búsquedas por moods ni categorías.
- Mi lista con tarjetas grandes, cuatro por fila, 15 entradas iniciales y Ver más. Puedes guardar/quitar un título desde su pantalla sencilla.
- Series: reproducción/continuación y selección de temporada/episodio, sin menús de contexto ni acciones sociales. Conserva el siguiente episodio automático cuando esté activado.
- Live TV utiliza el listado compartido de canales y categorías, con el comportamiento existente de OK, arriba/abajo y Atrás. No abre automáticamente el último canal al entrar. Importación, borrado y menús avanzados por pulsación larga quedan ocultos en este modo.
- Reproductor de películas/series: solo Reproducir/Pausar e Idioma de audio, con tiempo como información. Directos: Canales e Idioma de audio. No hay Party, cola, info técnica, enlaces manuales, PiP, Split Screen ni Live Scores.
- Texto ampliado y botones grandes. El audio primario/secundario y el idioma de interfaz del perfil se respetan; las preferencias se configuran antes de activar el modo.
- VOD de este modo consulta exclusivamente **Deepflix y Mandanguita TV**, mediante un repositorio separado. No consulta Comet, Torrentio ni TorBox. Conserva las configuraciones del modo normal y usa claves de caché/enlace anterior que distinguen ambos modos. Conserva los filtros de tamaño e idioma ya configurados.

## Progreso y consultas

El progreso sigue entrando en Seguir viendo desde el umbral existente de cinco segundos. Se añade una copia local por cuenta/perfil, con hasta 1000 entradas recientes de progreso, actualizada en los puntos de guardado local existentes. Al recuperar el perfil se conserva el valor más reciente entre la copia local y la nube. Las entradas se basan en la posición real, sin enviar un heartbeat a Supabase. La lista de favoritos local se recupera de esa copia solo sin sesión; con sesión se respeta la lista de la nube.

Se conserva la sincronización de progreso/lista existente al salir o finalizar y el inicio de sesión/renovación necesarios. No se introduce un temporizador de escrituras a Supabase. Las fuentes de video, TMDB e IPTV siguen necesitando sus peticiones normales.

En Modo fácil no arrancan los bucles de actividad social/solicitudes de Party, polling/heartbeat de sala, consultas de notificaciones ni carga de ratings. Se suspenden publicación y reintento de presencia, Trakt watched, restauración de Party al iniciar, servidor companion LAN y paneles sociales. Al activar el modo se abandona la sala si existía y se intenta **un evento de detener presencia**, con espera máxima de cuatro segundos; después se cancela el worker. Ese cierre inicial puede generar consultas, pero no se repite durante la reproducción.

No se ha medido un porcentaje de ahorro de logs. El ahorro depende de las funciones y tráfico que antes estuvieran activos; ocultar botones por sí solo no sería suficiente.

## Validación realizada

- PASS: compilación Java y ejecución de `verification/EasyModeRP79Check.java`: aislamiento de preferencias, rutas/menús permitidos y política inmutable de dos proveedores.
- PASS: `python3 verification/EasyModeRP79WiringCheck.py`: verificaciones estáticas de integración de las protecciones y cachés.
- Inspección sintáctica de Kotlin: archivos nuevos/pequeños y bloque nuevo de AppState sin errores; Screens y raíz no añaden diagnósticos frente a sus bases. El parser completo de AppState es inestable y tiene diagnósticos heredados; se revisó su diff y se comprobó aparte el bloque añadido.
- Se incluye `EasyModeRP79Test.kt` para JUnit en CI; no se ejecutó localmente.

**No se compiló Android ni se generó APK. No hay Android SDK/Kotlin compiler configurado en este entorno. Falta validar compilación y comportamiento visual/foco en TV.**

Prueba recomendada en TV: activar en un perfil, buscar/reproducir, cambiar audio, salir y recuperar Seguir viendo tras reiniciar; navegar canales con OK/arriba/abajo; comprobar que no hay acciones avanzadas; desactivar manteniendo el logo y comprobar que otro perfil conserva su modo normal. Comparar consultas de Supabase con la misma sesión/tiempo antes y después si se desea medir el ahorro.
