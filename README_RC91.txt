BruniO TV RC91 — Continuidad vertical y corrección de saltos laterales

INSTALACIÓN
Reemplaza los 22 archivos sobre RC90 conservando sus rutas y genera el build de GitHub.
Versión 2.0.0-rc91 / versionCode 22090. Sin SQL, Edge Functions ni despliegue de Vercel.

VERTICAL EN HOME
- Sustituye los trabajos de animación que se cancelaban/reiniciaban entre filas por un único movimiento activo. Cambiar el foco solo cambia su destino: conserva velocidad y reloj de fotogramas.
- Elimina la espera adicional de un fotograma al cambiar de fila.
- Integración de resorte críticamente amortiguado con frenado progresivo, velocidad limitada por el tamaño visible y pasos de tiempo limitados para no convertir un fotograma lento en un salto grande.
- Home sigue formado por grupos del tamaño del área visible. Si la fila de destino aún no está medida, estima su posición con ese tamaño uniforme hasta obtener la medida real; no usa un salto scrollToItem como recuperación normal.
- Se detiene alineando el encabezado de la fila seleccionada. Cambios de dirección responden inmediatamente; no añade movimientos de foco después de soltar. El alcance sigue siendo la posición final de la fila enfocada, sin scroll libre fuera de ella.

HORIZONTAL
- Detecta la última dirección aceptada por el mando en las filas con anclaje.
- Durante los 350 ms siguientes evita una corrección en sentido contrario si la tarjeta enfocada ya está completamente visible. Esto reduce el pequeño vaivén causado al cerrarse una tarjeta expandida mientras avanzas.
- Si la tarjeta está cortada, permite la corrección necesaria para hacerla visible. Cuando el movimiento se estabiliza recupera el anclaje normal.
- Conserva la expansión después de 3 segundos, el desplazamiento real de las tarjetas vecinas, los márgenes y la aceleración de RC90.

RESTO
Conserva inglés prioritario, recuperación de fuentes, Shared moments, búsqueda tolerante, Christmas Movies y Load more. No añade llamadas al servidor ni logs de Supabase. El bucle de movimiento termina al acomodar la fila o al cancelarse por navegación/interacción.

VERIFICACIÓN
ContinuousMotionCheck ejecutado en JVM: convergencia del integrador con pasos equivalentes a 30/60/120 Hz, cambios sucesivos de destino sin introducir un paso de movimiento cero, cambio de dirección, límite ante fotogramas lentos, protección lateral por cierre de tarjeta y visibilidad de bordes. Es una simulación de la lógica; no una medición de FPS reales.
Ocho verificaciones de integración/regresión aprobadas. Sintaxis de los bloques Kotlin modificados y fuentes espejo comprobadas. APK, ejecución de Compose y prueba visual en TV pendientes: sin Android SDK/Gradle wrapper en este entorno.

PRUEBA EN TV
- Mantén abajo 3–5 segundos, suelta y luego vuelve hacia arriba. Busca continuidad al pasar de una fila a otra y encabezados completos al detenerte.
- Espera la expansión de una película durante 3 segundos y avanza inmediatamente varias tarjetas a la derecha; repite a la izquierda y cerca del final de la fila.
- Prueba invertir la dirección, entrar a una película y volver a la misma posición. Revisa también los logos de Streaming y los accesos Load more.
