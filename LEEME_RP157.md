# BruniO RP157 · Buscar por descripción y filmografía

Paquete acumulativo de fuentes RP153–RP157. No es APK.

## Instalar

Copia la carpeta `app` sobre la de tu repositorio, conservando los demás archivos. Ejecuta tu workflow habitual. Versión 2.0.0-rp157, versionCode 22261 / Build 157. Activa Settings → Assistant y configura Gemini para la identificación por descripciones.

## Episodios

Se retiró el botón independiente «Find a scene». La escena se pide directamente al asistente por voz o por texto.

Pulsa AI y di «Episodio de Dragon Ball de la pesadilla de Vegueta». Gemini propone la serie correcta de la franquicia y el título del episodio, corrigiendo nombres de dictado. La app consulta TMDB y contrasta títulos tanto en inglés como en el idioma del catálogo. Los números propuestos por el modelo no bastan para aceptar una coincidencia: la app conserva la numeración canónica del catálogo.

Una coincidencia única de una solicitud dictada o enviada desde la entrada rápida abre los detalles del episodio y carga su temporada. Varias coincidencias se muestran para elegir. El botón de los resultados abre también los detalles del episodio. La identificación de escenas ya no inicia reproducción automática.

La pantalla existente del episodio muestra imagen, título, sinopsis y acciones (Play, elegir enlace, añadir a cola…). Los resultados sugeridos por Gemini muestran que la escena no está confirmada. Los detalles se revelan porque el usuario los ha solicitado; no se cambia la preferencia global de spoilers.

## Personas e historias

- «Películas donde sale Tom Holland», «Movies starring Tom Holland»: consulta créditos reales de actuación en TMDB.
- «Series con Pedro Pascal»: filtra créditos a series.
- «Películas dirigidas por Christopher Nolan»: consulta créditos de dirección.
- «Películas con Tom Holland, menos de 2 horas»: comprueba duración de los resultados.
- «Película donde un hombre revive el mismo día», «¿Cómo se llama la película de un…?»: Gemini propone hasta tres títulos; se comprueban título original/localizado, tipo y año en TMDB.

Para una identificación única desde la entrada rápida se abren los detalles con un aviso de posible coincidencia. Con varios resultados se muestran carátulas y sinopsis para elegir. La búsqueda por persona conserva una elección explícita si hay homónimos; no toma la primera persona de la lista.

## Consumo y límites

La ruta del asistente es TV → Gemini / TMDB. No se añadió una tabla, Edge Function, registro de conversaciones ni polling de Supabase. Consultar estas búsquedas genera cero llamadas nuevas a Supabase por la búsqueda en sí. Las tareas normales de la app, historial, cuentas, Watch Party o sincronización pueden seguir generando llamadas al usarse; esto no equivale a cero logs para toda la app.

Las búsquedas por actor/director usan TMDB y caché de 15 minutos, sin necesitar una consulta a Gemini. Los catálogos de episodios se cachean seis horas, con concurrencia tres y límite de 40 temporadas por serie; Gemini conserva el límite local de 20 solicitudes/día y 20 segundos entre solicitudes. Las cuotas y tarifas efectivas de la API pertenecen al proyecto de Google del usuario.

Gemini interpreta la descripción y propone candidatos; esta versión no activa Google Search grounding ni tiene una base completa de transcripciones/doblajes. No se garantiza identificar cualquier escena. Una coincidencia de título en TMDB comprueba la existencia del episodio, no prueba el contenido de la escena. No se inventan streams ni minutos exactos.

No se hicieron llamadas reales con la clave personal de Gemini ni pruebas físicas en TV/Firestick.
