# BruniO TV Native 2.0 RC15

Proyecto completo acumulativo. Conserva el reproductor y todas las mejoras anteriores.

## Torrentio/Comet privados por cuenta

Se quitó MIFLIX_TORRENTIO_MANIFEST del workflow y de BuildConfig. El APK ya no incorpora ese manifiesto personal. Al iniciar sesión se cargan solamente los add-ons del privateSetup de esa cuenta: no se mezclan con los del APK, del dispositivo o de un usuario anterior.

Si falla la carga de la cuenta, el listado queda vacío en vez de usar un manifiesto ajeno. Al cerrar sesión, cambiar de cuenta o vincular otra por QR se borran los add-ons y selecciones anteriores. Las respuestas tardías de otro usuario no pueden asignar sus enlaces al nuevo usuario.

Cada usuario añade su propio Torrentio/Comet desde Settings o el QR de vinculación. Los perfiles dentro de una misma cuenta siguen compartiendo sus propios add-ons. Settings permite eliminar cada complemento. Guardar cambios requiere iniciar sesión.

El secreto antiguo de GitHub puede eliminarse: el workflow nuevo ya no lo lee. El token TMDB de catálogo continúa como antes.

## IMPORTANTE: limpiar las copias guardadas por APK anteriores

Las versiones anteriores podían insertar el manifiesto del APK en privateSetup de otras cuentas. Quitar la configuración del APK no borra por sí solo esas copias ya guardadas.

1. Actualiza las TVs de los usuarios a RC15 para que los APK anteriores no vuelvan a copiar el manifiesto.
2. Supabase > Authentication > Users: copia el UUID de TU cuenta, propietaria del manifiesto.
3. Supabase > Table Editor > miflix_user_state: busca la fila de tu UUID con profile_id __account__. Copia el manifiesto antiguo exacto de privateSetup.torrentioManifest o addonManifests. No lo publiques ni lo compartas por chat.
4. Abre supabase_rc15_remove_shared_addon.sql. Sustituye owner_user_id NULL por 'tu-uuid'::uuid y coloca el manifiesto antiguo exacto dentro de las comillas de shared_manifest.
5. Ejecuta ese script en Supabase SQL Editor como administrador. Conserva tu configuración y elimina SOLO ese enlace de las otras cuentas. No elimina sus otros add-ons, perfiles, favoritos, progreso ni tokens TMDB. Puede ejecutarse otra vez sin efectos adicionales.
6. Los usuarios cierran sesión y vuelven a entrar para cargar su configuración limpia; cada uno agrega su propio add-on si quedó vacío.

Si compartiste más de un manifiesto personal, repite la limpieza para cada URL exacta. No es una migración de esquema ni una función pública: es una corrección dirigida de datos.

## Revocar el acceso de APK anteriores

El manifiesto personal pudo quedar incorporado en APK ya distribuidos. La actualización no retira esas copias. Para revocar ese acceso, cambia/regenera la credencial de tu proveedor de debrid y vuelve a configurar tu add-on con el manifiesto nuevo, guardándolo solamente en tu cuenta. Guarda la URL antigua para identificarla en la limpieza antes de reemplazarla.

## Enlaces TB Download

Se excluyen filas que indiquen TB Download, TB download o [TB+] Download en nombre/título. El filtro se aplica antes de ordenar y de escoger el enlace del Play rápido. Los enlaces TB disponibles/cached se conservan. El orden sigue siendo resolución descendente y después tamaño descendente.

## Foco suave

Marco blanco exterior que se desplaza en unos 150 ms entre carátulas, colecciones, géneros, perfiles, amigos, canales, acciones y controles del reproductor. El marco cambia de tamaño con la selección y acompaña el scroll. El aumento leve de las tarjetas también se suavizó. El menú superior conserva la transición que te gustó.

El marco no captura teclas/touches y no modifica la superficie de vídeo. Los diálogos conservan su foco independiente, sin reutilizar coordenadas de la pantalla de fondo.

## Build

Reemplazar los archivos del repositorio y ejecutar Build MiFlix Native TV APK. Mismo paquete Android y APK permanente native-latest/MiFlix-TV-Native.apk: mismo Downloader. Pages y Trakt no requieren despliegue nuevo.

El workflow mantiene testDebugUnitTest antes de assembleDebug; incluye nuevos casos para excluir TB Download y conservar TB cached.

## Validación y pruebas en TV

Se verificaron sintaxis Kotlin/XML/SQL, integridad del ZIP, ausencia del manifiesto de build en app/workflow y la limpieza dirigida con PostgreSQL: parámetros obligatorios, preservación del propietario, otros add-ons, perfiles/tokens/progreso y ejecución repetida.

No hay Gradle/Android SDK en este entorno. Compilación, pruebas unitarias Kotlin y aspecto final deben confirmarse con la build y en la TV.

Comprobar: una cuenta nueva sin add-ons no recibe Torrentio; cambiar de A a B no conserva los add-ons de A; la cuenta A conserva los suyos; eliminar un add-on actualiza Settings; TB Download no aparece; el marco acompaña navegación y scroll en Home/Collections/catálogos/Live TV/perfiles/player; diálogos y Party continúan con foco propio.
