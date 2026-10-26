# BruniO PC · Windows x64 · 1.0.0-rc5

Proyecto de la app de escritorio. Este paquete contiene código fuente y el workflow para compilar el instalador; no contiene un instalador ya compilado.

## Generar el instalador

1. Descomprime el ZIP conservando las carpetas. En el repositorio `Randy3m2/miflix-tv-native`, reemplaza `desktop/brunio-pc` con la carpeta incluida y actualiza `.github/workflows/build-pc.yml`. Conserva todos los demás archivos y workflows del repositorio.
2. En GitHub abre **Actions → Build BruniO PC Windows → Run workflow** en la rama que contiene esos archivos.
3. Al terminar, descarga el artifact **BruniO-PC-Windows-RC5** o el instalador de la release `pc-v1.0.0-rc5`.
4. Ejecuta `BruniO-PC-Setup-1.0.0-rc5.exe` en Windows 10/11 x64. No necesitas instalar Python o VLC por separado. El instalador no lleva firma comercial y Windows podría mostrar una advertencia.

El workflow verifica las pruebas, descarga y verifica VLC, construye el instalador y publica los canales de PC `pc-v1.0.0-rc5` y `pc-latest`. No modifica las releases de Android. Si la ejecución falla, revisa el log de Actions: no hay instalador listo hasta que el workflow termine correctamente.

## Uso y alcance

Inicia sesión con tu cuenta de BruniO. El catálogo, complementos, progreso y favoritos utilizan los datos de esa cuenta. El límite de tamaño de fuentes configurado en PC es local y no cambia el límite del móvil o TV.

La interfaz rediseñada incluye navegación superior, portada destacada, tarjetas de plataformas con los recursos de TV, búsqueda, detalles, Live TV, calendario, descargas y lista. Las carátulas de plataformas alojadas externamente necesitan conexión; AMC+, Hulu y Paramount+ se incluyen en la app. El reproductor usa VLC, con opciones de audio, subtítulos, selección de fuente y pantalla completa. Live TV incluye el catálogo M3U distribuido con la versión de TV y permite importar una lista local. Las personalizaciones y favoritos locales de TV no se sincronizan automáticamente con PC; la disponibilidad de cada señal depende de su fuente. Multiview permite hasta cuatro canales en vivo; el rendimiento depende de la PC y de los streams.

Las descargas admiten archivos directos compatibles; no guardan playlists HLS/DASH. Puedes descargar una película, un episodio o poner en cola episodios estrenados de una temporada, según las fuentes disponibles. La app debe permanecer abierta durante las descargas; si se interrumpen, vuelve a intentar desde Descargas. Las carátulas disponibles se guardan localmente y los archivos se separan por cuenta y perfil. Las descargas fallidas se reinician desde el principio.

El calendario consulta eventos del perfil y suscripciones deportivas compartidas con la TV. La sincronización de suscripciones requiere que `supabase_rp134_calendar_sync.sql` ya esté aplicado en el backend y que la TV/móvil use la versión compatible; en PC se consultan las suscripciones, pero todavía no se editan. La programación depende de los servicios deportivos externos. F1 muestra fechas de carreras, no todas las sesiones.

Amigos y Watch Party usan el esquema de perfiles RP1 del backend de TV. Chat, control y reproducción entre PC y TV requieren que el esquema y las funciones RP1 estén instalados y que cada participante tenga acceso a sus propias fuentes. Las listas M3U locales no se comparten en Party.

## Verificación y limitaciones

Se probaron el inicio de Qt en modo sin pantalla y contratos con datos simulados, incluidas descargas, listas y perfiles. No se ha compilado ni probado esta RC en una PC Windows real, ni se ha validado una Party con una TV real; por favor prueba instalación, reproducción, subtítulos, multiview y ambos sentidos de Party. Algunos textos y funciones siguen siendo diferentes de TV. Fuentes, códecs, disponibilidad y servicios externos pueden variar.

En desarrollo Windows: instala las dependencias de `requirements.txt`, ejecuta `python -m unittest discover -s tests -v` y usa `scripts/build_windows.ps1` para compilar con los requisitos indicados en el script. La sesión local utiliza Windows DPAPI; el proyecto no incluye las credenciales privadas del usuario.

## RC5 · reproducción y actualizaciones

La pantalla de perfiles aparece al iniciar. En Ajustes puedes cambiar el orden de complementos para audio español o inglés y activar el reintento de fuentes. La prioridad y el reintento son independientes por perfil y se guardan localmente en PC; no sincronizan las preferencias de orden con la TV. El selector prioriza el idioma cuando lo identifica en los metadatos y luego el orden elegido; los metadatos incompletos pueden afectar la selección. Si una fuente VOD falla o queda en carga, se intenta otra de las fuentes ya encontradas, hasta cinco intentos, conservando la posición cuando está disponible. El reintento automático se desactiva durante Watch Party y para archivos locales o canales en vivo. El autoplay del siguiente episodio puede activarse por perfil.

Ajustes muestra «Buscar actualización». Tras compilar y publicar una nueva versión de PC desde el workflow, el botón consulta `pc-latest`, verifica tamaño y SHA-256, y solicita confirmación antes de abrir el instalador. No instala una build que no haya sido publicada. La instalación y el actualizador necesitan una prueba real en Windows.

Esta RC incorpora mejoras concretas de TV, pero no incluye todavía todas sus funciones ni replica todos los detalles de su motor de selección. Las preferencias de audio y subtítulos y la prioridad de complementos son locales de PC.
