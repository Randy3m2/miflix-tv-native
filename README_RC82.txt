BruniO TV RC82 — Hotfix sobre RC81

Reemplaza los 11 archivos conservando sus rutas y ejecuta el build habitual en GitHub.
Versión 2.0.0-rc82 / versionCode 22081.
No requiere SQL ni redespliegue de Vercel.

Corrección:
Home desactivaba el desplazamiento automático para mantener encabezados y tarjetas alineados verticalmente. Streaming y Browse by genre heredaban ese bloqueo también horizontalmente. Ahora cada LazyRow tiene su propia política para revelar por completo la tarjeta enfocada al avanzar hacia cualquiera de sus extremos. La política se aplica solo en Home; Collections conserva su comportamiento anterior. El bloque superior, expansión de tarjetas y paginado vertical de RC81 se conservan.

Verificación:
Sin nuevos errores sintácticos Kotlin frente a RC81. Cinco verificaciones automatizadas de regresión aprobadas. No se compiló APK en este entorno (sin SDK/Gradle wrapper). Pendiente probar en TV: avanzar con el control más allá de Prime Video hasta el último proveedor y volver al primero; recorrer todos los géneros en ambos sentidos; confirmar encabezados al cambiar de fila y navegación de Collections.
