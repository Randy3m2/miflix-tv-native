BruniO RC102 — Hecho para ti / Made for you
Aplicar sobre RC101 conservando rutas. 21 archivos. No incluye APK.

SELECCIÓN VISUAL DE GUSTOS
- Pantalla de bienvenida con pósteres reales y fotos de actores: Películas, Series, Novelas, Actores y Géneros.
- Tarjetas con foco ampliado, borde de selección y marca ✓; contador, búsqueda manual y botón Omitir.
- Hasta 30 títulos y 12 actores. Un título elegido en varias categorías cuenta una sola vez.
- Aparece una vez por perfil no infantil, al entrar a Home por primera vez tras instalar este parche. Se puede omitir. Los perfiles existentes también pueden usar esta primera selección una vez.
- Settings > Account / Appearance > Tu universo, tus gustos permite repetir/editar, encender o apagar la personalización, reiniciar el aprendizaje y controlar si se ofrece la bienvenida a nuevos perfiles.
- Al crear un perfil se entra a su Home para ofrecer la selección, si está habilitada.
- Kids conserva su catálogo y filtros infantiles; no se le presenta este catálogo de adultos.

RECOMENDACIONES Y APRENDIZAJE
- Selección independiente por cuenta/perfil en esta TV. Elegir una carátula no marca el título visto, no cambia My List y no crea una valoración.
- Combina títulos, actores y géneros elegidos con las recomendaciones existentes basadas en valoraciones e historial.
- Aprende interés local por hitos de tiempo realmente reproducido; adelantar al final sin ver no genera una señal positiva. Los registros locales cambian solo al cruzar hitos, no en cada actualización de progreso.
- Catálogos públicos y actores en caché local durante 24 horas; recomendaciones personalizadas en caché hasta 6 horas y se recalculan cuando cambian los gustos/aprendizaje. Consultas limitadas a TMDB al abrir/confirmar/cargar contenido, sin polling nuevo de Supabase.
- La selección local y el aprendizaje se incluyen en el backup manual. Las configuraciones de add-ons no se exportan porque sus URLs pueden contener claves.

NOVELAS CERCA DEL INICIO
- Fila Novelas que atrapan / Telenovelas to fall for inmediatamente después de Browse by genre y antes de For you.
- Incluye Pasión de gavilanes, Café con aroma de mujer (2021 y original de 1994), Sin senos no hay paraíso, Yo soy Betty, la fea y Tierra de reyes cuando TMDB entrega sus fichas; amplía con títulos relacionados en español y novelas del catálogo.
- Las versiones se resuelven por título exacto y año; no se usan IDs inventados.
- Entrada al catálogo desde Load more y siguientes tandas de 25 con el búfer único ya existente.
- Se puede mover u ocultar como las demás filas. Un orden personalizado anterior recibe la fila cerca de Browse by genre; después puedes moverla.

ADD-ONS AL CREAR PERFILES
- Dos opciones visibles: Usar add-ons del principal (por defecto) o Configuración propia.
- La configuración propia empieza sin add-ons; añadir/quitar en un perfil secundario no modifica la lista principal.
- Settings > Stream Add-ons permite alternar entre heredar y usar la lista propia; alternar no borra la lista propia anterior.
- La elección y los add-ons propios se guardan por cuenta/perfil en esta TV. En otros dispositivos deben configurarse de nuevo; no se añade SQL ni sincronización adicional.
- Los perfiles antiguos siguen heredando la lista principal hasta que el usuario elija otra cosa. El motor de fuentes nativas mantiene su configuración de cuenta existente.

MOODS CON FOTOS
- Tarjetas más grandes con fondos fotográficos del catálogo ya cargado, degradados, iconos distintos, borde claro y selección marcada.
- Reutiliza imágenes/caché de Coil y los metadatos existentes; enfocar un mood no solicita imágenes a Supabase.
- Conserva la lógica RC96 de Dazzle Me y la ventana persistente de trailers.

VALIDACIÓN
- Compiladas y ejecutadas pruebas JVM aisladas sobre la clase real TasteProfileRC102 y los métodos de add-ons extraídos del código de producción, con adaptadores de Android/Compose para pruebas.
- Probados perfiles independientes, omitir, límites, selección sin valorar/marcar visto, hitos de visualización, ausencia de escrituras por tick, caché, backup y preservación de add-ons propios/principales.
- Compilados y ejecutados en JVM los métodos nuevos de novelas/actores con respuestas TMDB de prueba: títulos/años correctos, deduplicación, filtrado de relacionados en español, paginación, caché y fotos de actores.
- Comprobaciones estáticas de integración y gramática de los archivos nuevos. Esto no equivale a compilar la aplicación Android completa.
- Pendiente compilación Android y validación visual/foco en TV, fotos y resultados reales de TMDB y comportamiento al cambiar entre perfiles.

Sin build/APK, SQL ni despliegue Vercel. Conserva RC101 (Canal anterior y correcciones anteriores).
Incluye app/src/main/... y main/java/... como los parches anteriores; usa las rutas correspondientes a tu proyecto.
