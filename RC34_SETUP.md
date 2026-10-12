# BruniO TV RC34 · sesiones, canales y navegación

Versión **2.0.0-rc34**, código **22032**. Proyecto acumulativo sobre RC33.

## Instalar y actualizar

Sube el contenido del ZIP a la raíz del mismo repositorio, conservando la estructura, y ejecuta **Build MiFlix Native TV APK**. Conserva los secretos de firma que ya usas. **No requiere SQL nuevo, otro repositorio ni nuevos secretos**. El workflow ejecuta las pruebas y compila el APK release con la misma firma estable; publica RC34 y actualiza el canal permanente. Esta entrega cambia Android TV; conserva el código PC existente sin modificarlo.

## Cambios y dónde encontrarlos

- **Friends & Party y Settings → Account & Sync → Sync Now:** la sesión se renueva antes de consultar Supabase si el JWT vence en los próximos 90 segundos. Una respuesta HTTP 401 provoca una renovación y un solo reintento. Las solicitudes concurrentes comparten la renovación y reutilizan el token actualizado, incluso si comenzaron con una copia anterior de la sesión. Se guarda la nueva sesión. Un cambio de cuenta o cierre de sesión invalida las solicitudes anteriores. Los 403 no se reintentan como si fueran expiración.
- **My List:** consulta los primeros 20 títulos. **Cargar más / Load more** añade 20; no descarga los detalles de los siguientes automáticamente. Reutiliza los títulos ya cargados al volver de detalles, resuelve hasta cuatro consultas a la vez y separa la caché por cuenta/perfil. Un fallo parcial ofrece **Reintentar**.
- **Live TV → Canales / Channels:** sustituye la lista global 10k por cinco listas: Dominicana, Turquía (idioma turco), México, España y USA. Solo descarga la región seleccionada, con búsqueda y páginas de 200 canales. Conserva Canales de TV y Live Sports. También están en el selector de canales del player y Multivista. Para estas nuevas fuentes regionales, usa RC34 en todos los participantes de una sala.
- **Favoritos de canales:** pulsa **☆** junto al canal para guardarlo o **★** para quitarlo. **★ Favoritos** en Live TV y **★** en el selector del player abren la lista y reproducen directamente. Se guardan localmente por cuenta y perfil en esta TV, incluidos canales de los proveedores existentes.
- **Guía:** las filas muestran **Ahora / Después**, y **Guía** abre la programación y la descripción. **EPG** en Live TV o **Settings → Guía de canales · EPG** permiten guardar otra URL HTTPS XMLTV y actualizarla.
- **Salir del reproductor:** el primer Atrás o clic en X muestra **Pulsa dos veces para salir / Double tap to exit**. El segundo, dentro de 2,2 segundos, sale. Atrás sigue cerrando primero los diálogos abiertos. Moverse a otra opción desarma el aviso.
- **Collections → Franchises:** logos gráficos de Marvel y DC incluidos en el APK, disponibles sin descargarlos en cada visita.
- **Motor nativo:** busca título original/localizado en paralelo, conserva resultados de índice durante dos minutos para reutilizarlos entre episodios de una temporada y permite preparar la reproducción sin esperar el bloqueo de otra búsqueda. Si falla un título alternativo, conserva los resultados del otro. Las cachés se separan por cuenta/clave. Se mantiene la comprobación de TorBox para cada nuevo episodio y antes de reproducir; no se muestran archivos pendientes de descargar. Sigue apagado de fábrica.

## Alcance de la guía

La fuente inicial solicitada es `https://www.open-epg.com/files/argentina.xml`. Sus horarios **no cubren automáticamente** las cinco listas regionales. La app solo vincula automáticamente un identificador de señal exacto, manteniendo el sufijo de país. Si el identificador difiere, abre **Guía → Vincular señal** y elige la señal correcta; **Quitar vínculo** restablece la detección automática. No vincules un canal de otro país solo porque tenga el mismo nombre.

Si no hay correspondencia o programación vigente aparece **Sin guía**. Para otra cobertura puedes cambiar la URL XMLTV en Settings; se admite una fuente a la vez. Se guardan en memoria tres días de programación, se respeta la zona horaria XMLTV y se presenta la hora local de la TV. Caché de seis horas; **Guardar y actualizar** fuerza la descarga. Se rechazan DTD/entidades externas y se limita el tamaño del XML a 12 MB.

Los canales públicos pueden cambiar o estar fuera de servicio. Los números se asignan alfabéticamente dentro de cada lista. No se verificó la reproducción individual de todos los canales.

## Si la cuenta necesita autenticación nueva

Un access token expirado con refresh token válido se recupera automáticamente. Si Supabase revocó la sesión, se cerró desde otro dispositivo o el refresh token ya no es válido, será necesario iniciar sesión otra vez. Esta corrección no desactiva RLS ni modifica permisos de Supabase.

## Verificación

- Compilación JVM de la capa de datos modificada y **32 pruebas unitarias aprobadas**: renovación concurrente, respuestas 401 simultáneas, aislamiento entre cuentas, conservación ante fallos, un solo reintento HTTP, 403, XMLTV/zona horaria/límites de programa/entidades externas, M3U y búsqueda nativa/caché/selección de archivos.
- Parser Kotlin ejecutado contra las cinco listas descargadas: 260, 177, 175, 325 y 1.481 señales respectivamente en esta consulta; cantidades variables.
- XMLTV proporcionado leído con el parser: 226 señales y 13.695 programas dentro de la ventana temporal en esta consulta.
- Sintaxis de todos los archivos Kotlin revisada. Logos inspeccionados.
- **Pendiente:** build Android completa en GitHub Actions y prueba con control remoto/TV. Este entorno no tiene Android SDK. La mejora de latencia se basa en paralelismo, caché y separación de bloqueos; no se ha medido con tu cuenta TorBox o tu conexión.
