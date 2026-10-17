BruniO TV RC85 — Cambio automático de fuente (sobre RC84)

Sube los 16 archivos conservando rutas y genera el build de GitHub.
Versión 2.0.0-rc85 / versionCode 22084. Sin SQL ni despliegue de Vercel.

- Recuperación automática también para preferencias de audio en inglés.
- Play manually conserva la fuente elegida como primer intento; si falla, usa alternativas de la lista ya cargada.
- Pantalla de carga con póster y aviso: Source failed. Trying the next link… (intento/total).
- Hasta cinco fuentes distintas en total; se conserva el minuto y estado de pausa/reproducción, y los límites de momentos compartidos.
- Español mantiene hasta tres fuentes españolas antes de inglés; si hay menos españolas, los espacios restantes se aprovechan para inglés.
- Reutiliza enlaces ya cargados, sin repetir búsquedas de add-ons ni lecturas de Supabase por cada salto. Resolver una fuente nativa sí puede requerir una llamada a su proveedor.
- Se aplica a errores de fuente/red/contenedor/decoder y al límite ya existente de 20 segundos sin primer fotograma. Los errores internos del reproductor, AudioTrack o DRM no provocan una cadena de cambios de fuente.
- Watch Party y Live TV conservan su comportamiento anterior. Sin conexión, sin alternativas o al agotar el presupuesto se muestra el error final.
- Conserva la corrección de audio de RC84 y los cambios de navegación anteriores.

Verificación:
Comprobación de integración de fallback y cinco comprobaciones previas de regresión aprobadas. Ejecutado SourceFallbackBudgetCheck en JVM: límite de cinco, sin índices repetidos y aislamiento de errores de hardware/DRM. Análisis sintáctico del bloque modificado y fuentes de carga/ordenación aprobado. Añadidos tres casos JUnit para candidatos ingleses, prioridad española y deduplicación; esos casos no se ejecutaron aquí. APK pendiente de compilación y prueba en TV, sin SDK Android/Gradle wrapper en este entorno.

Prueba en TV:
Con audio en inglés, inicia una fuente que falle y confirma aviso, cambio de enlace y conservación de minuto. Repite desde Play manually y español. Prueba también cancelar con Atrás durante la carga.
