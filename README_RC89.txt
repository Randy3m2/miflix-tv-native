BruniO TV RC89 — Navegación y transiciones

INSTALACIÓN
Reemplaza los 22 archivos sobre RC88 conservando sus rutas y genera el build de GitHub.
Versión 2.0.0-rc89 / versionCode 22088. Sin SQL, Edge Functions ni despliegue de Vercel.

CAMBIOS
- El control ya no descarta todas las repeticiones mientras una fila está desplazándose: permite reajustar el destino con una cadencia limitada. Pulsaciones nuevas, cambio de dirección y liberación siguen respondiendo inmediatamente; no introduce una cola de movimientos.
- Horizontal: 140 ms de separación mínima en reposo, 180 ms durante scroll. Vertical: 200/240 ms. El estado vertical del control pasa a un objeto simple, evitando estado de composición que cambia con cada repetición.
- Las filas Spotlight (Home y catálogos de plataformas) y las filas de Streaming/Genres de Home utilizan un anclaje al 20% del ancho disponible: el movimiento comienza antes de llegar al extremo derecho. Respeta los límites reales de scroll y conserva la visibilidad de la tarjeta enfocada.
- Home: alineación del grupo completo en 220 ms, conservando encabezado y tarjetas. Sigue entrando por la primera tarjeta al bajar a una fila.
- Enfoque de pósteres con borde suave de 120 ms y escala sutil dentro del espacio reservado. Sin sombras ni desenfoques costosos.
- Expansión de sinopsis después de los mismos 3 segundos, con transición de ancho de 220 ms y contracción de 160 ms; sigue empujando a las tarjetas vecinas. El ajuste de visibilidad se realiza después de completar la expansión.
- Los logos animados de Streaming esperan 420 ms de enfoque estable, evitando iniciarlos al pasar rápidamente. Se conservan los logos estáticos, animaciones disponibles y colores del tema.
- Conserva el buscador, inglés prioritario, Shared moments, Load more y reproducción de RC88. No añade peticiones al servidor.

VERIFICACIÓN
BrowseMotionCheck ejecutado en JVM: pulsaciones, inversión de dirección, liberación, límite de repeticiones durante scroll y geometría del anclaje. KidsAndNavigationCheck ejecutado: compatibilidad con entrada anterior y filtros Kids. Ocho comprobaciones existentes de integración/regresión aprobadas. Bloques Kotlin de navegación, filas y pósteres analizados sintácticamente; fuentes espejo iguales.
No se compiló el APK aquí y no se midieron FPS ni tiempos de fotograma en una TV. No se puede garantizar una fluidez equivalente a Apple TV/HBO sin probar el hardware real. Pendiente build y prueba física.

PRUEBA EN TV
- Pulsa izquierda/derecha lentamente y mantén cada dirección 5–10 segundos; suelta y comprueba que no siga avanzando.
- Repite arriba/abajo y cambia de dirección rápidamente. Confirma que no se corten los encabezados.
- Recorre Streaming rápidamente y detente en un logo para que empiece su animación.
- Espera 3 segundos sobre una película; confirma que expanda empujando a la vecina. Abre detalles y vuelve a la misma tarjeta.
