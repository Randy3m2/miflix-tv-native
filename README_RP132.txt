BruniO RP132 — Logos de equipos y fotos F1
Version: 2.0.0-rp132 / 22236

APLICAR
Acumulativo: incluye RP130 y RP131 completos. Extraer conservando las rutas sobre
RP129, RP130 o RP131. 15 archivos. No SQL ni APK.

CAMBIOS RP132
- Selectores de equipos NBA/MLB/LIDOM: solo logo, con selección/foco glass.
- Marcadores y resúmenes: logos en lugar de nombres/abreviaturas de equipos.
- El nombre sigue en accesibilidad, sin mostrarse en el botón.
- Las listas para elegir partido conservan sus nombres para identificar encuentros.
- F1: fotos reales de pilotos, sustituyendo las banderas usadas como avatar.
- Usa las mismas funciones de tabla que Stats normal: cabecera fija, filas con foto,
  columnas alineadas y un foco estable que permite recorrer todos los pilotos.
- Tanto el resumen de dos videos como la tabla del cuarto cuadro permiten scroll;
  el número visible de filas depende de la altura disponible.
- Fotos de pilotos cargadas solo para las filas visibles/prefetch habitual y
  decodificadas a 64px por la tabla. Logos de equipos a 80px; caché Coil existente.
- Contenedores de tamaño fijo evitan saltos cuando termina de cargar una imagen.
- Sin API adicional por piloto ni consultas nuevas a Supabase.

IMÁGENES
22 retratos de carrera verificados HTTP 200 image/png desde ESPN, por su ID de piloto.
Colton Herta (entrada adicional de FP1, ID ESPN 5892) usa su retrato oficial de FIA F2,
verificado HTTP 200 image/jpeg, porque ESPN no publica uno para ese ID.
Si el feed ya proporciona un retrato HTTPS válido, se respeta primero.
Si la imagen falla, la tabla conserva la inicial del piloto; el logo de equipo muestra
un símbolo discreto, conservando el nombre accesible. Nunca cambia la identidad del dato.
Imágenes remotas: su disponibilidad futura depende del proveedor.

COMPORTAMIENTO INCLUIDO RP130/RP131
2 pantallas: Stats encima de cada video. 3: cuarto cuadro con selector de pantalla.
SplitScreen en pie reservado; X/Atrás cierran Stats; no recrea videos ni cambia audio.
F1 por ESPN: posiciones y vuelta general publicadas, selector de sesión y Auto.
Final/Programada identificados; intervalos ausentes muestran —, sin inventar datos.
Título ambiguo: OK sobre video → Stats · F1; volver con Stats · NBA / MLB / LIDOM.
F1 refresca 15s en vivo según ESPN, 30s fuera de sesión; caché compartida de 10s.
Cierra/cancela al ocultar Stats, salir o pasar la app al fondo.
NBA/MLB/LIDOM mantienen sus datos y controles existentes.

VERIFICACIÓN
40 pruebas JVM de datos, retratos, selección, columnas, navegación, caché/cancelación
incluyendo los fixtures NBA/MLB/F1 y la ruta LIDOM.
Compilación parcial de las cuatro UI Stats, helpers y datos con Kotlin/Compose 2.3.21,
Compose Android 1.11.4 y Coil 3.5.0; AppState y HTTP se sustituyen por stubs de prueba,
que no se incluyen. No equivale a compilar el módulo Android entero.

PENDIENTE
CI Android/Gradle completo y validación visual/mando en TV con streams reales.
F1 se consultó con una sesión Final; no se validó latencia durante carrera en vivo.
