BruniO RP49 — Others → Nuvio Test
Aplicar sobre RP48. Versión 2.0.0-rp49 (22153).

- Añade Nuvio Test como tarjeta en Others.
- Usa la colección importada: Sports Replays. Contiene 8 deportes y 48 subcategorías, no un listado de canales en directo.
- Categorías: Football, Motorsport, Baseball, Rugby, Basketball, Tennis, Hockey y American Football. Dentro están Today, Yesterday, This Week y las demás subcategorías del archivo.
- Cuadrícula con las imágenes de cada categoría y evento; OK o pulsación larga abre un selector lateral manual de enlaces nativos.
- Conserva el manifiesto configurado original, sus IDs y sus parámetros de idioma. No reemplaza la fuente de deportes existente.
- Nuvio Test aparece como proveedor correcto dentro del listado lateral del reproductor. Los controles de Enlaces y el cambio de evento conservan el selector manual.
- Atrás desde la reproducción regresa a Nuvio Test y recuerda deporte/subcategoría por perfil durante la sesión. Dentro de un deporte, Atrás vuelve a los deportes. Desde la raíz, Atrás vuelve a Others.
- Cargar más evita duplicados y se detiene si la fuente no avanza. Caché local de la primera página durante 5 minutos, con máximo 12 categorías guardadas. No hay refresco automático.
- Corrige la construcción de las rutas de add-ons cuyo manifest.json lleva una query: conserva la query al solicitar catálogo y stream. Los manifiestos sin query mantienen las rutas anteriores.

Validación:
5 tests JUnit: importación/deduplicación, fuente configurada y rutas con query/extra/fragmento. Código real del repositorio con HTTP simulado: 8 deportes, 48 catálogos, IDs de eventos, paginación, URL con token y encabezado Referer intactos. Sintaxis Kotlin y rutas duplicadas comprobadas.
La fuente devolvió HTTP 403 desde este entorno al probar el manifiesto y un catálogo. No se pudo comprobar contenido remoto ni reproducción real. Si sucede en TV, la sección muestra el error y permite reintentar. No se omiten bloqueos de la fuente.
No se ejecutó Gradle ni se generó APK. Falta probar el APK y las respuestas de la fuente desde tu TV.
Sin SQL ni Vercel. Copiar conservando rutas.
