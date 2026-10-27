# BruniO PC: reconstrucción en desarrollo

Este directorio NO es una versión final ni un instalador. No reemplaza todavía al cliente anterior.
Móvil (`mobile/`), TV (`app/`) y sus flujos de compilación quedan fuera del alcance.

## Estado comprobable

- Puente local por stdio para cuenta, perfiles, catálogo, detalles y búsqueda, reutilizando el protocolo existente.
- No devuelve tokens ni configuración privada de complementos a la interfaz.
- Seis pruebas Python del puente pasan.
- Módulo independiente Kotlin/Compose con presentaciones para Inicio, Buscar, Mi lista, Live TV, Amigos & Party, Calendario, Descargas, Notificaciones, Ajustes y selector de perfiles.
- Controles visuales de reproducción basados en `app/.../PlayerControls.kt`: título/fuente, progreso, play/pause, episodios, canales, CC, audio, enlaces, Party, chat, reacciones, cola, información, PiP, maximizar, scores y multiview. Se reutilizan los glifos de BruniO TV sin modificar su archivo original.
- Los componentes reciben datos y callbacks del host; aún NO están conectados al puente, a un motor de vídeo o a los controladores de esas funciones. No hay reproducción funcional en este módulo.
- La galería de desarrollo permite inspeccionar pestañas y controles VOD/Live. Muestra explícitamente que no tiene motor conectado. No contiene títulos, canales o progresos simulados como contenido real.
- Pruebas Kotlin definidas para las condiciones de botones VOD/series/Live y los tiempos en milisegundos. Todavía no ejecutadas: Gradle no resuelve el plugin Kotlin en el entorno local.
- Temporización del hover separada de los componentes para evitar aperturas accidentales y cierres al cruzar espacios.

## Pendiente antes de una entrega

Conectar interfaz y puente; migrar perfiles, inicio, búsqueda, detalles, series, biblioteca, TV en vivo,
multiview, calendario, amigos, Watch Party, descargas, ajustes y actualización.
Integrar motor de vídeo, audio, subtítulos, fuentes, retry, autoplay y progreso por perfil.
Comprobar rendimiento, accesibilidad, caché, instalador y actualización reales en Windows.
Eliminar el cliente anterior únicamente cuando todo lo anterior tenga reemplazo comprobado.

No publicar en el canal de actualización ni presentar este banco como una aplicación terminada.
