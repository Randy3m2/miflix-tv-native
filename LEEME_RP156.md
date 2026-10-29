# BruniO RP156 · Voz, navegación y búsqueda integrada

Paquete acumulativo de fuentes, incluye los cambios RP153–RP155. No es un APK.

## Instalación

Copia la carpeta `app` sobre la del repositorio conservando los demás archivos. Ejecuta tu workflow habitual de GitHub. Versión 2.0.0-rp156, versionCode 22260 / Build 156.

## Cambios

- Pulsar el icono AI inicia el dictado sobre la pantalla actual. Fire TV abre directamente el teclado del sistema; si otra TV no tiene reconocimiento de voz también ofrece teclado. El enfoque por sí solo no inicia captura.
- Cancelar cierra la entrada sin cambiar la pantalla. Completar el dictado o enviar desde el teclado aplica la búsqueda. Las búsquedas generales abren los resultados en el buscador unificado; las rutas y controles locales admitidos se ejecutan directamente. Canales/partidos con una sola coincidencia se reproducen; con varias se muestran opciones.
- Open Standings abre Live → liga → Table, fuera de la tarjeta del asistente.
- Se eliminó la fila de tiempos. Carátulas más compactas y títulos completos con ajuste de línea.
- «Busca películas que duren más de 2 horas» aplica mínimo 121 minutos; «menos de 2 horas» aplica máximo 119. El catálogo y la duración real del título se filtran.
- Scene Finder está dentro de la misma consulta, sin formulario independiente. Ejemplo: «Episodio de Naruto donde Gai dice ...». Usa títulos/sinopsis y, si activaste Gemini, una sugerencia de serie/episodio contrastada con el catálogo. Una coincidencia única de metadatos tras dictar puede reproducirse directamente; una propuesta de Gemini queda identificada como sugerencia y permite abrir el episodio con un botón.

## Límites reales

Scene Finder no tiene una base de transcripciones o audio del doblaje: no puede garantizar encontrar cualquier frase ni un minuto exacto. Las sugerencias del modelo no se presentan como prueba de que la escena esté en ese capítulo. Si no puede identificarlo, lo indica. La búsqueda de episodios está acotada a 40 temporadas cuando no hay una temporada sugerida, con concurrencia 3 y caché de seis horas.

Gemini sigue siendo opcional y conserva el límite local de 20 peticiones/día y una cada 20 segundos. No se añade un servicio de búsqueda de pago ni consumo de Supabase para Scene Finder. La clave y las cuotas del proveedor siguen aplicando.

El icono sigue siendo optativo: Settings → Assistant → BruniO Assistant ON. Las fuentes configuradas determinan los canales y partidos disponibles. Los recordatorios siguen siendo locales al perfil y dispositivo.

Falta probar el micrófono, IME, control remoto y diseño en una TV/Firestick físico.
