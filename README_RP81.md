# BruniO RP81 — Actores, directores y búsqueda bilingüe

`2.0.0-rp81` · versionCode **22185**. Parche sobre **RP80**; copiar respetando las rutas. No incluye APK ni requiere SQL/Vercel.

## Ambos modos

El buscador original compartido por el modo normal y el modo fácil ahora consulta también personas de TMDB y sus créditos. Al escribir un nombre reconocido puede mostrar hasta **120 películas/series**, según su filmografía disponible, en lugar de limitarse a los títulos cuyo nombre se parece al del actor/director.

Ejemplos:

- `Tom Hanks`, `Brad Pitt`, `Penélope Cruz`.
- `Christopher Nolan`, `Guillermo del Toro`.
- `Películas de Tom Hanks`: filtra películas.
- `Series con la actriz Penélope Cruz`: filtra series y créditos de actuación.
- `Películas dirigidas por Christopher Nolan`: solo créditos de dirección de películas.
- `Muéstrame las películas del director Steven Spielberg`.
- También admite frases equivalentes en inglés.

Una consulta explícita por actor no mezcla créditos de producción/dirección; una consulta explícita por director solo acepta el trabajo `Director`. Un nombre sin indicar función permite actuación y dirección. Se excluyen noticias/talk shows de TV para no llenar la filmografía de entrevistas. Las restricciones Kids existentes se conservan.

Hay comparación sin tildes, con errores pequeños y una pista fonética limitada para nombres pronunciados en español, por ejemplo `Cristofer Nolan`. Si no encuentra el nombre completo, puede intentar apellido y nombre por separado, pero el candidato debe seguir coincidiendo con el nombre completo. Como máximo se consultan créditos de dos personas, para consultas ambiguas. Un título exacto de una sola palabra conserva prioridad sobre personas coincidentes; no se transforma toda búsqueda en una filmografía.

## Títulos escritos en español o inglés

Se conserva el idioma de la interfaz. Si no aparece una coincidencia cercana de título se consulta también español latino (`es-MX`) o inglés (`en-US`), según corresponda. Los títulos traducidos sirven como alias para ordenar resultados; cuando existe el mismo título en la respuesta del idioma principal se conserva esa ficha primero. Continúan los reintentos limitados por variaciones del título.

No es traducción automática ni un índice completo de todos los alias: depende de las traducciones y coincidencias publicadas por TMDB.

## Micrófono

El teclado incorpora **ES / EN** junto a Voz, para ambos modos. La selección se guarda localmente por cuenta/perfil y no cambia el idioma de la interfaz, del audio ni de los subtítulos.

- ES solicita dictado latino con `es-MX`.
- EN solicita `en-US`.
- Sin una elección guardada se toma español latino para la interfaz española, o inglés para la interfaz inglesa. Se puede seleccionar ES aunque la interfaz esté en inglés.
- El sistema puede devolver hasta cinco transcripciones. Se utiliza la primera y, si entrega más de una, se muestran alternativas bajo el campo para elegir la correcta sin repetir el dictado.
- El aviso del micrófono permite decir un título, actor o director. Una respuesta de dictado iniciada en otro perfil se descarta.

La disponibilidad y precisión real dependen del servicio de voz instalado en la TV; algunas TVs devuelven una sola transcripción o no ofrecen dictado. No se ha probado el micrófono físico en este entorno.

## Rendimiento y consultas

Las consultas son de metadatos, directamente a TMDB; no se consultan los add-ons de video para buscar actores ni se envían estas búsquedas a Supabase. El dictado lo maneja el servicio de voz del dispositivo.

Se usa caché breve y acotada, cancelación al cambiar la consulta, límite de dos personas por búsqueda y espera limitada para la búsqueda de personas/títulos. Las consultas por actor/director pueden necesitar más peticiones de TMDB que una búsqueda de título. No se añade polling ni se reactivan Friends/Party en modo fácil.

Se conservan el Home variado de RP80, MLB conectado al add-on de deportes, Live TV y el gesto de 60 segundos sobre BruniO para salir del modo fácil.

## Verificación realizada

- PASS: compilación Java y ejecución de `verification/SearchQueryRP81Check.java`: frases ES/EN, filtros de tipo/función, tildes, normalización fonética limitada y selección del idioma del micrófono.
- PASS: `python3 verification/SearchRP81WiringCheck.py`: integración estática de créditos/roles, búsqueda bilingüe, alternativas de voz, caché, límites y aislamiento por perfil/idioma.
- PASS: comparación de AppState con RP80; el código fuera de búsqueda y preferencias de voz se conserva idéntico.
- Inspección sintáctica del buscador, modelos nuevos y bloques nuevos de búsqueda/personas sin errores del parser. No equivale a compilar Kotlin.

**No se compiló Android, no se generó APK y no se realizaron consultas reales de TMDB ni pruebas de dictado en TV. Falta validar compilación y comportamiento real.**

Prueba sugerida: buscar actor y director en ambos modos; verificar que `dirigidas por` no incluya trabajos de productor; buscar un título español con UI inglesa y viceversa; probar ES/EN y transcripciones alternativas; comprobar otro perfil y regresar para confirmar que conserva su elección de micrófono.
