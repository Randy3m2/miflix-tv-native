BruniO RP64 — My List 5×3 y acciones con foco blanco
Versión 2.0.0-rp64 / versionCode 22168.
Aplicar SOBRE RP63, reemplazando los archivos con sus rutas intactas.

My List
- Cinco tarjetas por fila. Primer lote de 15 títulos; Cargar 15 más en cada
  pulsación, hasta agotar los favoritos disponibles.
- Se sigue la última indicación de 15 (reemplaza la de 16).
- Se conservan Todo/Películas/Series, orden, filtros locales, progreso,
  badges, pulsación larga y regreso desde detalles.
- Márgenes laterales de 16 dp, más 4 dp internos de la cuadrícula, sin el
  ancho máximo anterior que dejaba espacio vacío en los extremos.
- La tarjeta completa recibe foco: imagen, título y metadatos se desplazan
  juntos y el foco ya no se limita al póster.
- Zona de título con hasta tres líneas; al enfocarla, el texto largo se
  desplaza horizontalmente para poder leerlo completo.
- Altura de póster ajustada al espacio visible, preservando la zona del
  título. Imagen completa con Fit, sin estirar ni recortar el póster.
- Top preview reducido de 108 a 80 dp. Marcadores de carga para el primer
  lote mientras llegan los metadatos, sin contar esos marcadores como
  títulos ya cargados. La red y los títulos no disponibles pueden limitar
  el número de resultados; no se descargan todas las carátulas de golpe.

Botones de acciones
- Fondo blanco y texto/iconos oscuros SOLO para la acción que tiene foco.
- Las opciones activadas/seleccionadas dejan de conservar fondo blanco
  cuando el foco está en otro botón. Mantienen borde o estado correspondiente.
- Acciones comunes, paneles modernos, ajustes, Load More, filtros,
  teclado de búsqueda, selectores deportivos y controles del player.
- También editar/agregar en el selector de perfiles.
- Los botones deshabilitados no se rellenan de blanco.
- Se comprueba el foco de la ventana para que un botón detrás de un diálogo
  no conserve relleno blanco mientras otra acción del diálogo tiene foco.
- La barra superior, sus funciones y los pósteres/canales NO cambian de
  estilo por este ajuste. Se preserva el shimmer anterior de Home.
- No cambian las acciones ni tiempos de pulsación de los controles.

Validación
- Sintaxis de los 14 archivos Kotlin verificada con Kotlin 2.3.21.
- Comparación de componentes: barra superior y código de MediaCard
  idénticos a la revisión anterior aplicable (RP58), salvo los dos botones
  de acciones compartidos modificados. Teclado: solo la tecla de acción;
  el resto de CinematicBrowsing está intacto.
- Revisión de límites 15/15, cinco columnas, zona del título y estados de
  foco/deshabilitado/ventana.
- Sin compilación Android completa ni prueba en TV. No se genera APK.
  El entorno sigue sin SDK Android ni firma de actualización configurados.
  Compilar mediante el workflow habitual de GitHub después de aplicar.
- Sin SQL ni cambios de Vercel. El formato de 12 horas de RP63 se conserva.
