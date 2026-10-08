# BruniO TV Native RC8

Proyecto completo acumulativo: incluye RC6 y RC7.

1. Si aún no ejecutaste `supabase_rc6_upgrade.sql`, ejecútalo para habilitar la eliminación de perfiles. Si ya lo hiciste, no hay nueva migración.
2. Reemplaza los archivos del repositorio, ejecuta Build MiFlix Native TV APK e instala el APK.
3. Pages, Trakt y el código de Downloader no requieren cambios.

## Interfaz compacta

- Navegación horizontal superior con perfil a la izquierda y campana de avisos a la derecha. La fila se desplaza con el control cuando no caben todas las opciones.
- Inicio, Buscar, Colecciones, Amigos y salas, TV en vivo, Mi lista y Ajustes.
- Géneros está dentro de Colecciones; se mantienen plataformas, próximos estrenos y años.
- Amigos y salas abre el mismo directorio con pestañas Amigos, Salas y Solicitudes. Crear sala / Mi sala permite gestionar tu sesión. Se elimina la entrada separada de Party y el botón duplicado para regresar a Friends.
- Solo el botón con foco tiene fondo blanco. La selección persistente de un idioma, pestaña o sección se distingue mediante borde gris, sin mantener otro botón blanco.
- Más separación entre acciones y grupos de Ajustes que se distribuyen en varias filas si hace falta.
- Portada inicial más compacta con márgenes y esquinas suaves.

Se conservan la limpieza de caché, fondo gris, idioma español/inglés, gestión de perfiles y todos los cambios anteriores.

## Validación

Se verifican sintaxis Kotlin/XML/SQL, recursos y ZIP. No hay Android SDK/Gradle disponible en este entorno: la build y la comprobación visual con el control deben hacerse en GitHub y en la TV.
