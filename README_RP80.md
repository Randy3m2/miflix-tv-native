# BruniO RP80 — Más variedad en Modo fácil

Versión `2.0.0-rp80` · versionCode `22184`.
Parche sobre **RP79**. Copia los archivos respetando las rutas. No incluye APK; no requiere SQL ni Vercel.

## Buscador original

Modo fácil vuelve a utilizar `SearchScreen / CinematicSearchScreen`, el mismo buscador del modo normal: teclado en pantalla, dictado, borrar/espacio, flechas para mover el cursor, sugerencias de títulos y carátulas de resultados. Ya no utiliza el campo simplificado que abría otro teclado.

Solo en Modo fácil se amplían las letras del teclado (16 sp frente a 13 sp, además de la escala del modo), el alto de sus teclas y el texto/campo de búsqueda. El teclado lateral pasa de 220 a 250 dp para dejar espacio a las letras. El modo normal conserva sus medidas.

Los resultados del modo fácil siguen abriendo los detalles sencillos y no permiten acciones avanzadas por pulsación larga. La barra mantiene Home, Buscar y Mi lista, conserva Live TV y añade MLB; se mantienen las protecciones de RP79.

## Home con variedad

Se conservan las dos filas populares y se añaden 24 selecciones: **26 filas de catálogo**, además de **Seguir viendo** cuando exista progreso.

- Películas populares y series populares.
- Novelas, comedias para desconectar y clásicos.
- Películas y series mejor valoradas, estrenos en cartelera.
- Acción, aventuras, comedia, romance y drama.
- Suspenso, ciencia ficción y fantasía.
- Familia y animación.
- Crimen, misterio y películas del oeste.
- Documentales y dramas coreanos.
- Series de crimen, drama y comedia.

Cada selección muestra hasta 20 títulos, según lo que devuelva TMDB. Las novelas y comedias para desconectar reutilizan los catálogos existentes, con sus títulos destacados. Puede aparecer una misma película en más de un género, como en el Home normal.

Las filas adicionales se cargan después de abrir el Home, con hasta tres cargas de fila simultáneas, y aparecen progresivamente con claves estables. No retrasan el arranque esperando todos los catálogos. Las carátulas se muestran a medida que sus tarjetas entran en las listas visibles.

Se reutilizan las filas terminadas al volver al Home en el mismo perfil e idioma. Al cambiar de perfil/idioma se descartan las filas de la selección anterior. Salir del Home cancela las cargas pendientes; al volver se retoman las que faltan. Si una fuente falla o devuelve vacío, aparece **Cargar las filas pendientes** para reintentar solo las que no llegaron. No se sustituyen filas vacías por resultados genéricos.

## Live TV, MLB y salida del modo fácil

La barra superior queda en **Home, Buscar, Mi lista, Live TV y MLB**. Live TV conserva el listado de canales y categorías. MLB es un acceso separado a la sección de béisbol del **mismo add-on de deportes configurado en la app**, usando `calendarLiveManifest("sports")`; no instala otra fuente ni cambia su dirección.

MLB reutiliza las tarjetas/eventos, categorías y carga de más eventos existentes. Al pulsar un partido reproduce su primer enlace y conserva el mecanismo existente de hasta cinco opciones automáticas cuando el inicio falla. Atrás vuelve al área MLB; el acceso Live TV abre los canales normales. La selección de área queda separada por cuenta/perfil en memoria. En Modo fácil no se abre el selector manual mediante pulsación larga; el modo normal conserva esa opción.

Para salir del modo fácil, mantén **OK sobre BruniO durante 60 segundos completos y suelta**. Solo entonces se abre la confirmación existente. El contador es local, no realiza consultas. La repetición de eventos del control no reinicia ni completa el contador. Soltar antes, mover el foco, pulsar otra tecla, salir de la pantalla o mandar la app a segundo plano cancela la pulsación. También se admite mantener pulsado con ratón/táctil. Sigue siendo una interfaz simplificada, no una protección con PIN.

## Consultas y funciones conservadas

Las nuevas filas consultan directamente metadatos de TMDB. No consultan Supabase, Friends, Party, Trakt ni las fuentes de reproducción para rellenar el Home. No se añade ningún temporizador de refresco. Los métodos existentes de novelas/comedias pueden realizar varias peticiones dentro de una carga de fila.

Se conservan sin cambios las protecciones de RP79, la copia local de progreso, las sincronizaciones necesarias al salir/finalizar y la renovación de sesión. El reproductor básico y Deepflix/Mandanguita para VOD siguen igual. MLB sí realiza las consultas necesarias al add-on de deportes al entrar o reproducir. Los controles de otros perfiles y del modo normal no se modifican.

No se ha medido un ahorro concreto de logs. Más variedad supone consultas e imágenes de catálogo adicionales, pero no reactiva el trabajo social en segundo plano.

## Validación

- PASS: `python3 verification/EasyHomeRP80Check.py`: 24 filas adicionales con claves únicas, tipos de carga válidos, buscador original, ampliación limitada a Modo fácil y protecciones de caché/propietario/cancelación conectadas. Son comprobaciones estáticas.
- PASS: comparación de AppState contra RP79: todo el código anterior queda idéntico fuera del nuevo bloque de carga de filas y selección de MLB.
- PASS: compilación Java y ejecución de `verification/EasyExitHoldRP80Check.java`: frontera exacta de 60 segundos, repetición, pulsación interrumpida, teclas distintas y activación solo al soltar.
- La comprobación estática también verifica que MLB reutilice el add-on de deportes y que la salida larga use el temporizador nuevo.
- Inspección sintáctica: archivos UI modificados/nuevos y bloque nuevo del cargador sin errores del parser. No equivale a compilación Kotlin.

**No se ha compilado Android ni generado APK. Falta comprobar compilación, resultados reales de TMDB y navegación/foco con el control en TV.**

Para probar: activar Modo fácil; comprobar teclado original/letras, dictado y búsqueda; recorrer las filas hasta el final; abrir un título y volver sin recargar las filas terminadas; comprobar Seguir viendo; abrir MLB y comprobar su add-on, autoplay y regreso al salir del partido; mantener el logo 59 segundos y soltar (no debe abrir nada), repetir durante 60 segundos y soltar (debe abrir confirmación); cancelar al perder foco o poner la app en segundo plano; confirmar que el buscador normal conserva su tamaño.
