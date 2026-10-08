# BruniO TV Native RC10 · Home focus fix

Proyecto completo acumulativo. Cambios limitados a la navegación de Home.

- Desde el menú flotante, flecha abajo lleva explícitamente al botón Ver detalles del banner; la flecha arriba conserva la búsqueda normal de foco hacia el menú.
- El listado reserva los 76 dp del menú en su área visible. El desplazamiento por foco usa los límites reales del área disponible, sin imponer el margen de 82 dp a las filas horizontales.
- Se conserva el regreso al inicio del banner cuando recibe foco, el menú flotante y todos los cambios anteriores.

Actualiza el repositorio y ejecuta Build MiFlix Native TV APK. No hay SQL nuevo, cambios de Pages ni cambios de Downloader. Si aún no has aplicado `supabase_rc6_upgrade.sql`, sigue siendo necesario para la eliminación de perfiles.

Comprobar en TV: menú → abajo → Ver detalles → abajo → filas; arriba hacia el banner y hacia Inicio; izquierda/derecha en las filas. Sintaxis y paquete verificados aquí; este entorno no permite compilar Android ni probar el mando real.
