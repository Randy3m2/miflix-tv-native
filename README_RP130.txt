BruniO RP130 — SplitScreen Stats
Version: 2.0.0-rp130 / 22234

APLICAR
Extraer sobre el proyecto actualizado a RP129, conservando las rutas.
9 archivos. No requiere SQL. No incluye APK.

COMPORTAMIENTO
- 2 videos: Stats independiente por pantalla; resumen glass en la franja negra superior.
  Marcador, logos disponibles, fase y tabla de equipo (NBA: PTS/REB/AST; béisbol: R/H/E).
- 3 videos: el cuarto cuadro es un panel de datos, sin crear un cuarto reproductor.
  Tres botones permiten escoger la pantalla; tabla compacta con jugadores y columnas.
  En MLB/LIDOM hay Bateo y Pitcheo. El equipo y las columnas se pueden cambiar.
- 1 o 4 videos, o video YouTube expandido: no se inserta este panel.
- Botón SplitScreen en un pie reservado de 52 dp; no ocupa la esquina superior derecha.
- Stats y SplitScreen usan un único clickable de Compose, sin duplicar manejadores de OK.
- X cierra Stats y retorna el foco a SplitScreen. Atrás cierra Stats antes de salir.
- El refresco no solicita foco; conserva la navegación estable de la tabla RP106.
- La selección y el audio de los videos permanecen independientes.
- Los reproductores conservan sus claves al abrir/cerrar/cambiar Stats.
- Si no hay altura suficiente para un resumen superior, se ofrece el selector lateral.

DATOS REALES
NBA: resumen ESPN; MLB y LIDOM: box score de MLB Stats API.
Se intenta identificar por los dos equipos; LIDOM permite un canal de equipo solo
cuando existe un único partido compatible. Si no hay coincidencia, elegir Partido.
Cada pantalla guarda su partido durante esta sesión SplitScreen.
Para LIDOM reutiliza logos de lidom_teams.json; no inventa imágenes ni estadísticas.
La coincidencia automática nunca elige arbitrariamente el primer partido.
F1 NO tiene en esta entrega una fuente de tiempos en vivo: los datos F1 de la imagen
conceptual eran ilustrativos. No se muestran tiempos ficticios ni posiciones históricas
como si fueran en vivo. El video F1 continúa funcionando.
La disponibilidad y el detalle de LIDOM dependen de lo que publique el proveedor.
Los datos deportivos pueden ir por delante de la transmisión.

RENDIMIENTO
Red cancelable y parseo fuera del hilo de interfaz, caché local existente,
sin blur en tiempo real, sin nuevas consultas/presencia/logs de Supabase.
Actualiza al abrir; cada 10 s en vivo o 30 s prepartido, con backoff ante errores.
Conserva el último dato si falla. Se detiene al cerrar Stats, finalizar el partido,
salir del multiview o pasar la app al fondo.

VERIFICACIÓN REALIZADA
- 30 pruebas JVM aprobadas: datos NBA/MLB, columnas, navegación más allá de la quinta
  fila, caché/cancelación, cambios de partido, modos 2/3/4 y ruta LIDOM.
- Sintaxis PSI de todos los archivos Kotlin del parche: cero errores.
- Compilación parcial con Kotlin/Compose 2.3.21 y artefactos Compose Android 1.11.4:
  cuerpo modificado de MultiViewScreen, paneles Stats y políticas. AppState,
  reproductor y servicios ajenos al cambio se sustituyeron por stubs solo en esa
  comprobación; no están incluidos en el parche.
- Revisión estática: pie separado, un solo manejador OK por botón nuevo,
  claves de reproductor estables y limpieza de selecciones de canales retirados.

PENDIENTE
CI Android/Gradle completo y prueba en TV con mando y streams reales:
abrir/cerrar repetidamente, pulsaciones rápidas, 2→3→4→3 pantallas, cambiar audio,
elegir otro partido, scroll prolongado de jugadores, cierre con X/Atrás y reanudar.
Las pruebas anteriores no sustituyen esa validación en dispositivo.
