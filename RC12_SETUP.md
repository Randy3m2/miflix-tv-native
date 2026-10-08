# BruniO TV Native 2.0 RC12

Proyecto completo y acumulativo. Conserva las mejoras de RC11 y la navegación de Home de RC10.

## Reproductor

- Barra inferior fina, título y origen del enlace.
- Orden: Play/Pause, Zoom/Fit, Speed, Subtitles, Audio track, Playback links, Volume.
- Arriba a la derecha: X para salir y maximizar/restaurar. En TV, restaurar reduce ligeramente el área del vídeo; maximizar vuelve a ocupar la pantalla.
- Iconos blancos sobre vídeo; exclusivamente el botón con foco tiene fondo blanco e icono negro. Su etiqueta aparece al enfocarlo.
- Arriba desde Play se llega a la barra. Izquierda/derecha sobre la barra adelanta/retrocede 10 segundos; mantener pulsado repite. También admite tocar o arrastrar con ratón/touch. En directos no seekables la barra no cambia la posición.
- Subtítulos y audio muestran solo pistas compatibles del enlace actual, con selección individual y opción Off/Automatic.
- Velocidad 0.5–2×. Volumen del reproductor 0–100%, con +10%, −10% y mute; no cambia el volumen del sistema de la TV.
- Los menús tienen foco propio y evitan que se navegue por la pantalla de fondo.
- Para Episodios y Party, selecciona el título del contenido encima de la barra. Se conservan sus funciones sin añadir más iconos a la barra.
- Controles desaparecen tras 3 segundos de inactividad. Cualquier tecla vuelve a mostrarlos. La primera interacción tras pausar descarta la información de pausa, como antes.
- Se conservan sinopsis, hora y tiempo restante tras pausar, Skip Intro/Credits, autoplay, recomendaciones y selección independiente de enlaces del guest.

## Guest y pantalla negra al pausar

Se eliminan los saltos de posición mientras el host está pausado y mientras se espera confirmación de una pausa/reanudación del guest. Se conserva el fotograma y se reconcilia la posición al reanudar o durante reproducción. PlayerView mantiene el contenido al resetearse. La instancia ExoPlayer no se recrea al pausar.

Esto corrige un posible desencadenante identificado en el código. No se puede asegurar que resuelva una incidencia del decodificador o del enlace sin comprobarlo en la TV afectada.

## Instalación

1. Subir el contenido del ZIP al repositorio, conservando las rutas y reemplazando archivos.
2. Ejecutar Build MiFlix Native TV APK.
3. Instalar el APK como actualización. Mismo paquete Android y canal permanente native-latest/MiFlix-TV-Native.apk: no cambia el código de Downloader.
4. No hay SQL nuevo, ni función Supabase/Trakt o Pages que desplegar para RC12. Si ya aplicaste RC6, no repitas sus migraciones.

## Comprobación en TV

- Película y episodio: botones en el orden indicado, foco blanco individual, menús y retorno del foco.
- Barra: arriba desde Play, izquierda/derecha, mantener pulsado, luego abajo de vuelta a los botones.
- Subtítulos, audio, velocidades, volumen, zoom, restaurar/maximizar y salir.
- Host + guest: pausa desde guest, conservar imagen, pausa en host, reanudar desde ambos; confirmar que no se cambia el enlace ni se recrea el player.
- Repetir pausa/reanudación con dos enlaces distintos para distinguir un problema del stream/decodificador.

Se validaron sintaxis Kotlin, XML y contenido/integridad del ZIP. Pasaron las pruebas de base de datos de acceso privado y pausa/reanudación del guest; estas pruebas no verifican la imagen del decodificador. La compilación Android y la prueba visual/funcional en TV siguen pendientes: este entorno no tiene Gradle ni Android SDK.
