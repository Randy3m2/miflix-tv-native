BruniO Web · Catálogo, idioma y calendario

1. Reemplaza web/index.html, web/app.js y web/style.css en GitHub. Conserva web/brunio-logo.png. Vercel desplegará el cambio. Los archivos usan v=5 para renovar la caché.
2. Supabase > Edge Functions > brunio-catalog > Code: reemplaza TODO index.ts por supabase/functions/brunio-catalog/index.ts y pulsa Deploy. Mantén Verify JWT desactivado; la función valida la sesión internamente. Usa tu secreto TMDB_TOKEN existente.
3. Recarga la web. No se necesita otro APK. No vuelvas a ejecutar el SQL de favoritos.

Idioma: selector muestra el idioma actual. Interfaz, títulos, sinopsis, géneros y solicitudes al catálogo usan ES/EN; las cachés incluyen el idioma y los resultados antiguos no sustituyen los nuevos. Los nombres propios y títulos de eventos escritos por usuarios no se traducen automáticamente. TMDB puede carecer de una traducción y devolver un título original o sinopsis vacía.

Catálogo: Próximamente muestra películas/series con fecha de estreno posterior a hoy, según TMDB; no significa que estén disponibles para reproducción. Explorar permite género, año, tipo, puntuación/popularidad/fecha y personas (reparto/equipo). En Mi lista se filtra género/año/tipo y se ordenan los títulos cargados; usa Ver más para cargar el resto. Selecciona un resultado de la búsqueda de personas para activar ese filtro.

Plataformas: Netflix, Apple TV (suscripción), Prime Video, HBO Max, Disney+ y Crunchyroll. Logos desde TMDB; disponibilidad desde JustWatch según región seleccionada (US por defecto, igual que TV). Usa los mismos filtros dentro de cada colección. Los catálogos pueden variar por país y no equivalen a contenido exclusivo de la marca.

Eventos: botón Eventos > Crear evento, o desde detalles de un título. Elige película/serie, fecha y hora, temporada/episodio cuando corresponda, y amigos (máximo 20). La hora se interpreta en la zona horaria del dispositivo, indicada en el formulario, y se guarda en UTC. Supabase aplica las políticas de amigos aceptados. El evento aparece en el calendario de TV del creador y de sus invitados al actualizarlo. No se envían correos. Solo el creador puede eliminar un evento, previa confirmación.

Se reutiliza miflix_calendar_events y las políticas existentes RC22/RC23. Si ya funciona el calendario de TV, no ejecutes SQL adicional. Si la web informa que falta la tabla, ejecuta primero optional_sql/SUPABASE_RC22_CALENDAR.sql y después optional_sql/SUPABASE_RC23_CALENDAR.sql.

Validación: pruebas con HTTP simulado de idiomas/caché, filtros, próximas fechas, personas; pruebas de controles web y datos de eventos. No se probaron cuentas reales ni el navegador desplegado. Comprueba guardar un favorito y un evento desde la web y actualizar Mi lista/Calendario en TV.
