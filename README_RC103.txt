BruniO Android TV — RC103
Parche visual SOBRE RC102. Instala primero RC102 y conserva sus dependencias de RC101.

Instalación
- Copia las carpetas del ZIP en la raíz del proyecto conservando las rutas.
- app/src/main/... y main/... contienen archivos idénticos, siguiendo los parches anteriores.
- 12 archivos. Versión 2.0.0-rc103 / versionCode 22102.

Cambios
- Funciones locales: tarjetas oscuras con acentos turquesa, iconos vectoriales dibujados en código,
  interruptores animados y borde de foco para el mando.
- Agrupación: A tu ritmo, Un catálogo a tu manera, Herramientas y respaldo.
- Ayuda contextual a la derecha al enfocar con el mando o pasar el cursor por una tarjeta.
  Muestra descripción y estado actual sin activar el ajuste. En ventanas compactas, ayuda arriba.
- El mismo tratamiento visual y ayuda para Canal anterior, Siguiente enlace automático,
  selección adaptativa de fuentes, gustos, herencia de add-ons y selección de filas de Home.
- Los límites de 2/3/5 episodios tienen explicación contextual.
- Etiqueta de manifiestos genérica, apta para complementos compatibles adicionales.

Persistencia y alcance
- Conserva las preferencias existentes, los valores predeterminados y sus ámbitos de perfil/dispositivo.
- Enfocar o pasar el cursor solo modifica el estado visual de ayuda. No consulta proveedores ni Supabase.
- No se han instalado add-ons nuevos ni se ha probado su compatibilidad con este parche.
- Sin SQL, Vercel, nuevas dependencias, permisos ni imágenes remotas.

Validación
- Sintaxis Kotlin revisada: ningún error de análisis nuevo respecto de RC102.
- Rutas duplicadas comparadas byte a byte.
- Revisadas las llamadas a las acciones existentes, incluida saveStillLimit de RC101.
- Sin cambios en AppState, almacenes, consultas ni código del reproductor.
- No se ha compilado la app ni generado APK. Pendiente validar compilación Android,
  distribución visual, foco D-pad, hover y desplazamiento en la TV real.
