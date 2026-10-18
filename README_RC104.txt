BruniO Android TV — RC104
Parche SOBRE RC103. Conserva RC102 y los parches anteriores instalados.

Instalación
- Copia las carpetas del ZIP en la raíz del proyecto conservando las rutas.
- app/src/main/... y main/... contienen archivos idénticos.
- 10 archivos. Versión 2.0.0-rc104 / versionCode 22103.

Cambios
- La fila local_lists de Home pasa de un botón simple a una fila horizontal de colecciones.
- Cada tarjeta muestra el nombre elegido, cantidad de títulos y hasta cuatro carátulas distintas.
- Al pulsar una tarjeta se abre directamente esa lista. Una tarjeta adicional permite crear otra.
- Sin títulos o sin imágenes hay una presentación alternativa; no se buscan fotos nuevas.
- Administrador renovado: cabecera visual, tarjetas con collage y contenido con pósteres.
- Crear, renombrar, añadir, quitar, eliminar con confirmación y Cargar 25 más conservan su función.
- La fila conserva su identificador, orden personalizado y visibilidad por perfil.
- Las listas permanecen independientes por perfil y guardadas en esta TV.

Requisitos y rendimiento
- Funciones locales > Listas personales locales debe estar activado, como antes.
- Usa las URLs de póster/backdrop ya guardadas con los títulos y la caché de imágenes existente.
- Fila de vistas previas con carga diferida. Sin consultas nuevas a TMDB para generar collages,
  sin polling, sin escrituras nuevas en Supabase ni cambios de almacenamiento.
- Sin SQL, Vercel, nuevas dependencias ni permisos.

Validación
- Sintaxis Kotlin revisada: ningún error de análisis nuevo respecto de RC103.
- Rutas duplicadas comparadas byte a byte.
- Revisadas lista vacía, ausencia de imágenes, nombre largo, límite de cuatro carátulas,
  acceso inicial por ID y actualización de vistas previas mediante la revisión local existente.
- Ajustes y reproducción fuera de la interfaz de listas permanecen iguales a RC103.
- No se ha compilado Android ni generado APK. Pendiente validar compilación, diseño,
  foco y desplazamiento con mando en TV real.
