# BruniO TV RP149 · Dependencias del reproductor

Aplicar sobre RP148 respetando todas las rutas. Copiar los 6 archivos de app/ y reemplazar cuando corresponda. Generar con build-native-tv-apk.yml. TV 2.0.0-rp149 / 22253. Versión móvil M7/RP148 no cambia.

La build reportada tiene Screens.kt actualizado pero no las dependencias RP144. Este paquete incluye:

- data/Live404RetryRP144.kt: Live404RetrySessionRP144 y Live404RetryBudgetRP144.
- ui/PlayerFailureDetails.kt: liveHttpStatusRP144 y el diagnóstico de fallos existente.
- ui/LivePlayerTools.kt: reproductor SplitScreen coherente con el controlador, incluyendo propiedad de los players y cancelación al retirar un cuadro.
- Pruebas del controlador, versión y notas de actualización.

Los errores Cannot infer type de attempt/failure son consecuencia de los tipos ausentes. No se cambian los callbacks ni se ocultan los errores.

Se mantienen los cuatro reintentos HTTP 404 (1/2/4/8 segundos), limitados a canales live directos; excluye VOD, recordings, YouTube y el add-on Sports con su recuperación propia. No reinicia los otros cuadros.

No es necesario volver a aplicar todo RP144 ni reemplazar Screens.kt de RP148. No incluye APK firmado ni cambios de SQL. La nueva V1 de descubrimiento/IA sigue pendiente mientras se resuelve esta build.
