# MiFlix: Watch Party Social + Trakt

## 1. Actualizar Supabase

En Supabase > SQL Editor > New query, pega y ejecuta TODO el archivo
`supabase_party_social_upgrade.sql`. Incluye la reparación Beta → RC1 y las
nuevas tablas para participantes, chat, nicknames, amigos y Trakt. Conserva los
datos existentes. Es seguro ejecutarlo otra vez. Usa este archivo después de
cualquier script antiguo de Watch Party, porque aplica las políticas actuales.

## 2. Publicar e instalar

Reemplaza en la raíz del repositorio el contenido de `miflix-tv-native-main`.
No subas esa carpeta como otro nivel dentro del repositorio.

Ejecuta Actions > Deploy MiFlix Pairing Pages, sobre main, para actualizar el
chat móvil en `/party/`. Luego ejecuta Build MiFlix Native TV APK e instala el
nuevo APK. El workflow y `native-latest/MiFlix-TV-Native.apk` no cambian.

## 3. Usar las funciones

- Menú izquierdo > Party > Create Room: crea la sala antes de elegir contenido.
  Create with current title usa la película/episodio/canal abierto.
- Choose content vuelve al catálogo. Cada nuevo título que el host reproduzca
  se sincroniza en la MISMA sala; sus invitados resuelven sus propios enlaces.
- El código y QR permanecen mientras la sala siga abierta. Leave / close room
  cierra la sala si eres host; si eres invitado, solo sales de ella.
- La sala se restaura al abrir la app con la misma cuenta si no ha expirado.
  Tiene 8 horas de caducidad desde la última actualización del host. Un host
  activo renueva la sala. No es un enlace perpetuo después de cerrar/expirar.
- Durante el video, React / Chat abre emojis y frases. Aparecen durante unos
  4.5 segundos; no son controles permanentes. También hay botones en Party.
- Settings > Friends / Nickname / Phrases: elige tu nickname único (3–24 letras
  minúsculas, números o _), busca amigos y acepta solicitudes. Puedes agregar
  a cualquier participante desde Party. La amistad requiere aceptación.
- En esa pantalla configura hasta 8 frases, separadas con `|`. Se guardan en
  el dispositivo y pueden prepararse antes de entrar a una sala.
- Escanea el QR de Party: abre la web móvil, inicia sesión con tu cuenta
  MiFlix y escribe. Incluye emojis, frases personalizables, participantes y
  solicitudes de amistad. No hay que instalar una app. Las frases móviles
  se guardan en ese navegador; no sustituyen las frases guardadas en la TV.
- El chat se actualiza aproximadamente cada 2 segundos. Una cuenta puede usar
  TV y móvil; otro participante debe entrar con su propia cuenta MiFlix.
- Live TV utiliza el mismo canal y el add-on configurado en cada dispositivo;
  los canales en vivo no hacen seek sincronizado y pueden tener distinta
  latencia. Películas/series sí sincronizan play/pause/seek/episodio.

## 4. Activar Trakt (una vez, para el proyecto)

Trakt necesita una aplicación API registrada y una Edge Function publicada.
La integración está en el ZIP, pero no puede activarse sin tus credenciales.

1. Crea tu aplicación en https://trakt.tv/oauth/applications/new (MiFlix).
   Configura el redirect URI `urn:ietf:wg:oauth:2.0:oob` para el flujo de dispositivo.
2. Copia Client ID y Client Secret a Supabase > Edge Functions > Secrets,
   como `TRAKT_CLIENT_ID` y `TRAKT_CLIENT_SECRET`.
3. Publica `supabase/functions/miflix-trakt/index.ts` como función `miflix-trakt`.
   El archivo `supabase/config.toml` ya configura `verify_jwt = false`: la función
   verifica explícitamente el Bearer token con Supabase Auth en cada solicitud.
   Si lo haces desde el Dashboard, aplica la misma configuración de la función.
4. En la TV: Settings > Trakt > Connect Trakt. Escanea el QR, introduce el
   código mostrado y autoriza en Trakt. No necesitas introducir Client Secret
   en la TV ni incluirlo en GitHub.

Alternativa con Supabase CLI, desde la raíz del proyecto:

```bash
supabase login
supabase link --project-ref iwvhigqxsvvgyzdygiqa
supabase functions deploy miflix-trakt
```

Si usas un archivo local para los secretos, copia `.env.trakt.example` a
`supabase/.env.trakt`, completa los valores y ejecuta:

```bash
supabase secrets set --env-file supabase/.env.trakt
```

Ese archivo privado está excluido por `.gitignore`.

Trakt ofrece en esta versión: conexión por código/QR, importación de películas
 y series de Watchlist a My List (hasta 2,000 entradas), consulta de 30 entradas
recientes del historial, desconexión y registro automático como visto al
superar el 80% de una película o episodio. Los canales en vivo se excluyen.
No es sincronización bidireccional de listas/ratings ni scrobbling start/pause.
Las credenciales OAuth y los códigos privados quedan en Supabase, accesibles
solo por la función del servidor. Los tokens se renuevan desde el servidor.
Trakt se conecta por cuenta MiFlix, compartida por sus perfiles.

## Verificación realizada

- 23 fuentes Kotlin analizadas sin errores de sintaxis. Esto no sustituye la
  compilación Android: no hay Gradle/Android SDK en este entorno.
- Migración ejecutada en PostgreSQL WASM desde el esquema Beta, con backfill
  y reejecución. Pruebas de membresía, propiedad de eventos, aislamiento de
  extraños, aceptación de amistad, protección de identidades y tokens y expiración.
- Pruebas del servidor Trakt con API simulada: rechazo de autenticación,
  conexión pendiente/autorizada, tokens privados, payload de episodio e idempotencia.
- Lógica del chat probada con DOM/API simulados: ingreso, participantes,
  renderizado de texto, deduplicación de mensajes, solicitudes y frases.
  No se pudo realizar prueba visual con navegador porque no está instalado.

Pendiente: compilar en GitHub Actions, publicar SQL/Pages/Edge Function en tu
proyecto, autorizar Trakt real y probar con dos cuentas/TVs.

## Prueba rápida con dos usuarios

A crea una sala vacía; B entra por código. A reproduce una película y luego
cambia a otra o a un episodio: B debe seguirlo sin QR nuevo. Desde el móvil de
B, envía un emoji, una frase y un mensaje; deben aparecer y desaparecer en la TV.
A agrega a B y B acepta en Friends o en la web. Conecta Trakt, importa la
Watchlist y verifica un título visto en el historial tras alcanzar el 80%.

## Referencias

- Trakt Device Flow: https://developer.trakt.tv/docs/authentication-oauth
- Supabase secretos: https://supabase.com/docs/guides/functions/secrets
- Supabase deploy: https://supabase.com/docs/guides/functions/deploy
