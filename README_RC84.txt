BruniO TV RC84 — AudioProcessor flush hotfix (sobre RC83)

Reemplaza los 9 archivos conservando sus rutas y genera el build habitual en GitHub.
Versión 2.0.0-rc84 / versionCode 22083. No requiere SQL ni Vercel.

Error reportado:
ERROR_CODE_FAILED_RUNTIME_CHECK / IllegalStateException: AudioProcessor must implement at least one #flush() overload.

Corrección:
El adaptador de mezcla usaba delegación de interfaz de Kotlin. Ahora implementa explícitamente ambos métodos flush y los reenvía al ChannelMixingAudioProcessor, para aplicar el formato pendiente y vaciar el audio correctamente durante inicio, búsqueda y reinicio. Se conserva la detección dinámica de auriculares, mezcla estéreo con diálogo y separación del renderer estándar de Live TV.

Se conserva todo RC83, incluida la restauración de posición de Home/Collections y la corrección horizontal de RC82.

Verificación:
Sintaxis Kotlin y reenvío explícito de las dos sobrecargas comprobados; fuentes espejo iguales. Cinco comprobaciones automatizadas de regresión aprobadas. No se compiló APK ni se ejecutó reproducción real aquí: faltan SDK Android/Gradle wrapper y dispositivo TV.

Después de instalar:
Prueba una película y un episodio, con altavoces y con auriculares; adelanta, pausa y reanuda. Si aparece otro error, conserva el texto completo o una captura.
