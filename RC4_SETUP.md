# BruniO TV — 2.0.0-rc4

Este ZIP contiene todos los cambios de RC3, más pausa/reanudación por guests y cinco avatares de perfil.

## Actualizar

1. En Supabase → SQL Editor, ejecuta **supabase_rc4_upgrade.sql**. Incluye la migración de Friends/Party de RC3 y puede ejecutarse aunque ya la hayas aplicado. Requiere la configuración social y supabase_final_upgrade.sql de RC2. No borra salas, miembros ni perfiles.
2. Reemplaza los archivos del repositorio con el contenido de este ZIP.
3. Ejecuta **Deploy MiFlix Pairing Pages**, porque el chat móvil ahora tiene botones para pausar y reanudar.
4. Ejecuta **Build MiFlix Native TV APK**. Actualiza el host y los guests a RC4 para que todos procesen reanudación.

No necesitas desinstalar la versión anterior, volver a configurar Trakt ni cambiar el enlace/código de Downloader.

## Pausar y reanudar

Los guests admitidos por el host pueden usar Play/Pause en su TV o los botones del chat móvil. El host recibe la solicitud y publica el nuevo estado para el party. La app deja hasta cinco segundos para recibir la confirmación antes de volver a seguir el estado remoto. Solicitudes antiguas, de otro episodio o de usuarios que salieron de la sala se descartan. Si llegan varias, se aplica la última recibida. Cambiar episodio y adelantar siguen bajo control del host.

## Avatares

En **Profiles**, cada perfil tiene un botón **Avatar**. Elige Astronauta, Zorro, Robot, Dragón o Especial (la foto proporcionada, sin modificaciones). Al crear un perfil, usa **Elegir avatar** antes de **Add Profile**.

Los cuatro avatares ilustrados se generaron con IA. Las cinco imágenes están incluidas en el APK y pueden mostrarse sin conexión. Para cuentas iniciadas, la elección usa el campo avatarValue existente y se guarda con los perfiles en Supabase; no necesita otra tabla. Sin cuenta, se guarda en el dispositivo. Los perfiles existentes reciben un avatar ilustrado por defecto hasta elegir uno.

## Validación

Se comprobaron sintaxis Kotlin/XML/SQL, integridad de recursos y ZIP. Las pruebas con PostgreSQL WASM verifican migraciones repetibles, acceso aprobado, visibilidad entre amigos, pausa/reanudación, consumo exclusivo del host, orden de solicitudes, contenido coincidente, expiración y salida/reingreso de miembros. El chat móvil se verificó con DOM/API simulados para ambas acciones y la aprobación de acceso.

El APK no se compiló en este entorno. GitHub Actions y la prueba con dos TVs confirmarán compilación, foco, navegación y sincronización real.
