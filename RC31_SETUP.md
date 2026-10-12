# RC31 — Reproductor, canales y multivista

Versión 2.0.0-rc31, versionCode 22029. Proyecto acumulativo con RC30 y animación RC29. No requiere SQL adicional. Conserva el SQL RC30 por si aún no se ha instalado; no existe un SQL RC31.

Sube el contenido del ZIP al mismo repositorio y ejecuta **Build MiFlix Native TV APK**. Conserva tus secretos de firma. No se cambia el reproductor de PC.

## Dónde está cada función

### Sincronizar subtítulos
Reproductor → CC → **Sincronizar subtítulos / Subtitle timing**.

Ajusta en pasos de 0,25 s o 1 s, hasta ±30 s. Negativo adelanta los subtítulos; positivo los retrasa. Restablecer vuelve a cero y Aplicar confirma. Afecta las pistas de texto internas y externas gestionadas por el reproductor; no puede mover subtítulos incrustados en la propia imagen de vídeo. Se reinicia al cambiar de reproducción. En archivos que permiten buscar, Aplicar vuelve a leer la posición actual para refrescar los subtítulos y puede producir un breve buffering. En directos no desplazables el efecto se verá en los siguientes subtítulos.

### Canales dentro del player
En Live TV/Live Sports, el botón de lista que para series muestra **Episodios** muestra **Canales**. Abre categorías, búsqueda, logos y números. Cambia de canal sin volver al Home. La búsqueda del catálogo principal permanece independiente. Selecciona el primer enlace que devuelve el proveedor. Las series conservan Episodios.

En party el host cambia el canal común; un guest no cambia por su cuenta el canal de la sala. Multivista solicita salir del party (o cerrarlo si eres host), porque cada cuadro tiene reproducción independiente.

### Información técnica
Icono **i**, arriba en el reproductor: resolución, codec de vídeo, fps, bitrate declarado de vídeo/audio, formato de audio, canales, frecuencia, idioma, nombre del archivo y buffer. Se refresca al cambiar de pista/calidad. El bitrate mostrado proviene del formato activo y no es una medición de velocidad de internet. Si el proveedor no informa el dato aparece No informado; no se muestran URLs ni credenciales.

### Picture in Picture dentro de BruniO
Icono de ventana, arriba: minimiza el mismo reproductor a la esquina inferior derecha y abre Home para navegar. Incluye Play/Pause, Ampliar y Cerrar. Ampliar devuelve ese contenido a pantalla completa; no crea un segundo reproductor. Cambiar de perfil/cuenta cierra la ventana. Es PiP interno: no es una superposición sobre otras apps ni sobre el escritorio Android. Esta elección evita depender del soporte/restricciones de PiP del sistema en Android TV.

### Hasta cuatro canales
En un canal en vivo, icono de cuadrícula arriba → **Multivista**. El canal actual ocupa el primer cuadro. Añade hasta tres más desde Live TV o Live Sports. Pulsa OK sobre un cuadro para escuchar ese canal, cambiarlo, reintentar, quitarlo o abrirlo a pantalla completa. Solo un canal tiene audio habilitado a la vez. Cambiar la selección de audio no reinicia el vídeo.

- **Fluido · 540p** es la preferencia inicial: hasta 30 fps y 1,8 Mbps por canal cuando el enlace ofrece variantes compatibles.
- **HD · 720p** prefiere hasta 3 Mbps por canal.
- Estas preferencias no transcodifican ni reducen un archivo de calidad fija. Si solo hay una variante, el proveedor puede obligar a reproducir esa calidad.
- La fluidez depende del chip/decodificadores de la TV, resolución/codecs, conexión y límites de conexiones simultáneas del proveedor. No se puede garantizar 4 canales fluidos en todas las TVs. Ante fallos, prueba Fluido o reduce a 2 canales.
- Se escalona la creación de los reproductores, se limita su buffer y se liberan antes de volver a pantalla completa. Si falla un enlace se ofrece cambiar/reintentar ese cuadro. Al salir se liberan todos; al dejar la app se pausan.

## Validación y prueba de TV
Revisada sintaxis Kotlin, cambios respecto a RC30 y APIs consultadas en documentación oficial Media3. No hay SDK/compilador Android en este entorno: compilación pendiente en Actions. Las funciones nuevas necesitan prueba de dispositivo real.

Prueba primero un archivo con subtítulos internos y otro con subtítulos externos: aplica +1 y −1 segundo, pausa/reanuda, avanza y restablece. Prueba cambiar audio y revisar Información. Abre PiP, navega entre Home y una ficha, amplía el contenido original y cierra la ventana. En directo cambia canales sin salir. Finalmente abre 2, 3 y 4 canales, cambia el audio, reemplaza/quita uno, vuelve a pantalla completa y sal; confirma que no quedan audios/reproductores ocultos. Prueba el aviso al entrar en multivista desde un party.

Referencias técnicas: https://developer.android.com/reference/androidx/media3/exoplayer/Renderer y https://developer.android.com/training/tv/get-started/multitasking
