# BruniO TV · RP154 / Build 154

TV 2.0.0-rp154 / versionCode 22258. Paquete acumulativo con RP150–RP153.

## Aplicar

Copia la carpeta `app` sobre `app` de tu proyecto completo RP149–RP153, respetando las rutas. Conserva una copia de cambios propios. Sube los archivos y ejecuta **Build MiFlix Native TV APK** (`build-native-tv-apk.yml`). No contiene APK firmado, no se ha publicado nada en GitHub desde este chat y no requiere SQL ni dependencias nuevas.

## Buscador unificado

- El teclado existente permanece a la izquierda. BruniO AI y la búsqueda normal comparten lo que escribes, dictas, envías por QR o introduces con teclado físico.
- Junto al icono Bluetooth aparece el botón **✦ BruniO AI**. Cambia únicamente el modo del panel derecho y conserva la consulta. Vuelve a pulsarlo, usa Búsqueda normal o Atrás para regresar a resultados por título.
- El botón Bluetooth usa su símbolo y conserva la acción de abrir el emparejamiento de la TV.
- Los resultados aparecen a la derecha, sin abrir otra pestaña o ventana. La escritura no desplaza el foco del teclado a las carátulas.
- El botón AI de la barra superior abre este mismo buscador, con AI seleccionado; ya no abre un buscador separado.
- Las tarjetas de inspiración y las colecciones también rellenan la misma consulta. Se conservan modo invitado y acceso a Scene Finder.
- Kids y modo sencillo conservan su búsqueda habitual.

## Resultados automáticos

Desde dos caracteres, el modo AI espera aproximadamente 600 ms sin cambios y actualiza los resultados con filtros locales. Una nueva edición cancela el trabajo anterior y descarta respuestas atrasadas. Voz y QR utilizan el mismo proceso: no es necesario pulsar Buscar.

Si Gemini está configurado y activado, tras una pausa más larga de aproximadamente 1.8 segundos puede mejorar consultas de ocho caracteres o más. Respeta el límite existente de veinte peticiones diarias y veinte segundos entre peticiones. Durante la espera, o si no hay cuota disponible, sigue ofreciendo filtros locales. Cancelar detiene también la mejora automática pendiente de esa consulta.

La búsqueda normal mantiene su búsqueda automática existente por títulos/personas. No se añaden llamadas a Supabase ni polling nuevo. Las consultas de catálogo van a TMDB; Gemini recibe únicamente la consulta enviada cuando corresponde.

## Conservado de RP153

Compatibilidad de claves AQ. y claves largas, cifrado con Android Keystore, confirmación de escritura, errores seguros, fondo negro y controles separados. Gemini se configura desde Settings → BruniO AI.

## Prueba en TV

1. Abre el buscador. Comprueba el icono Bluetooth y ✦ BruniO AI juntos sobre el teclado.
2. Escribe un título y alterna AI/normal: el texto debe conservarse y los resultados cambiar a la derecha.
3. Dicta una consulta o introdúcela por QR: comprueba que se actualice sin pulsar Buscar.
4. Escribe rápido, borra y cambia la consulta: los resultados anteriores no deben sustituir los nuevos ni mover el foco.
5. Abre AI desde la barra superior y comprueba que aparece el mismo teclado.

Ver validación en VALIDACION_RP154.txt. No se ha probado físicamente en TV, ni con dictado real ni con una clave real de Gemini.
