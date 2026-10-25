BruniO RP122 / 22226 / 2.0.0-rp122
Aplicar sobre RP121 (incluye sus dos hotfixes), conservando rutas. Sin SQL ni APK.

CALENDARIO
- Flechas anterior/siguiente juntas al mes. Acciones separadas a la derecha.
- Letras de los días centradas y con el mismo espaciado de sus columnas en ambas vistas.
- La caché mensual y general incluye todas las suscripciones y la fuente opcional: añadir o quitar otro equipo/otra liga invalida la caché anterior.
- Seguir equipos continúa descargando en el ámbito de la aplicación aunque se cierre el panel. Las descargas se serializan; las suscripciones se modifican de forma sincronizada.
- El calendario observa cambios en las suscripciones para actualizar también el mes visible, sin bucle de recarga por cada actualización de datos.
- Una liga que falle no marca la caché como completa por seis horas; conserva datos previos y permite reintentar.
- Se conserva el soporte de múltiples equipos/ligas, logos, avisos por perfil y las fuentes actuales. La disponibilidad final depende de los horarios publicados por cada fuente.

TABLE
- En la vista integrada elimina el título Standings repetido y el pie técnico.
- Oculta el botón de grupo cuando solo existe un grupo (como LIDOM).
- Menos espacios, encabezado compacto y filas de 38 dp con logos de 28 dp; nombres de una línea con elipsis.
- Se conserva el desplazamiento para toda la tabla, temporadas y selectores de conferencias/divisiones.

VALIDACIÓN
8 pruebas JUnit de regresión pasaron: nueva liga, segundo equipo, quitar equipo, migración de caché, orden de suscripciones, expiración/refresh, cambio de fuente y seguimiento simultáneo de ligas.
Revisión de cambios y ZIP. No se compiló Android/Compose ni se probó el foco/fluidez en TV; pendiente validar en el dispositivo.
