BruniO RP58 / 22162 — navegación y directores

Aplicar sobre RP57 conservando rutas. 9 archivos.

Navegación: pulsación nueva inmediata; repeticiones al mantener flechas aceleran con límites de 85 ms vertical y 70 ms horizontal tras 1.2 s. Mantiene coalescencia de eventos, sin acumular una cola de acciones.
Home usa el integrador existente con respuesta de resorte más rápida y acotada. Se conserva el anclaje y restauración de enfoque existentes.
Carátulas MediaCard y SpotlightCard: espacio fijo desde el inicio, carga asíncrona, brillo diagonal tras 160 ms de espera. El brillo se retira al cargar o fallar. Error mantiene fondo gris estable sin animación perpetua. No modifica el tamaño ni dispara scroll al terminar la carga.
Directores: se sustituye la fila de botones existente por tarjetas horizontales, retratos en blanco y negro, nombre, borde de enfoque del tema y retrato destacado arriba. Consulta retratos mediante el repositorio TMDB existente y los reutiliza en memoria. Mantiene los directores y destinos existentes.
No añade consultas a Supabase por animación o scroll, ni cambia reproductor, canales o ajustes de perfiles. Conserva RP57.

Verificación: revisión del video de referencia por cuadros, diferencias de código limitadas, conservación de perfiles RP57 y rutas/versiones. No se ejecutaron Gradle ni pruebas JVM: el entorno carece de javac. No se generó APK.
Pendiente medir suavidad, navegación mantenida y carga lenta en la TV real. No se afirma equivalencia exacta de rendimiento con Nuvio.
