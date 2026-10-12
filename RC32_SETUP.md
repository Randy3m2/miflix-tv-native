# RC32 — Filtros, franquicias y Friends & Party

Versión **2.0.0-rc32**, versionCode **22030**. Proyecto acumulativo sobre RC31. Conserva las funciones anteriores; esta versión no cambia el reproductor ni la app de PC.

## Instalación

Sube el contenido del ZIP a la raíz del mismo repositorio, incluyendo `.github/workflows`, y ejecuta **Build MiFlix Native TV APK**. Conserva los secretos de firma actuales y el token de TMDB. No hace falta crear otro repositorio, añadir secretos ni ejecutar SQL nuevo. Las migraciones anteriores siguen siendo necesarias si todavía no estaban instaladas.

La actualización usa el mismo applicationId y un versionCode mayor. Para actualizar sin desinstalar, la APK instalada y la nueva deben tener el mismo certificado de firma; RC32 conserva el flujo de firma permanente.

## Dónde está cada cambio

### Collections → Filtro personalizado / Custom filter

Combina películas o series, actor, director, género, año inicial/final, puntuación mínima y orden por popularidad, puntuación o fecha de estreno. Todos los filtros elegidos se aplican juntos. Para un año exacto, escribe el mismo año en ambas casillas. Los años vacíos no limitan ese extremo del rango.

Actor y director abren un buscador con foto y títulos conocidos para seleccionar a la persona correcta. El teclado mantiene el comportamiento de activación mediante OK. El filtro de director usa créditos de dirección, no cualquier participación como productor o guionista. En series incluye aquellas con créditos de dirección de episodios para esa persona; el año es el estreno original de la serie.

Los resultados permiten cargar más, editar y limpiar filtros. Al entrar en una ficha y volver se conservan los filtros, los resultados y la posición de la cuadrícula durante la sesión. Las carátulas son compactas, con espacio entre columnas y margen inferior.

### Collections → Franquicias / Franchises → Marvel o DC

Dos tarjetas abren catálogos con selector **Películas / Series**, paginación y carátulas. Se combinan las asociaciones de compañías y palabras clave de TMDB y se eliminan duplicados. Incluyen contenido asociado a Marvel/DC, no solo sus universos cinematográficos más recientes. La cobertura depende de los metadatos de TMDB; no se promete una filmografía exhaustiva.

No se añaden pestañas a la navegación superior. Al volver desde una ficha se mantiene la posición del catálogo.

### Friends & Party

Cada pulsación de la pestaña actualiza amigos, actividad y solicitudes, incluso si ya estabas dentro. También permanece el botón Actualizar. Las pulsaciones durante una carga se agrupan en una actualización posterior, evitando lanzar varias cargas simultáneas desde estos controles. La lista no incorpora un temporizador de refresco continuo.

## Validación

Se revisaron la sintaxis Kotlin, navegación, cambios frente a RC31, referencias de versión y contenido del ZIP. No hay SDK ni compilador Android disponibles en este entorno: **la compilación debe completarse en GitHub Actions y la interacción necesita prueba en la TV**.

Prueba un filtro de actor + género + rango de años y otro de director, abre una ficha y vuelve. Comprueba las opciones Películas/Series y Cargar más de Marvel y DC. Finalmente pulsa Friends & Party varias veces, tanto desde otra sección como desde la propia pestaña, y comprueba que la actividad se actualiza y el foco sigue siendo utilizable.
