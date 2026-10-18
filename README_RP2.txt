BruniO Android TV — RP2
Parche SOBRE RP1. Versión 2.0.0-rp2 / versionCode 22106.
Copia las carpetas app/ y main/ conservando las rutas.
No requiere SQL nuevo ni Vercel. La migración social RP1 sigue siendo requisito de RP1.

PRIORIDAD SEGÚN EL IDIOMA DE AUDIO
Inglés: Comet → Torrentio → otros instalados.
Español: LAT-Add → Addon-Latam → Deepflix → Progreso Latino → Umbra → Nuve Debrid
→ Nuve+ → Torrentio → Comet → otros instalados.
Solo participan complementos efectivamente instalados para el perfil.

El idioma comprobado en las etiquetas sigue siendo la primera regla:
- Con preferencia española, todos los enlaces identificados como audio español preceden al inglés.
- Dentro de cada grupo de idioma se aplica el orden de complementos y después calidad/tamaño.
- Esto también se aplica a los reintentos automáticos de RP1.
- Un proveedor latino con un enlace inglés no desplaza una fuente identificada como española.
- No se deduce audio español por tener subtítulos, ni solo por el nombre del add-on.
- La selección manual de un enlace conserva tu elección.

ORDEN PERSONALIZADO
Settings → Complementos de reproducción → Orden de los add-ons.
- Panel negro del tema, flechas para subir/bajar, prioridad numerada y ayuda al enfocar/hover.
- Pestañas Español / Inglés: editan órdenes independientes; no cambian tu idioma de audio.
- Solo aparecen los add-ons instalados de fuentes, excluyendo los de catálogo cuando así lo declaran.
- Restablecer prioridad automática recupera la regla del idioma seleccionado.
- Se guarda por cuenta, perfil, idioma y TV; no modifica el orden de otros perfiles.
- El orden se guarda mediante hashes de los manifiestos, sin duplicar URLs o tokens.
- Los add-ons nuevos se incorporan después de los que ordenaste; los eliminados dejan de aparecer.
- El selector All/por add-on también refleja el orden del idioma de reproducción actual.
- Reordenar usa los enlaces ya almacenados en caché y no fuerza otra búsqueda de fuentes.
  Abrir este panel puede comprobar manifiestos no almacenados en la caché de seis horas de RP1.

DETAILS DENTRO DEL REPRODUCTOR
- Información de reproducción vuelve a pantalla completa, sobre el vídeo.
- Los detalles largos de un enlace también usan pantalla completa.
- Sin panel lateral opaco, desenfoque ni oscurecimiento de la ventana de Android.
- Degradado negro translúcido ligero para leer el texto, dejando ver el vídeo detrás.
- Conserva la información técnica, desplazamiento y botón Cerrar.

VALIDACIÓN
- Compilación JVM aislada de los archivos de datos nuevos/modificados: aprobada.
- Pruebas de orden inicial EN/ES, nombres con acentos, orden personalizado, altas/bajas,
  prioridad de idioma antes que add-on/calidad y orden de reintentos: aprobadas.
- Reejecutadas las pruebas JVM de RP1 de agregación, caché, exclusión de catálogos y fuentes españolas.
- Análisis de sintaxis Kotlin: sin errores nuevos respecto de los archivos anteriores.
- Comparadas las dos rutas de código byte a byte.
- Sin compilación Android ni APK. Pendiente verificar diseño, foco D-pad, reproducción y
  pantalla completa en la TV real.

20 archivos. Sin nuevas tablas, polling, cron, dependencias ni permisos.
