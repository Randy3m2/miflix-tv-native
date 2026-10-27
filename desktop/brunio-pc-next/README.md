# BruniO PC: reconstrucción en desarrollo

Este directorio NO es una versión final ni un instalador. No reemplaza todavía al cliente anterior.
Móvil (`mobile/`), TV (`app/`) y sus flujos de compilación quedan fuera del alcance.

## Estado comprobable

- Puente local por stdio para cuenta, perfiles, catálogo, detalles y búsqueda, reutilizando el protocolo existente.
- No devuelve tokens ni configuración privada de complementos a la interfaz.
- Seis pruebas Python del puente pasan.
- Módulo independiente Kotlin/Compose y banco visual de navegación. Sus botones sólo cambian la selección en el banco de desarrollo.
- Temporización del hover separada de los componentes para evitar aperturas accidentales y cierres al cruzar espacios.

## Pendiente antes de una entrega

Conectar interfaz y puente; migrar perfiles, inicio, búsqueda, detalles, series, biblioteca, TV en vivo,
multiview, calendario, amigos, Watch Party, descargas, ajustes y actualización.
Integrar motor de vídeo, audio, subtítulos, fuentes, retry, autoplay y progreso por perfil.
Comprobar rendimiento, accesibilidad, caché, instalador y actualización reales en Windows.
Eliminar el cliente anterior únicamente cuando todo lo anterior tenga reemplazo comprobado.

No publicar en el canal de actualización ni presentar este banco como una aplicación terminada.
