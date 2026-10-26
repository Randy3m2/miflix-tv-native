# BruniO Mobile M2 / RP134

Actualización de fuentes sobre Mobile M1 / RP133. Mobile: 1.0.1-m2-rp134 / 22238. TV: 2.0.0-rp134 / 22238.

## Instalar

1. Copiar el contenido del ZIP sobre la raíz del repositorio completo conservando las rutas y hacer commit. No borrar los demás archivos.
2. Ejecutar `supabase_rp134_calendar_sync.sql` una vez en el SQL Editor de tu proyecto Supabase. Reutiliza la tabla existente con RLS; no elimina datos.
3. GitHub Actions → Build BruniO Mobile APK → Run workflow. Usa los mismos secrets y firma permanente de M1.
4. Para sincronizar equipos/ligas seguidos, generar e instalar también TV RP134 con Build MiFlix Native TV APK. Ambas apps deben usar la misma cuenta y perfil.

No requiere redesplegar Vercel ni modificar brunio-downloads.

## Cambios

- VOD, Live y SplitScreen entran automáticamente en horizontal y ocultan las barras del sistema; al salir se restaura la orientación anterior.
- El botón de llenar pantalla alterna ajustar/zoom. Llenar puede recortar bordes para eliminar bandas negras.
- Controles compactos: título superior, pausa/avance central, progreso inferior y menú de opciones. Se mantienen fuente, audio, subtítulos, episodios, descarga, Party y demás acciones.
- AMC+ usa su logo local ya incluido en el proyecto.
- Multiview usa TextureView en móvil, recibe toques sobre el video, asigna identidades nuevas al reemplazar/reintentar y conserva un solo audio. Incluye Reintentar señal.
- El calendario móvil carga los mismos eventos compartidos que TV, muestra fechas locales y tiene actualización manual.
- Las suscripciones deportivas se sincronizan con una función por cuenta/perfil y marcas de borrado, sin sondeo continuo. Las fuentes de calendarios externos personalizadas permanecen locales.
- Límite de GB guardado solo para este celular; no importa ni sobrescribe el límite de TV. Se conserva la política existente de tamaños desconocidos y excepciones por proveedor.
- Sección Descargas en la navegación móvil con portadas guardadas localmente cuando están disponibles, progreso del sistema, reproducción local, reintento y eliminación confirmada.
- Películas y episodios se agrupan por título/temporada. Descargar temporada seleccionada prepara los episodios estrenados de esa temporada y muestra cuántos quedaron en cola o sin enlace descargable.

## Descargas

Se guardan archivos completos directos, incluyendo enlaces CDN con nombre de archivo en sus metadatos. No se descargan manifiestos HLS/DASH como si fueran videos ni se convierten torrents por cuenta propia. Se reutiliza la preparación nativa existente cuando corresponde.
Los archivos y su registro quedan en este dispositivo y perfil; no se sincronizan a TV. Las portadas remotas de TMDB se guardan localmente si la descarga de imagen funciona.
La cola ya enviada a Android continúa gestionada por el sistema. La preparación de enlaces de una temporada necesita que el proceso de la app siga vivo; si se interrumpe, puede volverse a ejecutar sin duplicar archivos activos o completos.
Una URL temporal puede caducar en la cola; Reintentar descarga vuelve a buscar una fuente. No se promete compatibilidad con todo proveedor/códec.
Las descargas hechas en M1 carecían de identificación de película/episodio; no se importan automáticamente al nuevo catálogo. Los archivos anteriores no se borran.

## Validación

Ver VALIDACION_MOBILE_M2_RP134.txt. La prueba de teléfono físico sigue siendo necesaria para evaluar rotación, rendimiento y límites de decodificadores, almacenamiento, red y reproducción real.
El SQL se entrega para ejecución en tu proyecto; no se desplegó desde esta sesión.
