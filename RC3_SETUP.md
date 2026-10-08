# BruniO TV — 2.0.0-rc3

## Instalar sobre la versión actual

1. En Supabase → SQL Editor, ejecuta **supabase_friends_party_upgrade.sql**. Requiere que ya hayas aplicado los scripts sociales y supabase_final_upgrade.sql de RC2. Puede ejecutarse nuevamente y mantiene las salas y miembros existentes.
2. Sube el contenido de este ZIP a la raíz de tu repositorio, reemplazando los archivos correspondientes.
3. Ejecuta **Deploy MiFlix Pairing Pages** para actualizar el chat del QR.
4. Ejecuta **Build MiFlix Native TV APK** y actualiza la app en todas las TVs que participen. No hace falta desinstalar la anterior.

No cambia el enlace permanente del APK ni el código de Downloader. No cambian Trakt ni sus secretos; no hay que volver a publicar la función.

## Cambios

- Hora y tiempo restante a la derecha, con el mismo tamaño, color y tipografía de la sinopsis. La primera pulsación durante la pausa oculta la información y muestra los controles. La reproducción sigue pausada; la información vuelve en la siguiente pausa.
- En contenido, el foco inicial cae en Play/Continue. Volver ahora es un botón circular compacto con foco visible.
- Sugerencias al final: carátulas seleccionables con borde al recibir foco, sin botones de texto debajo.
- Friends tiene pestañas **Friends** y **Party**. Amigos aceptados muestran estado online y el título que están viendo; Party filtra las sesiones activas. La presencia se actualiza cada 5 segundos y pasa a offline tras 90 segundos sin señales de la app.
- **Request access** envía una solicitud al anfitrión. Las solicitudes aparecen en Friends, Watch Party y un botón del reproductor mientras se ven sus controles. En el reproductor se pueden aceptar/rechazar mediante un diálogo, sin cerrar el video.
- Los códigos y QR también solicitan aprobación para usuarios nuevos. El propio host y miembros existentes mantienen acceso. El chat móvil espera la aprobación antes de mostrar mensajes y participantes. Rechazar impide volver a solicitar esa misma sala; el host puede crear otra sesión si cambia de decisión.
- Cada guest puede abrir **Playback links** y seleccionar su propio enlace. Conserva el contenido del host, se vuelve a sincronizar con su posición y no cambia el enlace de otros participantes. Si el host cambia de episodio durante la selección, se descarta la selección anterior.

## Comprobaciones

Se verificaron sintaxis Kotlin, XML y SQL; las pruebas con PostgreSQL WASM verifican migración repetible, visibilidad solo para amigos aceptados, solicitudes pendientes sin acceso, aprobación exclusiva del host, rechazo, expiración de presencia y salas. Las pruebas del chat con DOM/API simulados cubren espera/aprobación/rechazo, mensajes, amigos y pausa. También se ejecutaron las pruebas previas de base de datos y Trakt.

No se compiló el APK ni se probó en una TV desde este entorno. La build de GitHub y la prueba con dos cuentas en TVs validarán el foco del control, la apariencia y reproducción real.
