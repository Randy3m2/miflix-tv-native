# BruniO TV Native 2.0 RC7

Proyecto completo, acumulativo: incluye todos los cambios de RC6.

1. Si no ejecutaste todavía `supabase_rc6_upgrade.sql`, ejecútalo en Supabase SQL Editor para habilitar la eliminación de perfiles. Si ya lo ejecutaste, no hay SQL adicional.
2. Reemplaza los archivos del repositorio y ejecuta **Build MiFlix Native TV APK**.
3. Instala el APK actualizado con la misma firma. El código de Downloader y la URL permanente permanecen iguales.

No se requieren cambios en Pages ni en la función de Trakt.

## Cambios finales

- Ajustes → Almacenamiento → Eliminar caché. Vacía la caché de imágenes de Coil en memoria y disco, usando su API para conservar válido el almacén. La operación se ejecuta fuera del hilo de interfaz, evita ejecuciones simultáneas y muestra confirmación al terminar. Conserva preferencias, sesión, perfiles, favoritos, progreso, puntuaciones y complementos. No es un restablecimiento de la app. Las imágenes pueden descargarse otra vez inmediatamente si hay solicitudes en curso.
- El fondo compartido de Friends, Party, perfiles y Ajustes conserva su textura suave, ahora en escala de grises con un velo gris carbón. El filtro se aplica al fondo, sin desaturar avatares, carátulas ni controles.

Consulta `RC6_SETUP.md` para perfiles, galería, idioma y correcciones de pantalla.

## Validación

Sintaxis Kotlin/XML/SQL, recursos y ZIP comprobados. No se compiló Android ni se probó el control en una TV en este entorno. Verifica la build de GitHub y, al instalar, el botón de caché y el nuevo fondo.
