BruniO TV RP33 — Corrección del cliente de avisos
Aplicar sobre RP32 conservando rutas. Versión 2.0.0-rp33 / 22137.

Problemas encontrados en RP31:
- HTTP/JSON fallidos se convertían en guía vacía y se guardaban una hora, impidiendo nuevos intentos.
- Un timeout conjunto de 6.5 s incluía la resolución del ID IMDb y la llamada externa; podía finalizar antes de leer la guía.
- La petición empezaba después del primer fotograma, en lugar de preparar los datos durante la carga.

RP33 consulta en paralelo a la preparación del player, separa los plazos de metadata y guía, valida el formato JSON, acepta categorías en minúsculas y descarta los fallos sin guardarlos como ausencias. Una nueva caché ignora automáticamente los errores antiguos. Solo respuestas válidas se guardan: avisos 7 días y guías válidas vacías 1 hora. No se inventan advertencias ni se retrasa el video. Conserva el interruptor por perfil y el overlay animado.

Ajustes > Reproducción > Comprobar avisos de contenido permite probar Lanterns o The Shawshank Redemption directamente desde la TV, sin caché. Muestra HTTP real, timeout, respuesta inválida o categorías recibidas. Permite borrar la caché. La prueba solo hace una petición al pulsarla y no modifica vistos ni progreso.

LIMITACIÓN ABIERTA: desde el entorno de preparación, api.imdbapi.dev devolvió HTTP 503 para ambos títulos. El cuerpo indicó un fallo de conexión al upstream. Eso confirma que las peticiones desde este entorno no funcionan, pero NO demuestra una caída global ni qué respuesta recibe la TV. No se verificó una guía real en vivo. Nuvio permite configurar su URL de guía; no se puede asumir que su versión instalada use la misma URL pública que BruniO. RP33 corrige los errores del cliente, pero no puede garantizar avisos si la fuente está caída o bloqueada. El resultado de Probar Lanterns en la TV permite distinguir ese caso. No se cambió de proveedor ni se añadieron claves privadas de otra aplicación.

Verificación: 10 pruebas JVM de parser y validación de respuestas pasaron; cliente compilado con transporte sustituido en esas pruebas, sin peticiones reales. Sintaxis Kotlin comparada con base y espejos verificados. Las consultas de diagnóstico externas sí devolvieron los HTTP descritos. No se ejecutó Gradle Android ni se generó APK. Sin SQL, Vercel ni tráfico nuevo a Supabase.
