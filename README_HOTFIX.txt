BruniO RC75 - Hotfix de compilacion

Reemplaza estos dos archivos sobre RC75 en GitHub y ejecuta nuevamente el build.
Agrega el import kotlinx.coroutines.ensureActive que faltaba en AppState.kt.
No requiere SQL, Vercel ni cambio de version.
Verificado: import presente y archivos espejo identicos. No se compilo el APK en este entorno.
