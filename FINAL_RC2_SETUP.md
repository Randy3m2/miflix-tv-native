# MiFlix TV Native 2.0 RC2 — instalación y novedades

## Actualización desde Social/Trakt RC1
1. Supabase > SQL Editor: ejecuta todo `supabase_final_upgrade.sql`. Mantiene las salas y RLS.
   Si nunca ejecutaste el SQL social, ejecuta primero `supabase_party_social_upgrade.sql`.
2. Reemplaza el contenido de la raíz del repositorio con el contenido de este ZIP.
3. GitHub Actions > Deploy MiFlix Pairing Pages y Build MiFlix Native TV APK.
4. Instala la nueva APK encima de la anterior, con la misma firma de compilación.
5. La función Trakt PKCE actualizada está incluida. Si ya publicaste el archivo PKCE, no necesitas publicarla otra vez.
   Configuración exacta y redirect URI en SOCIAL_TRAKT_SETUP.md.

El enlace permanente `native-latest/MiFlix-TV-Native.apk` y el código de Downloader permanecen.
El artefacto de esta versión y el tag pasan a `2.0.0-rc2`; el canal permanente conserva su nombre.

## Reproductor
- El título aparece arriba a la izquierda cuando se muestran los controles y se oculta con ellos.
- Tras 5 segundos en pausa aparece título, temporada/episodio y sinopsis sobre una sombra lateral.
  Reanudar la reproducción oculta esa información. Se usa sinopsis del episodio si está disponible.
- Al pausar aparecen inmediatamente la hora local de la TV (formato 12/24 h del sistema) y
  el tiempo restante del episodio o película. La hora se actualiza mientras permanece en pausa.
  En contenido en vivo se muestra En vivo, y si la fuente no informa duración se indica que no está disponible.
- Settings > Playback preferences: audio y subtítulos predeterminados; subtítulos Off; autoplay On/Off.
  Se guardan por dispositivo y se aplican al iniciar el siguiente contenido. Si un idioma no existe
  en el enlace, Media3 usa las pistas disponibles; no se crean doblajes ni subtítulos inexistentes.
- Playback links abre el selector modal durante el contenido. Elegir otra fuente conserva la posición.
  Cada fuente puede tener duración o montaje distinto, por lo que no se garantiza alineación exacta.
- Skip Intro y Skip Credits usan IntroDB /segments por IMDb y episodio, o is_movie=true para películas.
  Solo aparecen en el tramo correspondiente y con datos válidos dentro de la duración del video.
  Skip Credits conserva una escena poscréditos si IntroDB indica su tiempo. No se inventan tiempos.
- Al terminar, las series ofrecen el próximo episodio emitido, incluyendo cambio de temporada,
  con cuenta atrás de 10 segundos si autoplay está activo. Puede cancelarse o elegirse manualmente.
- Películas y series muestran recomendaciones de TMDB al terminar. Los invitados siguen al host;
  el host elige el siguiente contenido para toda la sala.

## Watch Party
- Los invitados pueden PAUSAR desde los controles de la TV o el chat móvil.
- El host recibe y consume la solicitud en su siguiente ciclo; pausa y sincroniza a los demás.
  La sincronización depende de red/polling y no es instantánea.
- Solo el host reanuda, cambia contenido y hace seek global. Los invitados no obtienen permiso
  para editar libremente las filas de la sala. No necesitan otro QR cuando cambia el contenido.
- La cola de pausas valida membresía, contenido y episodio; descarta solicitudes antiguas o de
  contenido cambiado. Caduca tras 30 segundos si el host no las consume.
- No hay un máximo de miembros configurado en el código. Una misma cuenta se cuenta una vez.
  No se ha probado capacidad con muchos usuarios; el límite práctico depende de Supabase,
  la red y las consultas periódicas. No se promete un número de usuarios simultáneos.

## Notificaciones
- Campana Alerts en la barra izquierda; `N+` indica avisos pendientes.
- Revisión al abrir la app, cambiar perfil/favoritos y cada 30 minutos mientras permanece abierta.
  Revisar ahora permite solicitar una comprobación manual; Limpiar borra los avisos.
- Primera revisión: establece una referencia sin notificar todos los estrenos antiguos.
- Revisiones siguientes: avisa del último episodio emitido nuevo o del estreno de una película
  que era futura al establecer la referencia. Usa fechas de TMDB, no la disponibilidad del add-on.
- Cada perfil mantiene su historial local en el dispositivo. Limpiar no repite los mismos avisos.
  No hay push con la app cerrada ni sincronización de avisos entre TVs.

## Verificación
SQL ejecutado en PostgreSQL WASM con RLS real: migración repetida, host insert/upsert,
pausa de participante, rechazo de extraños/episodio incorrecto, consumo exclusivo del host,
aislamiento de solicitudes y permisos de chat/amigos/tokens.
Kotlin y SQL revisados por analizadores de sintaxis. IntroDB probado contra un episodio real.
Trakt PKCE probado con API simulada, incluido estado incorrecto, privacidad y reuso de callback.
No se dispone de Gradle/Android SDK en este entorno: falta compilar en GitHub Actions y
verificar navegación, reproducción, pistas y sincronización con dos TVs reales.

## Marca BruniO
El nombre visible del launcher pasa a BruniO. Se añaden el icono adaptativo, el banner TV
y la pantalla de arranque nativa/Compose con la marca. Se conserva applicationId
com.miflix.native2 y los enlaces permanentes para actualizar la instalación existente.
Los recursos se prepararon con la herramienta integrada de imágenes, a partir de la
referencia del usuario: icono inferior aislado y marca superior reenmarcada en 16:9,
conservando texto BruniO y gradientes azul/cian/violeta. No se generó un logo distinto.
