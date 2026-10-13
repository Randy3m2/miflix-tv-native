# BruniO: actores, temporadas, File Size y avisos de episodios

## App
- Selecciona un actor en Reparto para ver sus películas y series, ordenadas por popularidad. Selecciona un título para abrir sus detalles.
- El selector «Temporada ▾» está en Detalles y en el panel de episodios del reproductor. Permite saltar directamente a cualquier temporada regular.
- Info del reproductor muestra File Size en GiB y bytes. Si el proveedor no entrega ese dato, muestra «No informado por el proveedor». No estima tamaños de playlists/HLS.
- Un cambio rápido de temporada ya no permite que una respuesta anterior reemplace los episodios de la temporada seleccionada.

## Qué subir para un hotfix pequeño
Reemplaza estos cinco archivos dentro de `app/src/main/java/com/miflix/native2/`:
1. `model/Models.kt`
2. `data/TmdbRepository.kt`
3. `ui/Screens.kt`
4. `ui/PlaybackTools.kt`
5. `ui/AppState.kt`

El ZIP también conserva actualizadas las copias bajo `main/`. El build Android utiliza `app/src/main/`. Compila y publica con el workflow existente. Para distribuirlo como actualización automática, aumenta versionCode y versionName en app/build.gradle y genera el manifiesto de actualización con el proceso habitual. Esta entrega conserva la versión base hasta que decidas publicar.

## Activar emails (configuración obligatoria, todavía no desplegada)
La app sincroniza favoritos en `miflix_user_state`. El servicio consulta todas las cuentas confirmadas y todos sus perfiles; envía una sola vez por cuenta/episodio. No necesitas mantener la TV encendida. Los favoritos deben sincronizarse con una cuenta iniciada.

1. Ejecuta `supabase_episode_reminders.sql` en Supabase SQL Editor.
2. Configura un remitente verificado en Resend.
3. Configura estos secretos de Edge Functions en Supabase:
   - `TMDB_TOKEN`: API key o API Read Access Token de TMDB.
   - `RESEND_API_KEY`: clave de Resend.
   - `EPISODE_EMAIL_FROM`: por ejemplo `BruniO <episodios@tu-dominio.com>` (remitente verificado).
   - `EPISODE_CRON_SECRET`: una clave larga y aleatoria para autenticar el cron.
   - `EPISODE_TIMEZONE`: opcional, por defecto `America/Santo_Domingo`.
   Supabase proporciona `SUPABASE_URL` y `SUPABASE_SERVICE_ROLE_KEY` al runtime. No incluyas claves privadas en el APK ni en GitHub.
4. Despliega `supabase/functions/brunio-episode-reminders/index.ts` con nombre `brunio-episode-reminders`. La configuración en supabase/config.toml desactiva la comprobación JWT para esta función; cada petición exige el secreto `x-cron-secret`.
   CLI: `supabase functions deploy brunio-episode-reminders`
5. Activa Cron, pg_net y Vault en Supabase. En `EPISODE_EMAIL_SCHEDULE.sql`, reemplaza YOUR_PROJECT y el secreto por los tuyos y ejecuta una vez. Revisa primero cron.job y Vault si ya configuraste ese job, para evitar duplicar secretos.
6. El cron revisa cada hora, al minuto 15. La fecha se determina con la zona horaria configurada. Consulta respuestas y errores desde Edge Function Logs y Cron.

Los avisos se basan en la fecha de estreno de TMDB; eso no confirma que Torrentio, Comet u otra fuente ya tenga el episodio. Por eso el email dice «hoy se estrena» y aclara que la disponibilidad varía. Se incluyen varios episodios estrenados juntos, sin mandar estrenos antiguos. No hace backfill de fechas anteriores si el servicio estuvo apagado o se activó tarde. Un error de envío no se marca como enviado y puede reintentarse ese mismo día. La tabla evita duplicados persistentes; la clave idempotente del proveedor protege reintentos/concurrencia de envío.

Para dejar de recibir avisos de una serie: quitarla de favoritos en todos los perfiles y sincronizar. Al eliminar la cuenta se eliminan sus registros de envío.

### Ejemplo
Hello Randy,

Reminder de que hoy se estrena el episodio 4, temporada 2, de tu serie X.

Pasa por BruniO para disfrutar sin precedentes.

La disponibilidad puede variar según tu fuente de streaming.

## Verificación
`node verification/test_episode_reminders.mjs` pasa con solicitudes simuladas: autenticación, varios episodios, exclusión de episodios antiguos, deduplicación y reintento tras error del proveedor. La función TypeScript se ejecutó con eliminación de tipos de Node. Se verificó igualdad de las copias Kotlin app/main. No se compiló APK: el entorno no tiene Gradle ni Android SDK. No se desplegó Supabase ni se enviaron emails reales.

Documentación utilizada:
- https://supabase.com/docs/guides/functions/schedule-functions
- https://resend.com/changelog/idempotency-keys
