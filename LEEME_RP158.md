# BruniO RP158 · Assistant Tools

Paquete acumulativo de fuentes RP153–RP158. No es APK. Copia `app` sobre el repositorio, conserva los demás archivos y ejecuta el workflow habitual. VersionCode 22262, versión 2.0.0-rp158 / Build 158.

Activa Settings → Assistant. Gemini necesita su propia clave y el interruptor de Gemini encendido para las funciones generativas. Los comandos locales no necesitan una llamada a Gemini.

## Qué incluye

| Función | Comportamiento de esta versión |
| --- | --- |
| Análisis de contenido | Pulsar AI desde Details analiza ritmo, intensidad, acción y concentración. Estimación sin spoilers basada en sinopsis, géneros y duración; no analiza el video completo. Resultado local en caché siete días. |
| Universal Navigator | «Abre mis favoritos de Live TV» abre la sección de favoritos del navegador de canales. Conserva las rutas anteriores del asistente. |
| Voice Bookmarks | «Guarda esta escena como favorita», o «Guarda esta escena como transformación de Goku», guarda episodio, posición real, duración y huella de la fuente localmente por perfil. |
| Scene Jump | Botón de escenas dedicado en el reproductor, junto a AI, sólo con el asistente encendido. Busca etiquetas de marcadores/índices reales. Puede usar Gemini para elegir una etiqueta existente; nunca le acepta un minuto inventado. Un único resultado compatible salta directamente. |
| Home Designer | «Pon Continue Watching arriba y mueve Streaming debajo de los géneros» cambia las filas conocidas. Muestra confirmación y Deshacer. Mantiene Continue Watching como primera fila según la política existente del Home. |
| Live TV Zapper | «Quiero ver canales de documentales» busca categorías y nombres de tus fuentes configuradas. Ofrece canales reales y conserva esa selección en la navegación del reproductor. También reconoce infantiles y deportes. |
| Multiview Assistant | «Abre Multiview con dos partidos que estén en vivo» consulta marcadores NBA, MLB y LIDOM y resuelve dos fuentes deportivas antes de cambiar la vista. Si no hay dos identificables, conserva la vista actual. |
| Franchise Builder | Usa títulos de películas y series de la franquicia disponibles en el catálogo. Gemini propone su orden narrativo y la app verifica que no agregue ni omita IDs de esa selección. Se puede guardar la lista local. |
| Visual Assistant | «¿Qué significa esta opción?» captura temporalmente la interfaz actual y la manda a Gemini para explicarla. En Settings, Profiles, Friends, Watch Party y Notifications usa explicación textual de una opción nombrada, sin enviar captura de esas pantallas. La imagen no se guarda. |
| Watch History | Consulta registros locales por rango y género. Desde RP158 conserva hasta 500 sesiones por perfil; para fechas anteriores usa el último progreso disponible. «La semana pasada» corresponde a la semana calendario anterior en la zona del dispositivo. |
| Smart Autoplay | «Cuando termine este episodio, pon uno de Dragon Ball Super» permite elegir la serie, verifica un episodio estrenado de la temporada inicial y lo pone primero en la cola existente. Se puede cancelar quitándolo de Cola. Respeta las restricciones de Party. |
| Content Detective | Conserva RP157: Gemini identifica candidatos por descripción y la app comprueba títulos/tipo/año en TMDB; abre Details para coincidencias únicas enviadas desde la entrada rápida. |
| Personal DJ | «Ponme trailers de películas de acción» prepara títulos filtrados y usa la sesión continua de tráileres existente. El siguiente se reproduce al terminar; puedes avanzar o salir. Depende de la disponibilidad de tráileres/YouTube. |
| Personalized Home Events | «Durante Halloween, pon películas de terror primero en mi Home» activa una regla local de octubre que sube Horror y Seasonal después de Continue Watching. Fuera de octubre recupera el orden guardado sin sobrescribirlo. «Desactiva Halloween en mi Home» la cancela. |
| Watch Order by Storyline | «Quiero ver solamente los episodios relacionados con la historia de Itachi» propone hasta 25 títulos de episodios y los contrasta con títulos reales en inglés o en el idioma del catálogo, conservando numeración canónica. Ordena los hallazgos por temporada y episodio y permite guardar la lista. |

Las frases son ejemplos: hay detección local de intenciones y Gemini puede dirigir otras formulaciones a las herramientas permitidas. Las funciones que necesitan seleccionar una serie, canal o resultado ambiguo muestran opciones. Escribir por sí solo no ejecuta cambios; se ejecutan las solicitudes completadas desde voz/teclado rápido o al pulsar Buscar.

## Escenas indexadas

La vista Mis escenas permite guardar el minuto actual con una etiqueta e importar un índice JSON para el video activo:

```json
[{"label":"Primera transformación de Goku","positionMs":60000}]
```

El valor es un ejemplo del formato, no un minuto real de Dragon Ball. Debes aportar los tiempos reales de tu versión. Se validan límites y se asocian al contenido, episodio, duración y huella de la fuente actual. El número de marcadores está limitado a 100 por perfil.

Un marcador incompatible no aplica un salto. Puedes abrir el contenido y pedir aplicar el marcador al cargar; sólo salta si la fuente y duración coinciden. Cambios de fuente, edición o URL firmada pueden impedirlo. El minuto sigue visible para consulta. Los Party invitados no pueden controlar el salto.

## Listas e historias

Guardar lista temática crea una lista local del perfil. Di «mis listas AI» para abrir sus entradas. Las listas por personaje y las cronologías son sugerencias de Gemini; comprobar un episodio en TMDB demuestra que existe, no que todo el episodio pertenezca al arco. No se promete cobertura completa. Se consultan hasta 40 temporadas y hasta 25 títulos sugeridos por lista temática; la franquicia usa hasta 50 títulos de su selección cargada.

## Consumo

Sin nuevas tablas, Edge Functions o polling de red de Supabase para estas herramientas. Marcadores, historial adicional, listas y regla de octubre se guardan en este dispositivo y perfil. La comprobación de la fecha es local. La cola y las funciones existentes conservan sus comportamientos habituales, incluida la sincronización normal de la app.

Las llamadas generativas van directamente a Gemini y la información del catálogo, a TMDB. Continúa el límite local de 20 llamadas generativas al día; se permite una segunda llamada vinculada a una misma solicitud interpretada, contándola dentro de esas 20. Entre solicitudes independientes se conserva la espera de 20 segundos. Las cuotas/tarifas reales dependen del proyecto de Google. No se activa Google Search grounding ni una base de transcripciones.

No se verificó una clave personal de Gemini ni el control remoto, captura visual, micrófono o reproducción en una TV/Firestick físicos.
