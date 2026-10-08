# BruniO TV — 2.0.0-rc5

Incluye todo lo anterior. Usa directamente este ZIP.

## Actualizar

1. En Supabase → SQL Editor, ejecuta **supabase_rc5_upgrade.sql**. Incluye RC3/RC4 y las tablas privadas de puntuaciones. Puede ejecutarse nuevamente. Requiere la configuración social y supabase_final_upgrade.sql de RC2.
2. Reemplaza los archivos en la raíz de tu repositorio con este ZIP.
3. Ejecuta **Build MiFlix Native TV APK** e instala la actualización. No hace falta desinstalar.

Si ya publicaste Pages con RC4, no necesitas volver a desplegar Pages para estos cambios. Si todavía usas una página anterior, ejecuta Deploy MiFlix Pairing Pages. Trakt y el enlace permanente/código de Downloader no cambian.

## Interfaz social

Watch Party separa crear sala y solicitar acceso en dos paneles. El QR tiene su área propia. Friends/Party tiene tarjetas con avatar, nickname, estado y contenido; cuatro columnas en TVs amplias, tres cuando el espacio es menor. Al seleccionar una tarjeta se puede Request access.

**Refresh** carga el directorio y las solicitudes. No se carga automáticamente al entrar, ni se reemplaza periódicamente mientras navegas. La presencia sigue enviándose en segundo plano, y la sincronización del video y el chat de la sala siguen activos. Solicitudes que tú envías pueden completar la entrada automáticamente tras la aprobación del host.

El fondo oscuro suave está incluido como imagen en el APK. El menú lateral reserva espacio para el perfil debajo de Settings y desplaza las opciones si la pantalla es pequeña.

## Campos de texto

Al recorrer cualquier campo con el control, solo recibe foco. Para escribir, pulsa **OK**: aparece un editor y el teclado. Usa **Listo** para cerrar el editor. Se aplica a búsqueda, login, contraseñas, manifiestos, códigos, nombres, nicknames y frases. Las contraseñas siguen ocultas.

## Coming Soon

Collections muestra películas y estrenos de series con fecha futura en los próximos doce meses, según TMDB. Las series corresponden a nuevas series; no representa un calendario completo de nuevas temporadas. Las fechas pueden cambiar y no garantizan disponibilidad en un add-on.

## Puntuaciones y recomendaciones

Dentro de un contenido, usa **★ Rate** y elige entre 1 y 10 estrellas. También hay un botón **Puntuar** al terminar. Puedes modificar tu puntuación después.

Las puntuaciones son internas de BruniO, separadas por perfil; no se envían a Trakt ni modifican la nota pública de TMDB. Con sesión iniciada se guardan en una tabla privada de Supabase y se cargan por perfil; sin sesión se guardan en el dispositivo.

El algoritmo toma hasta cuatro contenidos con puntuación alta como ejemplos positivos y hasta dos con puntuación baja como ejemplos negativos. Combina listas de contenidos similares de TMDB, pondera según tu nota y la posición en esas listas, reduce sugerencias parecidas a lo que puntuaste bajo y excluye contenidos ya evaluados. Una puntuación de 5 es neutral. En Home aparece **Para ti · tus puntuaciones** cuando hay resultados; también se personalizan las sugerencias al terminar. Es un algoritmo de ponderación, no un modelo de IA entrenado.

## Lenguajes del proyecto

- App Android TV: Kotlin, interfaz Jetpack Compose para TV, reproductor Media3/ExoPlayer.
- Páginas de QR y chat móvil: HTML, CSS y JavaScript.
- Función de Trakt: TypeScript.
- Base de datos: PostgreSQL y SQL en Supabase.
- Configuración y build: XML, Gradle/Groovy y YAML.

## Validación

Se comprobaron sintaxis Kotlin/XML/SQL y recursos; pruebas con PostgreSQL WASM verifican migraciones repetibles, ratings de 1 a 10, aislamiento entre cuentas y perfiles, avatares para amigos aceptados y las funciones anteriores del party. Se ejecutaron las pruebas del chat móvil y Trakt.

No se compiló el APK ni se probó en una TV desde este entorno. La build de GitHub confirmará compilación; verifica en TV el foco, desplazamiento, editor con OK y distribución.
