BruniO RC101 — Compilación + Canal anterior
Aplicar sobre RC100, con o sin el hotfix de minimumRating, conservando las rutas.

CORRECCIONES
- LocalExtrasRC100.stillLimit genera un setter JVM setStillLimit(Int). La función explícita se renombra a saveStillLimit y se actualiza su llamada en ajustes, eliminando la colisión.
- Incluye la corrección anterior: minimumRating se restaura con optInt y el valor por defecto del modelo.

CANAL ANTERIOR
- Botón integrado en la misma barra inferior de controles del reproductor, junto al selector de canales.
- Solo para solicitudes Live. No aparece en películas ni series.
- El nombre del destino aparece en la etiqueta al enfocar el botón.
- Alterna A ↔ B. Deshabilitado hasta tener un destino y para invitados de Watch Party.
- Actualiza el historial al renderizar el primer fotograma del canal, no al seleccionar o intentar abrir un enlace.
- Un cambio de fuente del mismo canal no desplaza el anterior. En LIDOM, las fuentes del mismo equipo se consideran una misma transmisión lógica.
- Si un tercer canal falla, el botón devuelve al último que sí comenzó a reproducirse.
- Historial en memoria por cuenta/perfil durante la sesión; se borra al cerrar sesión o apagar la opción.
- Settings > Playback > Live TV · Previous channel: ON por defecto, se puede apagar. Incluido en exportación de configuración; backups RC100 anteriores siguen aceptándose.
- Abre una nueva reproducción en vivo reutilizando la última fuente exitosa. No mantiene otro canal reproduciéndose en paralelo. Si esa fuente ya no responde, se conserva el manejo habitual de errores/enlaces.

DATOS Y VALIDACIÓN
Sin nuevas consultas de historial, tablas SQL, despliegue web ni polling Supabase. El cambio de canal conserva el tráfico habitual de reproducción y, si aplica, la sincronización existente de Watch Party.

Prueba ejecutada en JVM sobre la clase Java de producción: canal inicial, alternancia, actualización de fuente, tercer canal fallido, aislamiento de perfiles y borrado. Gramática de archivos Kotlin nuevos/modificados y auditoría de colisiones de setters de LocalExtras comprobadas; regresión estática RC100 y paginación RC98 verificadas.
No se generó APK ni build Android completo. Pendiente compilar y validar foco del mando/reproducción real en TV.

17 archivos. Incluye ambas rutas de los parches anteriores (app/src/main/... y main/java/...); utiliza las que correspondan a tu proyecto.
