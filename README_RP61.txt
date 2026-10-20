BruniO RP61 / 22165
Aplicar sobre RP60 conservando rutas. No incluye APK.

IPTV +
- Catálogo separado de Tirso y del IPTV actual, accesible desde las categorías de Live TV y desde Channels dentro del reproductor/Split Screen.
- La lista descargada contenía 863 entradas únicas, aunque su archivo indica 939. Se conservan sus URL.
- Clasificación por nombre (la fuente casi no trae grupos): General, Cine, Series, Deportes, Infantiles, España, Cultura, Noticias, Novelas, Música, Religión y Adultos.
- 208 entradas reutilizan logos ya existentes. La fuente no suministra logos; el resto conserva la presentación de respaldo de la app.
- Sin modificar el JSON de Tirso, sus 220 canales, imágenes, identificadores ni favoritos. Numeración propia para IPTV +. Se conserva el menú de pulsación temporizada de RP55.

Tus listas M3U
- Live TV y Channels: botón Añadir lista M3U por QR. Celular y TV deben estar en la misma red.
- Archivo M3U o URL HTTP(S), con nombre propio. Importación explícita, guardada como instantánea en este perfil de esta TV; no se actualiza sola.
- Categorías y logos de la lista; conserva User-Agent/Referer cuando los incluye en las directivas compatibles con el parser existente.
- Listas independientes, opción Mis listas para eliminarlas y menú de canal existente para eliminar canales individuales.
- Máximo 10 listas por perfil, 2 MB / 5000 canales por lista. No se modifica el archivo original ni se escribe en Supabase.
- Son listas de canales; una URL M3U8 de segmentos de un único video no se convierte en un catálogo.

Scroll vertical
- Mantiene el enfoque nativo de RP60, reduce la distancia aplicada por cuadro y consume Arriba/Abajo mientras está desplazándose. No acumula saltos.
- Guarda por identidad la tarjeta de cada fila de títulos/plataformas/franquicias. Al entrar desde arriba o abajo recupera esa tarjeta; si no hay selección previa, restaura la primera.
- No altera el algoritmo de navegación horizontal. Mantiene perfiles y ajustes de RP60.

Todos los equipos de LIDOM · fuentes por QR
- LIDOM > cualquier equipo > Fuentes > Fuente por QR.
- Pega YouTube (video/transmisión/canal), HLS (.m3u8) o DASH (.mpd). Se guarda en ese equipo y perfil.
- Permite varias fuentes por equipo; no reemplaza las anteriores. Una URL ya guardada actualiza su entrada, evitando duplicarla. Se pueden editar o eliminar en la configuración de fuentes existente.
- Un video directo se abre al seleccionar la fuente; un canal utiliza el selector de videos existente. Se conserva el último video funcional del sistema existente.
- Reutiliza los reproductores de YouTube de pantalla completa y Split Screen. Depende de que la transmisión permita reproducción insertada.

Comprobaciones
- Compilación Java del receptor con las clases de acceso LAN reales.
- Pruebas HTTP del formulario en transporte local aislado: archivo UTF-8, URL, rechazo de envío vacío y token inválido. El transporte de prueba adapta la interfaz LAN a loopback; no sustituye la prueba TV/celular.
- Pruebas de validación YouTube: canal, handle, video corto, URL live y rechazo de host incorrecto.
- Revisión estática de separación de catálogos, registro en Live TV/Channels, headers, restauración por fila, conservación de código horizontal y perfiles, numeración y archivo ZIP.
- No se compiló Kotlin ni la app Android. No se generó APK ni se comprobó la disponibilidad de las 863 señales.
- Pendiente en TV: subida QR real desde celular, búsqueda, favoritos/eliminación, cambio de canal, Split Screen, YouTube y la suavidad del scroll en ambos sentidos.
