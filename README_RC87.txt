BruniO TV RC87 — Recuperación, Christmas Movies y Load more

INSTALACIÓN
Reemplaza los 31 archivos sobre RC86 conservando sus rutas y ejecuta el build habitual de GitHub.
Versión 2.0.0-rc87 / versionCode 22086. Sin SQL, Edge Functions ni despliegue de Vercel.

REPRODUCCIÓN
- Corrige un caso que desactivaba el fallback con Español seleccionado: los enlaces sin etiquetas de idioma se excluían de las alternativas. Ahora se prueban como último recurso después de español e inglés confirmados. Una fuente sin etiqueta no garantiza un idioma; se conserva la preferencia de pistas de audio del player.
- Hasta diez fuentes distintas en total, con un único presupuesto compartido entre resolución nativa y errores del reproductor. Español: máximo tres fuentes identificadas en español antes de inglés. El enlace elegido manualmente siempre se prueba primero.
- Detecta la ausencia de primer fotograma tras 12 segundos y buffering prolongado tras 20 segundos. También revisa errores emitidos antes de registrar el listener e incluye errores generales/timeout en las categorías recuperables.
- La preparación de cada fuente nativa utiliza timeout de coroutine de 15 segundos. La cancelación de una petición HTTP bloqueante sigue dependiendo de su implementación de red.
- Conserva minuto, intención de pausa/reproducción, límites de momentos y aviso visible durante el cambio. Reutiliza la lista ya cargada. La comprobación de tiempo ocurre localmente, sin consultas a Supabase.
- Pausar, aparcar el player, salir a segundo plano o abrir el selector reinicia el reloj. Los cambios automáticos esperan a que cierres el selector. Si el sistema no permite conocer la conectividad, eso por sí solo ya no desactiva los reintentos.
- Watch Party y Live TV conservan su lógica anterior. Los fallos internos 1004, AudioTrack y DRM no se tratan como enlaces defectuosos. Al agotar alternativas aparece el error final.

CHRISTMAS MOVIES EN COLLECTIONS
- Nueva fila inmediatamente después de la introducción, antes de Franchises.
- Películas con tema navideño en TMDB, ordenadas por valoración, con mínimo de 100 votos y estrenadas hasta la fecha.
- View all abre un catálogo con Load more de 25 títulos por pulsación; conserva el sobrante de páginas de 20 y evita duplicados. Cache local de páginas y reintento visible si falla la carga.
- La fila de Navidad de Home se conserva.

HOME: FINAL DE FILA
- Cada fila de contenido añade Load more al final. Abre su catálogo con lotes de hasta 25 títulos y vuelve a Home conservando la posición al pulsar Atrás.
- Géneros, populares, mejor valoradas, Navidad, K-dramas, clásicos, recomendaciones por historial y próximos estrenos mantienen consultas acordes con su temática y paginación remota cuando corresponde.
- Continue Watching, For You y selecciones finitas solo muestran sus títulos disponibles; no se inventan resultados para completar 25. Top 10 amplía a More popular movies, sin numerar resultados adicionales como parte del Top 10.
- Oscars abre su colección existente por años. Streaming, Franchises y Genres añaden View all hacia Collections.
- Próximamente mantiene el modo de detalles para contenido futuro. Si una página remota falla, los títulos ya cargados siguen disponibles y se puede reintentar.

VERIFICACIÓN
Ejecutadas siete comprobaciones de integración/regresión. Ejecutados en JVM los tests de presupuesto de fuentes, reloj de arranque/buffering y lotes de 25 sin duplicados/pérdida de sobrantes. Bloques Kotlin modificados analizados sintácticamente; fuentes espejo iguales. Casos JUnit de StreamOrdering actualizados/ampliados, pendientes de ejecución con Gradle.
No se compiló APK ni se probó una TV real aquí: faltan Android SDK/Gradle wrapper.

PRUEBA EN TV
1. Con Español seleccionado, prueba una lista de enlaces sin etiqueta de idioma y una fuente que falle; confirma aviso y salto al siguiente enlace.
2. Repite con inglés, Play manually, cambio tras buffering y cancelación con Atrás.
3. Confirma que pausar o abrir el selector no provoca un cambio por tiempo.
4. En Collections abre Christmas Movies y pulsa Load more varias veces; deben agregarse hasta 25 títulos distintos por lote, salvo el final del catálogo.
5. En Home recorre una fila hasta Load more, abre contenido y vuelve; revisa también Streaming, Genres y el retorno a la posición anterior.
