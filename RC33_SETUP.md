# RC33 — Motor nativo y Canales 10k

Versión **2.0.0-rc33**, versionCode **22031**. Proyecto acumulativo sobre RC32. Sube el contenido del ZIP a la raíz del mismo repositorio, incluyendo `.github/workflows`, y ejecuta **Build MiFlix Native TV APK**. Conserva tus secretos de firma actuales. No necesitas SQL, otro repositorio ni nuevos secretos de Actions. Esta entrega modifica Android TV; no modifica PC.

## Motor nativo: Settings → Motor nativo de enlaces

Implementación propia en Kotlin: consulta el índice público Knaben, filtra las coincidencias y comprueba los archivos disponibles en la caché de TorBox. Funciona sin instalar un manifiesto de Torrentio/Comet. **No es el servidor Comet completo**: esta primera implementación admite TorBox y Knaben; no incluye otros servicios debrid, indexadores privados ni todas las fuentes de Comet.

1. Inicia sesión en tu cuenta de BruniO.
2. Abre Settings → Motor nativo de enlaces.
3. Copia tu API key personal desde la configuración de TorBox y pégala en el campo de la TV.
4. Pulsa **Verificar y guardar**.
5. Cambia **Motor nativo: Apagado** a **Encendido**.

El motor viene apagado y guardar una clave no lo activa. Al apagarlo, la resolución vuelve al flujo de add-ons existente. Los enlaces nativos se identifican como **BruniO Native · TorBox** y se combinan con los add-ons configurados. Mantiene el orden por calidad y tamaño, Size Limit y el filtro del año de la película. No ofrece resultados de tipo TB Download.

La búsqueda usa el título original y el título mostrado, cuando son distintos. En series valida el episodio exacto dentro del paquete y omite muestras/tráilers. Los archivos con nombres ambiguos o sin información de tamaño se omiten. La cobertura depende del índice y de la caché: pueden existir enlaces en Torrentio/Comet que este motor no encuentre.

Consultar enlaces no añade todos los resultados a TorBox. Al reproducir se vuelve a comprobar la caché, se registra únicamente el torrent elegido en la cuenta del usuario y se solicita el enlace del archivo seleccionado. Si el proveedor deja de tenerlo disponible o todavía no está listo, se muestra un error para elegir otro enlace. No se espera una descarga de torrent pendiente dentro del player.

La clave se cifra con Android Keystore y se guarda por cuenta en esta TV; los perfiles de esa cuenta la comparten. No se incluye en el APK, no se envía a Knaben, no se comparte por Watch Party y no se sincroniza a otros dispositivos. En otra TV debe configurarse de nuevo. **Eliminar clave y apagar** elimina esa configuración local. La búsqueda envía los términos al índice y las comprobaciones de archivos a TorBox.

El motor usa peticiones limitadas y cancelables, caché breve de resultados y espera ante HTTP 429 de TorBox. Con el motor apagado no consulta estos servicios. Con el motor encendido la búsqueda puede tardar más; si falla, los enlaces que devuelvan los add-ons siguen disponibles.

## Live TV → Canales 10k / 10k Channels

Nueva pestaña junto a **Canales de TV** y **Live Sports**. Conserva ambas fuentes anteriores.

- Lista pública de IPTV-org, descargada al entrar, con categorías, logos cuando existen, números y búsqueda por nombre o número.
- Carga 200 canales por página. La búsqueda se aplica a la categoría completa, no solo a los 200 visibles. **All / Todos** busca en toda la lista.
- **Actualizar** vuelve a descargar la lista. También se renueva después de seis horas durante el uso de la app; si una renovación automática falla, conserva la copia en memoria.
- La búsqueda y la pestaña elegida se mantienen al volver del reproductor durante la sesión.
- Los números se asignan por orden alfabético; pueden cambiar al actualizar el catálogo si se añaden o eliminan canales.
- Disponible también en **Reproductor → Canales** y al añadir cuadros en **Multivista**, mezclable con las fuentes existentes hasta el límite actual de cuatro cuadros.
- Los participantes con RC33 pueden resolver la nueva fuente en Watch Party. Versiones anteriores no conocen el proveedor `iptv`.

“10k” es el nombre de la pestaña, no una garantía de un número fijo ni de que todos los enlaces estén activos. La lista usada en la verificación contenía 11.187 entradas HTTP con identificadores distintos. No se comprobó la reproducción de cada canal: algunos pueden estar caídos, limitados por región o usar formatos que el dispositivo no reproduzca. No se eluden restricciones. La capacidad de cuatro vídeos simultáneos sigue dependiendo de la TV y de la conexión.

## Verificación realizada

- Compilación JVM de la nueva capa de fuentes y sus modelos, sin Android.
- **13 pruebas unitarias**: lector M3U, identidades estables, duplicados, protocolos/DRM no soportados, título, episodio exacto, tamaños, año, caché, aislamiento por cuenta/clave y protección ante solicitudes obsoletas.
- Lectura de la lista pública completa mediante el parser Kotlin y consulta pública de prueba a Knaben con respuesta HTTP 200.
- Prueba local de cancelación de una petición HTTP lenta y apertura de otra después de cancelarla.
- Revisión de sintaxis Kotlin y contenido del paquete acumulativo.

**Pendiente:** compilación Android completa en Actions, prueba de foco/interfaz en TV, cifrado con Keystore en el dispositivo y reproducción autenticada con la clave personal de TorBox. No se utilizó ninguna clave personal compartida anteriormente en el chat.

Prueba primero la versión con el motor apagado; después configura tu clave, actívalo y usa pulsación larga para comparar enlaces nativos con tus add-ons. Prueba una película y un episodio de un paquete de temporada. En Canales 10k busca un canal, reproduce, vuelve y comprueba que conserva la búsqueda; después prueba añadirlo a multivista.

## Referencias

- IPTV-org: https://github.com/iptv-org/iptv/blob/master/PLAYLISTS.md
- Lista: https://iptv-org.github.io/iptv/index.category.m3u
- Knaben API: https://knaben.org/api/v1/
- TorBox API: https://api-docs.torbox.app/
- SDK oficial TorBox: https://github.com/TorBox-App/torbox-sdk-py/blob/main/documentation/services/TorrentsService.md
- Arquitectura Comet: https://github.com/g0ldyy/comet/blob/main/docs/README.md
