# Suscripción HTTPS al calendario de BruniO

Exportar un archivo .ics no requiere estos pasos. Solo hacen falta para una URL que consulten Google Calendar, Outlook u otro cliente compatible. No se ha ejecutado nada en tu proyecto.

1. Ejecuta supabase_calendar_feed_rp167.sql en el SQL Editor de tu proyecto. Crea una tabla separada con RLS y sin acceso directo de anon/authenticated.
2. Despliega la carpeta supabase/functions/brunio-calendar-feed. Ejemplo con Supabase CLI desde el repositorio:

   supabase functions deploy brunio-calendar-feed --project-ref TU_PROJECT_REF --no-verify-jwt

   La configuración correspondiente ya está en supabase/config.toml. La función necesita el JWT del usuario para crear, actualizar o revocar. Los GET del calendario usan una URL aleatoria de lectura; la publicación de fondo usa una credencial distinta. Por eso el gateway no puede exigir JWT para todas las solicitudes.
3. SUPABASE_URL y SUPABASE_SERVICE_ROLE_KEY son variables del entorno de Supabase Functions. La clave de servicio no debe copiarse en Android, en archivos del repositorio ni en el enlace del calendario.
4. Instala la versión móvil RP167, inicia sesión y abre Calendar → Export / Subscribe · ICS → Create HTTPS subscription.
5. Copy private URL. En el calendario externo usa su opción para añadir/suscribirse desde URL. Si Samsung Calendar no ofrece suscripción por URL en tu versión, puedes importar el .ics o suscribirte desde tu cuenta Google y sincronizar esa cuenta con Samsung Calendar.
6. En la misma pantalla puedes actualizar o revocar. Al crear una URL nueva para el mismo perfil desde otro dispositivo se sustituyen la URL y credencial anteriores. Revocar borra la fila del servidor: los enlaces antiguos dejan de funcionar, aunque el calendario externo puede conservar temporalmente su copia en caché.

Solo quien tenga la URL puede leer sus eventos. No contiene streams, contraseñas ni claves de Supabase. Las credenciales de lectura/publicación se guardan como hashes separados en el servidor. El calendario externo elige cuándo vuelve a descargarlo; el TTL anunciado es una hora y no equivale a push instantáneo. El trabajo Android es de 6 horas, del perfil activo, y puede retrasarse por ahorro de batería. No hay un cron nuevo en Supabase: las lecturas externas y las publicaciones con cambios sí generan tráfico normal del servicio.
