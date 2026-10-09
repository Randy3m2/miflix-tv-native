# BruniO TV Native RC18 · 2.0.0-rc18 / 22016

## Subir esta versión

1. Sube el contenido de este proyecto al mismo repositorio (incluye docs/pair, el SQL y los nuevos avatares).
2. Supabase → SQL Editor: ejecuta **supabase_rc18_upgrade.sql** una vez. Se puede repetir. Solo crea/configura el bucket de avatares y sus permisos; no cambia Torrentio, Trakt, perfiles ni favoritos.
3. GitHub Actions → **Deploy MiFlix Pairing Pages** → Run workflow. Espera que termine. Pages debe seguir configurado con Source: GitHub Actions. La carga de fotos requiere la nueva página; no basta actualizar el APK.
4. Conserva exactamente los cuatro secrets y la clave de firma de RC17. No generes otra clave. Ejecuta **Build MiFlix Native TV APK**. Este workflow ejecuta los unit tests y compila el APK release antes de publicarlo.
5. Desde RC17 firmado con esa misma clave: Settings → Check for Updates → Download → Install update. Confirma la instalación en Android. Si el APK instalado todavía usa una firma debug anterior, conserva las indicaciones de transición de RC17_SETUP.md.

El enlace permanente del APK y el código de Downloader no cambian. No requiere deploy nuevo de Trakt ni volver a ejecutar la limpieza RC15.

## Ubicación de los botones

- **Home:** Reproducir aleatorio / Play random está dentro del banner inicial, junto a Ver detalles / View Details. Elige según las recomendaciones y reglas de gustos de RC17; los guests siguen el contenido del host.
- **Settings:** Playback link size limit / Límite de tamaño de enlaces. Presets Sin límite, 5, 10, 20, 40 y 80 GB; límite personalizado entre 0 y 500. 0 = sin límite. Se guarda localmente en esta TV. GB usa 1.000.000.000 bytes; GiB en las etiquetas de proveedores se convierte correctamente.
- **Profiles:** Avatar abre la galería, que incluye Especial 4 y Especial 5, más los diez avatares adicionales Especial 6–15, en el orden en que se adjuntaron. Subir desde celular / Upload from phone crea un QR para ese perfil existente. Inicia sesión en la TV, escanea, elige la foto, revisa la vista previa y pulsa Send securely to TV. No debes iniciar sesión ni instalar una app en el celular. La foto se centra y recorta a un cuadrado de 256 × 256 y se envía cifrada. El QR dura 10 minutos. Puedes reemplazarla, quitarla con Remove profile picture o usar la galería.

Las fotos subidas son avatares públicos para mostrarlas a amigos, con nombres aleatorios. Solo una cuenta autenticada puede subir o eliminar archivos en su propia carpeta. El celular no recibe el token de la TV; la TV realiza la subida. Se admiten fotos que el navegador pueda leer, con archivo original de hasta 20 MB. La imagen final es JPEG y el servidor limita su tamaño a 512.000 bytes.

## Enlaces y Party

- El límite se aplica al listado manual, selección rápida y reproducción aleatoria. Con un límite activo se ocultan tamaños desconocidos para no excederlo. No corta una reproducción que ya comenzó.
- Para películas se compara el año de TMDB con el que figura en filename o título del enlace. Se excluyen años explícitos distintos. Se conservan enlaces sin año; no hay información suficiente para descartarlos. Los números de títulos como 1917 y Blade Runner 2049 no se toman por año de estreno. La resolución, tamaño y año de remasterización tampoco deben sustituirlo.
- Las series no se filtran por año de estreno de la serie: cada episodio puede corresponder a otro año. Se mantiene el orden calidad → tamaño y se siguen excluyendo TB Download.
- Salir de Party limpia inmediatamente la sala, solicitudes pendientes, selector de enlaces, cargas pendientes y reproductor, antes de esperar al servidor. Las respuestas de una sala anterior no pueden reabrir su reproducción ni restaurar su estado. Una nueva consulta de enlaces obtiene una lista nueva.
- El host ve **Nombre · Joined** durante unos segundos cuando la consulta de miembros detecta una nueva incorporación (cada ~2 segundos si la red responde). No se repite mientras el miembro siga en la sala; sí al detectar una salida y reentrada. La lista inicial de una sesión restaurada se toma como referencia sin anunciarla.

## Validación y prueba en TV

Se comprobaron la sintaxis Kotlin/XML/SQL/workflows, el cifrado y envío de la página de avatar con pruebas de navegador simuladas, y los permisos del SQL con PostgreSQL en una estructura de Storage simulada, incluyendo aislamiento entre cuentas y repetición del SQL. No se compiló el APK ni se probó un televisor localmente: lo hará el workflow y la instalación real.

Los unit tests nuevos cubren años/títulos numéricos/resoluciones/tamaños y la invalidación de resultados antiguos. Prueba en TV: encontrar Play random en el banner; configurar un límite y ver los enlaces; unirse/salir/reproducir de nuevo; Joined en host; subir, reemplazar y quitar un avatar por QR. La corrección de Party aborda las carreras encontradas en el código; la prueba real confirma si resuelve el fallo intermitente observado.
