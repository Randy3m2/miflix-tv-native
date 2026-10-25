RP127 - Add-on dependency hotfix
Aplicar sobre RP127 conservando las rutas, incluyendo las carpetas data y ui. No cambia la versión 22231 / 2.0.0-rp127.

Incluye AddonManifestRP126.java y AddonArtworkRP126.kt bajo app/src/main/java/com/miflix/native2/data, y AddonManagerRP126.kt de RP127 bajo ui. Los errores keys, logo, description, url y tipos no inferidos se derivan de que no se resuelven esas clases. RP127 era incremental sobre RP126; este ZIP vuelve a incluir las dependencias y conserva el espaciado de RP127.

12 pruebas JUnit pasaron; parser Java y repositorio Kotlin compilados en JVM con adaptador HTTP aislado. Kotlin sin errores de sintaxis. No se compiló el módulo Android/Compose; falta confirmar el workflow completo. Sin SQL ni APK.
