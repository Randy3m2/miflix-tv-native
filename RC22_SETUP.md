# BruniO TV RC22 · 2.0.0-rc22 / 22020

## Activación

1. En Supabase → SQL Editor ejecuta **SUPABASE_RC22_CALENDAR.sql** una vez. Es repetible. Solo añade la tabla privada de calendario, su validación de amigos aceptados y políticas RLS; no cambia salas ni datos existentes.
2. Sube el contenido al repositorio conservando desktop y el workflow PC. Ejecuta **Build MiFlix Native TV APK** en main.
3. Instala encima con la misma firma estable o actualiza desde la app después de publicar. Downloader mantiene el mismo enlace.

## Dónde está cada función

| Función | Ubicación |
| --- | --- |
| Amigos y salas durante el video | Reproductor → Party. Actualizar carga la actividad; Solicitar acceso conserva la aprobación del host. Crear sala requiere pulsar el botón correspondiente. |
| Calendario | Amigos/Party → Calendario, o Reproductor → Party → Calendario. |
| Cola | Detalles → Añadir a cola / Ver cola; reproductor → icono Cola. |
| Episodio concreto a cola | Detalles de serie → selecciona episodio → Añadir a cola; o Añadir a cola → elige temporada y episodio. |
| Desde el principio / continuar | Detalles de película/serie y diálogo al seleccionar un episodio en detalles. La pulsación larga para enlaces se conserva. |
| Hora estimada de finalización | Barra del reproductor y panel de pausa, calculada con posición y velocidad actuales. No hay estimación para Live TV ni duración desconocida. |
| Sinopsis siguiente | Panel al finalizar, debajo del siguiente capítulo o contenido de la cola. |
| Skip Intro destacado | Diálogo transparente por encima del video con foco directo en Saltar introducción, cuando IntroDB devuelve un tramo válido. Back lo cierra sin saltar. |

## Cola

Cola persistente en esta TV, por cuenta y perfil, hasta 100 contenidos. Se puede mover arriba/abajo, eliminar y vaciar. La reproducción manual nunca la borra. Al terminar cualquier contenido no live, si existe cola empieza un contador de 10 segundos; se puede cancelar o editar. Solo se retira una entrada cuando se resuelve su enlace y se inicia esa solicitud; si no hay enlaces permanece en la cola. Sin cola se conserva el autoplay habitual de series. En Party, el host decide la secuencia y los invitados siguen el contenido compartido; la cola del guest no cambia el contenido de la sala. Cola/calendario de esta RC todavía no tienen interfaz de gestión en PC.

## Calendario

Vista mensual y filtro por día. Películas/series se seleccionan con búsqueda TMDB; series requieren temporada y episodio. Juegos tienen título libre, no catálogo ni ejecutor de juegos. Fecha/hora: YYYY-MM-DD HH:mm en la zona horaria de la TV; se guarda en UTC. Hasta 20 amigos aceptados invitados. Las invitaciones aparecen dentro del calendario de los invitados al Actualizar. Solo el organizador elimina un evento. No se crean automáticamente salas ni se envían mensajes externos; no hay notificaciones push/recordatorios en esta RC.

## Audio privado en el celular: pendiente

No se incluyó un botón sin funcionalidad. Para mantener video en TV y audio sincronizado en el teléfono hace falta capturar/transportar audio decodificado, emparejar por QR y compensar latencia; debe probarse en TV y teléfono reales. No basta con abrir el mismo enlace en el navegador (formatos, pistas, buffering y sincronización varían). Dos audífonos conectados al mismo celular dependen de su soporte de audio dual; la app no puede habilitarlo en hardware que no lo tenga. Consulta PRIVATE_LISTENING_DESIGN.md para el planteamiento.

## Verificación

Validada sintaxis Kotlin e integridad del ZIP. El calendario pasó pruebas locales de migración repetible, lectura organizador/invitados, bloqueo de no amigos y falsificación del organizador, y restricción de edición/borrado a terceros. La compilación se hará en Actions. Prueba en TV: foco de nuevos diálogos, Skip Intro, cola A/B/C con reproducción manual entre medias, reordenar/eliminar durante contador, enlaces fallidos, reinicio por perfil y Party host/guest. Comprueba eventos con dos cuentas amigas y otra ajena.
