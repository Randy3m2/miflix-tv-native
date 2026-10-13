# RC36: Tiempo de uso

Reemplaza/sube los archivos app/ y el workflow incluidos, conservando sus rutas.
Ejecuta el workflow Build MiFlix Native TV APK después del commit. El workflow incluido toma la versión del APK automáticamente. RC36 es 2.0.0-rc36 / versionCode 22034, para que Check for Updates detecte esta entrega sobre RC35.

Cambios de app: app/build.gradle, MainActivity.kt, ui/Screens.kt y el nuevo data/AppUsageTracker.kt. Las copias main/ también están actualizadas.

Settings muestra un botón Tiempo de uso con horas y minutos acumulados. Al pulsarlo explica la medición. Cuenta navegación y reproducción mientras la app está visible, incluyendo PiP visible; no mide tiempo viendo contenido exclusivamente. No suma segundo plano ni sueño profundo. Es un total local para todos los perfiles, desde instalar RC36, sin reconstruir uso anterior ni sincronizarlo con Supabase. Sobrevive a cierres, reinicios y actualizaciones; borrar datos/desinstalar elimina el contador. Guardado cada 30 segundos y al ocultar la app; un cierre forzado puede perder los últimos 30 segundos.

Verificación: revisadas las conexiones onStart/onStop, persistencia local y copias app/main. No se compiló APK en este entorno, que no tiene Gradle/SDK Android. Prueba en TV: abrir Settings, permanecer dos minutos, salir a Home, esperar un minuto y volver; debe haber sumado solo el tiempo visible. Reiniciar la app y verificar que el total se conserva. GitHub debe completar el build antes de instalar.
