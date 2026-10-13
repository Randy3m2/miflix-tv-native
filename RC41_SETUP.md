# RC41: menos solicitudes/logs de Supabase

Hotfix sobre RC40. Reemplaza los archivos respetando sus rutas, commit y ejecuta Build MiFlix Native TV APK. Versión 2.0.0-rc41 / versionCode 22039. No necesita SQL ni redeploy de funciones. Gmail, cron y Trakt conservan su configuración.

Presencia: se mantiene un control local cada 5 segundos, pero se escribe a Supabase como máximo una vez cada 60 segundos si no cambió el contenido. Cambios de película/episodio/sala activan actualización anticipada. La frecuencia de 60 segundos está por debajo del umbral de presencia de 90 segundos del esquema existente.
Avatar: una publicación inicial por proceso; después se escribe solo si cambia la cuenta/avatar. Solicitudes entrantes: polling frecuente solo si eres host de una sala. El estado de acceso pendiente se sigue comprobando cada 5 segundos. Consultas manuales de Friends siguen disponibles.
Las consultas periódicas sociales, de sala y de sincronización del reproductor se suspenden mientras la Activity deja de estar visible, y se reactivan al volver. PiP visible conserva la actividad. Con Watch Party visible se conservan la sincronización cada segundo y el polling de miembros/eventos cada 2 segundos.

Refresh de sesión: bloqueo compartido para una sola renovación en vuelo. Fallos temporales esperan 15, 30, 60, 120, 240 y hasta 300 segundos. Un rechazo HTTP 400/401/403 del endpoint de renovación bloquea esa credencial hasta volver a iniciar sesión; se muestra aviso, conservando favoritos y datos locales. No implica que hayamos identificado la causa exacta del error visto en Logs; evita repetir indefinidamente una credencial rechazada.

Estimación en estado estable, sin Party ni cambios de contenido: antes tres peticiones cada 5 segundos = 2160/h por dispositivo; ahora presencia aproximada 60/h y un avatar inicial. Es una estimación de peticiones de ese circuito, no del total ni del volumen facturable de logs. Los dispositivos con versiones anteriores continúan generando su tráfico anterior hasta actualizarlos.

Validación: añadidas pruebas JUnit de cooldown creciente, rechazo definitivo y recuperación por nueva credencial, y cancelación sin penalización. GitHub las ejecutará junto a las pruebas existentes. El entorno no dispone de Gradle/SDK para ejecutar el build Kotlin/Android. Verificación estructural de lifecycle, controles y copias app/main completada.

Prueba: dejar app en menú estable 3 minutos y revisar Logs (presencia alrededor de cada minuto, sin PATCH avatar continuo); entrar a Party y comprobar solicitudes/chat/sync; salir a Home y comprobar que paran peticiones periódicas; regresar y verificar recuperación. Si una TV muestra Tu sesión ya no es válida, cerrar sesión e iniciar de nuevo en ella. Actualizar todos los dispositivos para medir la reducción.
