# BruniO PC · Compose

Cliente Windows x64 reconstruido en Kotlin/Compose, con interfaz oscura, botones de vidrio, navegación lateral superpuesta y controles basados en BruniO TV. El instalador incluye Java, VLC y el servicio local Python. No requiere instalar esos componentes por separado. Los cambios de esta reconstrucción se limitan a PC; no modifican `app/` ni `mobile/`.

## Funciones conectadas

- Cuenta y perfiles: restauración de sesión, login, selección, creación, nombre, avatares y eliminación mediante los RPC de TV. Tokens protegidos por DPAPI en Windows.
- Inicio: carrusel con navegación integrada, Seguir viendo, Streaming, Géneros, recomendaciones por valoraciones y catálogos. Los GIF de plataformas se activan al pasar el mouse; AMC utiliza su portada incluida. Búsqueda y Mi lista.
- Fichas: sinopsis, reparto, filmografía, valoraciones, lista, visto/no visto, temporadas y episodios. Imágenes y detalles de episodios no vistos ocultos para evitar spoilers.
- Reproducción con VLC: reanudación, progreso, fullscreen, PiP, cambio de fuente, pistas de audio/subtítulos, volumen, teclado, controles que se ocultan, retry limitado a las fuentes disponibles y autoplay/cola. El motor detecta errores de arranque y cuadros de vídeo detenidos durante la reproducción.
- Complementos y fuentes: configuración, prioridades por perfil/idioma, límite de resultados por complemento y límites de tamaño de reproducción/descarga propios de PC.
- Live TV y deportes: cuadrículas, catálogos de TV, favoritos locales por perfil, selector de canales, Multiview de hasta cuatro paneles con un solo audio, marcadores fijados y movibles.
- Amigos y Party: nombre de usuario por perfil social, actividad, presencia, solicitudes de amistad, creación/unión con aprobación, chat, reacciones y sincronización del estado de reproducción con el protocolo de TV. Las solicitudes de acceso también aparecen fuera del chat mientras el anfitrión reproduce.
- Calendario: eventos compartidos de TV, navegación mensual/día y suscripciones deportivas con los mismos identificadores de equipos. Notificaciones de novedades de la lista.
- Descargas: películas/episodios/temporadas, portadas, agrupación por temporada, progreso, cancelación, eliminación y reproducción offline. Las URLs de fuentes no se guardan en la base de descargas. Las transferencias interrumpidas se muestran como tales; no se promete reanudación automática al reiniciar.
- Ajustes: audio/subtítulos, escala de subtítulos, interfaz español/inglés, límites, caché y búsqueda de actualizaciones. Las actualizaciones validan canal PC, repositorio, versión, tamaño y SHA256 antes de abrir el instalador.

El proceso local intercambia JSON por stdio: no abre puertos HTTP ni devuelve credenciales/configuraciones privadas a la interfaz. Reutiliza los módulos de protocolo del cliente anterior, sin incluir su interfaz Qt en el instalador nuevo. El instalador conserva los datos de usuario de PC y reemplaza sus archivos de programa. El entorno de desarrollo usa una carpeta de descargas separada.

## Compilación y descarga

Workflow: **BruniO PC Compose Checks (development)**, archivo `.github/workflows/pc-compose-checks.yml`.

Las comprobaciones de PR/push generan un artefacto `BruniO-PC-Compose-Preview-…` con `BruniO-PC-Setup-2.0.0-rcN.exe` y `pc-update.json`. Para instalar una prueba, descarga el artefacto, extrae el ZIP y ejecuta el `.exe`. No uses los APK de TV/móvil ni los instaladores del cliente antiguo.

La ejecución manual permite marcar **publish_pc**, desmarcado por defecto. Sólo tras pasar las comprobaciones, publica el instalador en `pc-v2.0.0-rcN` y actualiza `pc-latest/pc-update.json`. El botón de actualización dentro de la app lee ese canal; una build de prueba sin publicar no aparecerá allí. No se publica ni se cambia el canal por un push o PR.

## Verificación y límites

El workflow ejecuta las pruebas Python, compila Kotlin/Compose, comprueba navegación y condiciones del reproductor, decodifica un vídeo AVI original mediante VLC y prueba el arranque del paquete con su propio servicio y runtime. Después captura las pestañas, perfiles, controles y ficha de contenido en Windows usando datos de prueba explícitos. El empaquetado falla si no se decodifican las imágenes locales requeridas. Estas capturas no contienen la cuenta personal ni prueban sus proveedores.

La suite Python contiene 63 pruebas de protocolo, aislamiento entre perfiles, descargas, calendario, Party y actualización. Se añadió una regresión para PNG que incluyen metadatos SVG: deben conservarse como imágenes rasterizadas.

La compilación, el arranque autónomo, la reproducción nativa de prueba y las capturas no sustituyen una prueba con la cuenta personal. No se afirma paridad completa con todas las funciones de TV. La disponibilidad de fuentes, compatibilidad de cada canal/códec, sincronización entre dispositivos reales y rendimiento Multiview/4K requieren validación con esos servicios y equipos. Mi lista carga actualmente hasta 100 elementos. Los favoritos Live son locales por perfil. La traducción cubre la interfaz principal; algunos mensajes dinámicos pueden conservar el idioma original.

## Desarrollo

Desde la raíz:

```sh
python -m unittest discover -s desktop/brunio-pc-next/tests -v
```

Desde `desktop/brunio-pc-next/compose`, con JDK 17, Gradle 8.14.3 y Python:

```sh
gradle test run
```

Para desarrollo de vídeo, instala VLC x64 o define `BRUNIO_VLC_HOME`. `BRUNIO_PYTHON` y `BRUNIO_BRIDGE` permiten seleccionar ejecutable Python y ruta del servicio. `scripts/build_windows.ps1` prepara el paquete autónomo después de verificar VLC en el workflow. Las licencias están en `THIRD_PARTY_NOTICES.md` y se incluyen en el instalador.
