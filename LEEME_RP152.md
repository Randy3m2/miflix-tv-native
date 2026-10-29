# BruniO TV · RP152 / Build 152

Rediseño de BruniO AI. Versión 2.0.0-rp152, versionCode 22256.

## Aplicar

Copia la carpeta `app` del ZIP sobre la carpeta `app` de tu proyecto, respetando las rutas y aceptando reemplazos. Este paquete es acumulativo: incluye RP150 y RP151, y se puede aplicar sobre RP149, RP150 o RP151. Conserva una copia de tu proyecto si tienes cambios propios.

Después sube los archivos y ejecuta el workflow **Build MiFlix Native TV APK**, en `build-native-tv-apk.yml`. No contiene APK firmado y no se han publicado cambios remotos desde este chat. No necesita SQL ni nuevas dependencias.

## Cambios

- Pantalla de AI con fondo azul/violeta, controles compactos y foco blanco sin cambios de tamaño al navegar.
- Buscador con micrófono al lado, botón Buscar y QR. Dictar utiliza el reconocimiento de voz del sistema y respeta el idioma de voz del perfil. Rellena el campo; pulsa Buscar para enviar la consulta. Si el sistema no ofrece dictado, abre el QR para usar el celular.
- Ocho tarjetas con imágenes para explorar: viajes en el tiempo, terror de los 90, ciencia ficción con acción, finales inesperados, comedia, romance, animación con acción y documentales cortos. Se pueden recorrer con el control remoto.
- Carátulas del catálogo desde la pantalla inicial y resultados en cuadrícula, con duración, episodios y acciones para continuar. Se prioriza el póster vertical; se excluyen resultados sin póster ni imagen de fondo.
- Gemini se configura y activa desde **Settings → BruniO AI**. Ya no hay botones de configuración en el buscador. La clave cifrada existente se conserva. Activa Gemini desde Settings si quieres usarlo; los filtros locales siempre están disponibles.
- Miniaturas al recorrer la barra del reproductor **siempre ON**, incluso si antes estaban guardadas en OFF. Se mantienen el límite de una extracción simultánea y la caché de imágenes pequeñas. Su disponibilidad depende de la fuente y de la TV: no se inventa una imagen si el vídeo no permite extraerla.
- Se conservan modo invitado, Scene Finder, colecciones, franquicias y votaciones de RP151. Scene Finder sigue buscando títulos/sinopsis, sin minuto exacto verificado.

## Consumo

El rediseño reutiliza imágenes del catálogo cargado; si falta una imagen para una tarjeta visible consulta su ficha en TMDB. No consulta Gemini al abrir la pantalla ni mientras dictas. No añade polling ni consultas a Supabase. Se mantienen los eventos habituales de fichas/reproducción/party. Las miniaturas pueden usar datos de vídeo de la fuente durante la navegación por la barra.

No se añade un modelo de IA descargado a la TV. La disponibilidad, gratuidad y cuotas de Gemini dependen de tu proyecto de Google: usa Free Tier sin facturación para evitar cargos. La clave permanece cifrada en este dispositivo.

## Comprobar en la TV

1. Abre ✦ AI: revisa las tarjetas con imágenes, el foco y los resultados al elegir una idea.
2. Dicta una consulta con el micrófono junto al campo, revisa el texto y pulsa Buscar.
3. Abre Settings → BruniO AI para configurar o activar Gemini; vuelve al asistente.
4. Busca un punto en un vídeo con izquierda/derecha; espera la miniatura. OK confirma el salto y Atrás cancela.

La validación de compilación y pruebas se detalla en `VALIDACION_RP152.txt`. No se ha probado físicamente una TV, el dictado ni una clave real de Gemini en este entorno.
