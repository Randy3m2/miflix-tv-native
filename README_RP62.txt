BruniO RP62 — Mandanguita TV, reproducción directa e idioma secundario
Versión: 2.0.0-rp62 / versionCode 22166. Aplicar SOBRE RP61.

Instalación
Extraer y reemplazar los archivos conservando exactamente sus rutas.
Los cuatro .kt de app/src/main/java/com/miflix/native2/p2p y su antigua
prueba TorrentSpecRP14Test.kt están vacíos intencionalmente. SÍ hay que
reemplazarlos: eliminan el código anterior incluso al aplicar por ZIP.
No generan clases del motor y pueden borrarse después del repositorio.

Mandanguita TV
- Manifest facilitado por el usuario incluido entre las fuentes de fábrica.
- VOD mediante las rutas stream/movie/IMDb y stream/series/IMDb:season:episode.
- El manifest ya ofrece esa API; no se añade un scraper ni se usa el login
  privado de Xuper. No necesita una página de configuración externa.
- Prioridad inicial: primera con audio principal español; segunda en los
  demás idiomas. El resto mantiene su orden. La migración se aplica una
  vez por perfil/idioma; después se puede reordenar desde Settings.
- Disponible de fábrica también en perfiles sin fuentes y sin sesión.
- Puede eliminarse en Settings; se respeta esa elección en esta TV.
- Sigue las reglas existentes de 7 enlaces visibles y Load More.
- Excepción de tamaño desconocido: no se ocultan sus enlaces por faltar GB.
  Si informa un tamaño, el límite seleccionado se sigue aplicando.
- Se conserva la prioridad del idioma al ordenar enlaces: la posición del
  add-on decide dentro del grupo de audio correspondiente. No se atribuye
  audio español a un enlace que no declara su idioma.
- Solo integración de enlaces VOD. No añade sus canales de TV ni descarga
  su catálogo completo al iniciar.

Idioma secundario
Settings > Idioma > Preferencias de reproducción > Idioma secundario.
- Opciones: desactivado, español, inglés, portugués y francés.
- Se guarda localmente por cuenta/perfil; no cambia interfaz ni subtítulos.
- Orden de fuentes: principal, secundario, idioma desconocido, otros.
- Preferencias de pistas del player: principal, después secundario.
- Conserva Latinoamérica antes de España al elegir pistas españolas.
- La selección manual de pistas guardada para una serie sigue teniendo
  preferencia, como antes. Los idiomas necesitan metadatos fiables; no
  se traduce ni se transforma el audio de una fuente.
- Si no se configura secundario, conserva el fallback anterior.

P2P retirado
- Eliminadas las 5 dependencias libtorrent (wrapper y 4 arquitecturas).
- Eliminados motor, lector de torrents, ajustes y opciones del player.
- Respuestas magnet/infoHash sin URL directa ya no se reproducen.
- HTTP/HTTPS, fuentes debrid directas y motor HTTP nativo se conservan.
- Los enlaces torrent antiguos guardados se ignoran.
- Al iniciar, limpieza del antiguo caché p2p-rp14 y sus preferencias.
- No se calcula la reducción exacta del APK porque no se ha generado uno.

Scroll vertical
- Ajuste de distancia por fotograma de 0.55 a 0.72: un poco más rápido
  que RP61. Conserva esperar la transición y recordar la tarjeta de cada
  fila. No modifica la navegación horizontal.

Impacto de red
- Sin tablas SQL, cambios web ni nuevos sondeos a Supabase.
- Una fuente adicional consultada en paralelo dentro del flujo de enlaces
  existente; reutiliza su caché. Metadatos del manifest: caché de 6 horas.
- La latencia y disponibilidad del proveedor siguen siendo externas.

Validación
- Inspección real del manifest y respuesta VOD con enlaces HTTPS .ts.
- 49 pruebas JVM de la lógica de enlaces pasaron (incluye defaults,
  prioridad, eliminación, filtros, idioma secundario y rechazo torrents).
- En esas pruebas se compilaron los componentes de datos Kotlin/Java
  aislados, con los límites Android/red sustituidos por dobles de prueba.
- Revisión de sintaxis Kotlin de los archivos modificados.
- Auditoría de fuentes combinadas: sin referencias ejecutables al motor
  P2P ni dependencias libtorrent.
- NO se ha ejecutado Gradle ni compilado APK; NO se ha probado en TV.
