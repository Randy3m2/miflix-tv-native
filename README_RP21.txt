BruniO Android TV - RP21 / 2.0.0-rp21 / 22125
Aplicar SOBRE RP20 conservando las rutas y reemplazando archivos.

Deepflix (https://mg.homelabx.qzz.io/manifest.json) vuelve a estar incluido
por defecto en todos los dispositivos y perfiles, incluso sin iniciar sesion.
Se retira de la lista de complementos retirados de fabrica de RP12.
Se conservan los otros tres complementos predeterminados y los personales.

Audio espanol: Deepflix primero por defecto, mediante la prioridad de RP19.
Se conserva el orden ingles y cualquier orden personalizado guardado.
Sigue siendo posible eliminar Deepflix en Settings. Una eliminacion previa
explicita del usuario se respeta; los retiros automaticos de RP12 no lo excluyen.
No garantiza disponibilidad del servidor del complemento.

Validacion: pruebas JVM de instalacion predeterminada, migracion, eliminacion,
orden espanol/ingles y personalizado. Sin APK, SQL ni cambios en Vercel.
