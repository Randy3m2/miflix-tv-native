# BruniO TV RC68 — Friends inside the player

Aplicar sobre RC66 o RC67: este ZIP incluye todos los cambios de RC67. Reemplazar respetando las rutas y ejecutar el build habitual. No requiere SQL ni Vercel. Versión 2.0.0-rc68, versionCode 22067.

Friends & Party dentro del player muestra los amigos debajo de Create party y Refresh requests, también cuando no hay un party creado. Cada fila compacta incluye avatar, nombre, estado online/offline y actividad si existe. Online primero. Pulsar una fila abre las mismas acciones de amigos: Watch, View Details, Recommendations y Close, con las condiciones de disponibilidad y confirmación de salida del party existentes. Abrir contenido cierra los paneles anteriores.

Se reutilizan friendActivity y la actualización que ya se ejecutaba al abrir el panel. No se añaden sondeos ni consultas por amigo. Se conservan las consultas manuales de solicitudes de acceso.

Incluye RC67: Return to player al explorar fuera del player; pausa automática y punto guardado al salir; publicación de la pausa si eres host; doble Back en Home dentro de 2.2 segundos para cerrar, con aviso inicial. Incluye las opciones de temporadas de RC66 y conserva la reproducción de canales de RC65.

Verificación: comprobaciones de orden, datos de amigos, acciones, claves lazy distintas entre miembros/amigos, reutilización de refresh, archivos espejo y sintaxis Kotlin del panel. Regresiones de pausa/retorno, Home y party aprobadas. Sin compilación Android ni prueba en TV en este entorno.
