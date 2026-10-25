BruniO RP124 / 22228 / 2.0.0-rp124
Aplicar sobre RP123 conservando rutas. Sin SQL ni APK.

MANDANGUITA
La prueba directVodWithUnknownSizeUsesImdbMovieAndEpisodeRoutes registraba solicitudes paralelas en un mutableListOf no sincronizado. La lista podía perder el registro de la ruta de película aunque la resolución devolviera el enlace correcto.
Reproducción local con el test anterior y el repositorio: 11 fallos de 1000, en la aserción de la ruta registrada.
Se usa CopyOnWriteArrayList para el observador del test. Se conservan todas sus aserciones de tamaño, nombre, cabeceras, rutas IMDb de película/episodio y ausencia de consultas de catálogo. No se alteró el resolver de reproducción ni se eliminó el test.

CHANGELOG
UpdateNotes.changes apuntaba a notas RC95 y anteriores, mientras las nuevas RP se agregaban en rc59Changes. La tarjeta de notificación y el diálogo ahora reciben novedades actuales RP124 desde changes.
El historial empieza por RP123 y revisiones RP recientes; las notas RC se conservan después, bajo Versiones anteriores. Se elimina el encabezado fijo RC57–RC62 que resultaba incorrecto para ese historial.

ADVERTENCIAS
CalendarStoreRP120: normaliza el String nullable de sesión antes de pasarlo a JSONObject.
CalendarAlarmsRP120: usa NotificationCompat.Builder, disponible con la dependencia androidx.core existente, en lugar del constructor legado de Notification.Builder.
Las advertencias restantes de AppState, TmdbRepository, LocalCast, Screens y LiveChannelBrowser no se modifican en este parche. No son la causa de la tarea testReleaseUnitTest fallida.

VALIDACIÓN
8 pruebas JUnit de Mandanguita y datos del changelog pasaron. El test de resolución VOD se repitió 1000 veces con cero fallos después del cambio.
Kotlin/JVM: StreamRepository real, filtros/orden/prioridad y modelos extraídos, con adaptadores de red/NativeSource para aislar las pruebas; se validó también el objeto real de datos de UpdateNotes separado de Compose. Sin peticiones externas en esas pruebas.
No se ejecutó la suite Android completa de 390 pruebas ni Gradle/Compose; pendiente ejecutar el workflow y validar en TV. No se generó APK.
