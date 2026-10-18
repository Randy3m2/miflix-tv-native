BruniO TV RC88 — English priority, búsqueda flexible y retoque visual

INSTALACIÓN
Reemplaza los 32 archivos sobre RC87 conservando sus rutas y genera el build habitual de GitHub.
Versión 2.0.0-rc88 / versionCode 22087. Sin SQL, Edge Functions ni despliegue de Vercel.

INGLÉS
Con Audio = English, la selección automática y su cadena de hasta diez intentos colocan primero las fuentes identificadas con audio inglés, independientemente de resolución o tamaño. Si hay cinco fuentes inglesas distintas disponibles, los primeros cinco intentos son ingleses. Si hay menos, se prueban las existentes antes de fuentes sin idioma identificado y otras disponibles. No repite enlaces para completar cinco intentos artificialmente. La elección manual explícita se conserva.
Los idiomas declarados como audio tienen prioridad sobre el nombre del archivo. Reconoce contenido bilingüe con inglés, sin confundir subtítulos ingleses con audio inglés; también reconoce polaco como otro idioma. Si las pistas reales de una fuente anunciada como inglesa tienen todos sus idiomas declarados y ninguna es inglesa, el player intentará avanzar automáticamente. Pistas sin idioma declarado no demuestran que falte inglés; los datos del proveedor pueden ser incompletos. Se conserva la prioridad española y el aislamiento de Watch Party/Live TV.

LOAD MORE
Nueva tarjeta oscura con acento del tema, icono + y enfoque visible, sin animaciones de tamaño que muevan otras tarjetas. También se pulen los accesos de paginación de géneros, My List y Navidad. Se conservan los lotes de 25 y los accesos agregados en RC87.

SHARED MOMENTS
Acceso como tarjeta con icono y subtítulo de actualización manual. Bandeja con encabezado, nuevo acceso Refresh y tarjetas recibidas con avatar del amigo disponible en caché, portada si ya existe en los catálogos cargados, título, episodio, intervalo y duración. Sin portada cargada muestra un símbolo de reproducción; no se añaden búsquedas de metadatos por cada momento. Play destacado; Remove permanece disponible. Abrir la bandeja no consulta solicitudes: los momentos se cargan únicamente con Refresh manual.

BUSCADOR
Tolerancia a letras intercambiadas (Avengres / Jhon Wick), acentos, espacios (starwars), palabras parciales y artículos habituales. Coincidencias exactas primero. Si la consulta normal no obtiene un resultado cercano, hace hasta dos consultas alternativas a TMDB, conservando el debounce y la caché. No repite variantes ante un fallo de red en la consulta principal; el filtro Kids sigue aplicándose. No añade consultas a Supabase.

VERIFICACIÓN
Ejecutados EnglishAudioEvidenceCheck y los 31 casos de StreamLanguagePriorityCheck en JVM. Ocho verificaciones existentes de regresión/integración aprobadas. Nuevos componentes, tarjetas de momentos, ordenación y búsqueda analizados sintácticamente. Casos JUnit añadidos para primeros cinco intentos ingleses, audio bilingüe/subtítulos, ausencia de inglés y errores comunes de escritura; pendientes de ejecución con Gradle. APK y validación visual/reproducción en TV pendientes: este entorno no tiene Android SDK/Gradle wrapper.

PRUEBA EN TV
- Audio English: abre Playback links y comprueba la selección automática frente a fuentes 4K en otros idiomas y fuentes inglesas 720p/1080p. Prueba también una lista sin inglés y una fuente inglesa bilingüe.
- Busca Avengres, Jhon Wick y starwars; las coincidencias exactas deben mantenerse primero cuando escribas el título correcto.
- Recorre Home hasta Load more y prueba sus accesos en catálogos/My List. Revisa Shared moments, pulsa Refresh y reproduce/elimina un momento recibido.
