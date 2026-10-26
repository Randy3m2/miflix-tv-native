# BruniO Mobile M1 · RP133

Primera versión nativa para teléfonos Android, basada en BruniO RP132.
Versión estable: `1.0.0-m1-rp133` · código `22237` · Android 8.0 o superior.

## Aplicar al repositorio existente

1. Extrae este ZIP encima del repositorio completo actualizado a RP132, conservando las rutas.
2. No borres `app`, sus recursos, assets ni los archivos de revisiones anteriores. Este ZIP es un update, no un proyecto completo.
3. En GitHub Actions ejecuta **Build BruniO Mobile APK**. Reutiliza los secrets existentes de TMDB y la firma permanente de BruniO.
4. Descarga el APK firmado del artifact del workflow o del canal `mobile-latest`.

El workflow prepara la firma y el paquete verificados antes de publicar. No se ejecutó ni publicó ninguna release en GitHub desde esta sesión.
La app para TV conserva su módulo `:app`; el nuevo módulo de teléfono es `:mobile`.

## Instalaciones independientes

- Estable: `com.brunio.mobile`, canal `mobile-latest`, archivo `BruniO-Mobile.apk`.
- Preview: `com.brunio.mobile.preview`, firmada con clave de depuración. No sustituye TV ni la futura app estable. No instala actualizaciones de producción.
- TV: `com.miflix.native2`, canal `native-latest`.

El APK Preview adjunto permite probar la interfaz y entrar con tu cuenta. Al no llevar el secret de TMDB del repositorio, necesita que tu cuenta ya tenga su configuración privada de catálogo guardada. El workflow de release sí incorpora el secret de TMDB existente.
Los perfiles, amigos, progreso y ajustes que ya sincroniza BruniO usan la misma cuenta; cada instalación conserva además sus datos locales independientes.

## Incluido

- Home táctil con Seguir viendo como primera fila de contenido, hero, plataformas, Top 10, franquicias, variedad de filas y Cargar más.
- Búsqueda con teclado del teléfono y voz; español/inglés, actores y directores; espera 2 caracteres y entrega resultados por grupo.
- Detalles con sinopsis, reproducción, selección manual de fuente, Mi lista, tráiler, Watch Party, pill de amigos y episodios horizontales con vistos.
- Conservación de las coordenadas de episodios y presentación completa para DBZ, Naruto y Shippuden.
- Live TV con favoritos, logos, búsqueda, categorías y 10 canales por carga visible. Reabrir el selector conserva categoría y posición de reproducción.
- NBA, MLB, F1, LIDOM y Others: fuentes deportivas existentes, partidos publicados, tablas, líderes, jugadores/pilotos y suscripciones al calendario.
- Reproductor compartido Media3: controles táctiles, barra de avance, audio/subtítulos, cambio de fuente, siguiente episodio y rotación. El cambio de fuente mantiene el video mientras eliges.
- Indicador de audio no detectado/mute. Grabaciones deportivas mantienen pausa y avance.
- SplitScreen de hasta 4 señales, una audible. Dos videos apilados en vertical; Stats para 2 o para el cuarto espacio cuando hay 3. En horizontal, Stats se coloca en su propia zona. F1 usa fotos; NBA/MLB/LIDOM usan logos y tablas.
- Fuentes YouTube de LIDOM mediante el reproductor embebido existente; su disponibilidad sigue dependiendo de los permisos de incrustación del video.
- Amigos y Party: actividad con refresh manual, detalles, acceso, creación/unión a sala y chat; el motor de sincronización es el existente.
- Calendario con equipos/ligas seguidos, logos por fecha y notificaciones del sistema existente.
- Perfiles, idioma, tamaño de texto, modo fácil, audio primario/secundario, subtítulos, límite de GB, precarga, TorBox y administración de add-ons por URL.
- Modo fácil con filas dedicadas y salida manteniendo el logo 15 segundos. Modo Kids conserva las restricciones existentes.
- Descarga de archivos directos compatibles mediante Android Download Manager, desde la selección manual y el reproductor. No descarga HLS/DASH como si fueran archivos completos ni temporadas enteras.

## Alcance y validación

La interfaz adapta los conceptos aprobados a componentes táctiles; las imágenes conceptuales anteriores no son capturas del APK.
La transmisión de pantalla abre la función de mirroring de Android; no se añadió un receptor Google Cast dedicado.
No se probó en un teléfono físico ni se verificó reproducción contra cada proveedor. Se requiere esa prueba para evaluar decodificación, rendimiento real, orientación, notificaciones y disponibilidad de streams. No se garantiza ausencia de bugs.

Consulta `VALIDACION_MOBILE_M1_RP133.txt` para los resultados concretos de compilación y pruebas.

## Compilar localmente

Con JDK 17, Gradle 8.13, Android SDK 36 y Build Tools 36.0.0:

```sh
gradle :mobile:testDebugUnitTest :mobile:assembleDebug -Pkotlin.incremental=false
```

APK Preview: `mobile/build/outputs/apk/debug/mobile-debug.apk`.
Para release usa el workflow con la firma permanente. Los archivos de entorno, SDK, cachés y claves de depuración no están incluidos en el update.
El compilador incremental mostró conflictos de símbolos al reconstruir fuentes compartidas en este entorno; la validación completa se ejecuta sin incremental. La compilación limpia de CI no depende de los artefactos locales.
