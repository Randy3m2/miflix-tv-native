# BruniO PC — reconstrucción en desarrollo

No es una versión final ni un instalador. Móvil (`mobile/`), TV (`app/`) y sus flujos de compilación quedan fuera del alcance. El cliente anterior se conserva para trasladar sus funciones.

## Código conectado

`ConnectedDesktop` es la entrada de Compose. `BridgeClient` mantiene un proceso Python privado por stdio, con identificadores de petición, límite de respuesta y cierre del proceso al salir. No publica un servidor HTTP ni registra credenciales. El servicio usa el `Client` existente y su almacén DPAPI de Windows.

Se escribieron conexiones para restaurar sesión, login, selección de perfil, búsqueda, detalles, lista, filas de Inicio, actividad y solicitudes de amigos, aceptación de solicitudes, calendario compartido y catálogos de Live TV/deportes. El calendario tiene navegación mensual y vista por día. Inicio sigue el orden Seguir viendo → Streaming → Géneros → Recomendaciones → otras filas. El servicio respeta prioridades de complementos por perfil/idioma y ofrece resolución de fuentes, todavía sin consumidor de vídeo en Compose.

Las presentaciones de pestañas, selector de perfiles y controles están en Kotlin/Compose. Los controles reciben datos/callbacks del host y reutilizan los glifos de BruniO TV. La navegación se despliega encima del contenido con temporización de hover; no cambia el espacio reservado a la página.

## Validación y límites

- 37 pruebas Python pasan con fixtures y catálogo M3U local.
- Hay pruebas Kotlin definidas para hover y condiciones de botones VOD/series/Live; no ejecutadas.
- Gradle se descargó, pero este entorno no resuelve el plugin Kotlin. No se consiguió compilar ni ejecutar Compose aquí.
- No se ha probado contra la cuenta personal ni en Windows. No se afirma paridad completa con TV.

Se añadió integración de VLCJ con cuadros de vídeo en Compose, controles de transporte, fuentes, selección de pistas de audio/subtítulos, fullscreen, selección de episodios, retry, reanudación, progreso y solicitud de siguiente episodio. Este código nativo NO se ha compilado ni probado aquí; rendimiento y ciclo de vida siguen pendientes de validación en Windows. En desarrollo requiere VLC x64 instalado o BRUNIO_VLC_HOME; todavía no se incluye su runtime en un instalador.

Los ajustes básicos de audio, subtítulos, límite de archivo para PC, autoplay y retry tienen formularios y servicios conectados. La traducción completa de la interfaz sigue pendiente.

Se añadieron servicios y vistas para cola de descargas, temporadas, biblioteca agrupada, cancelación, eliminación y reproducción offline; chat/reacciones de Party y solicitudes de acceso; administración y prioridad de complementos; y comprobación, descarga verificada y apertura del instalador. Las transferencias mantienen las URLs sólo en memoria. La biblioteca de desarrollo está en `BruniO/PCNext/Downloads`; no migra ni modifica las descargas del cliente anterior. El código de actualización reserva la versión `2.0.0-rc1` y rechaza como actualización las versiones anteriores de PC. No se ha publicado esa versión.

Todavía pendientes de implementación: sincronización del vídeo de Watch Party, suscripciones deportivas del calendario, PiP y multiview, notificaciones, edición de perfiles y otros ajustes avanzados. También faltan empaquetado autónomo del servicio Python/VLC, pruebas completas y revisión visual en Windows. Las acciones pendientes muestran un aviso explícito. No publicar ni distribuir como versión final.

## Desarrollo

Para pruebas del servicio: `python -m unittest discover -s desktop/brunio-pc-next/tests -v` desde la raíz del repositorio.

Para desarrollo de la interfaz: desde `compose/`, ejecutar `gradle run` con Gradle 8.14.3, JDK 17 y Python disponible como `python`. `BRUNIO_PYTHON` permite indicar el ejecutable y `BRUNIO_BRIDGE` la ruta absoluta a `backend/bridge.py`. No hay empaquetado autónomo todavía.
