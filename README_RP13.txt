BruniO TV RP13 — Peerflix configurado
Acumulativo sobre RP6–RP12: conserva RP12 completo.
Sustituye el Peerflix genérico de fábrica por la configuración suministrada
de addon.peerflix.mov. El anterior no se consulta como predeterminado.
Addon Latam y Torrentio free permanecen iguales.

Verificación real:
Manifest accesible, name=Peerflix, configurationRequired=false.
Prueba de película: 21 resultados infoHash/torrent, 0 enlaces HTTP(S).
Por tanto el manifest funciona, pero ESTA VERSIÓN NO LOS REPRODUCE:
BruniO sigue sin motor P2P. Se muestra el diagnóstico correspondiente.
Esta configuración no convierte torrents en enlaces directos.
Sigue siendo eliminable desde Settings y sigue la paginación de RP12.
No se generó APK ni se compiló Android. Sin SQL ni Vercel.

Detalle heredado de RP12:
Rendimiento en ambos idiomas:
Las consultas a stream y manifest se inician en paralelo por proveedor.
Transporte HTTP de add-ons con conexiones reutilizadas, cancelación real y plazo
total de 8 s para enlaces / 2 s para metadatos. Antes los timeouts eran 18/8 s y
se ejecutaban secuencialmente dentro de cada proveedor.
Se consultan todos los proveedores en paralelo y se ordenan los resultados al
terminar/responder o agotar el plazo; no se cambia de idioma por ganar una carrera.
Español conserva Latino > España > Castellano > genérico > inglés.
Inglés conserva su prioridad de idioma y Comet/Torrentio personales.
No espera hasta 800 ms por subtítulos externos: los ya disponibles se adjuntan;
los tardíos permanecen accesibles mediante el menú CC.
La búsqueda del motor nativo opcional tiene plazo de 10 s en lugar de 25 s.
La preparación de vídeo y sus propios tiempos de red siguen dependiendo de la
fuente. No es una garantía de inicio instantáneo.

Enlaces:
7 visibles por add-on por defecto, en español e inglés. Load more al final muestra
otra tanda por cada add-on del filtro actual, sin volver a consultar ni borrar
resultados. La caché conserva toda la respuesta y los reintentos pueden usarla.
La fuente que ya está reproduciéndose se mantiene visible aunque quede fuera
de los primeros siete. Las preferencias de 5/10/15/custom/All ya elegidas se
respetan; ahora indican el tamaño de tanda. Se añade el preset 7.
Paginación visual: el protocolo estándar stream devuelve la respuesta completa;
no se promete que el servidor calcule solo siete. El ahorro de espera viene de
consultas paralelas, límites de tiempo y menor carga del listado en pantalla.

Fábrica, solo:
Peerflix: configuración suministrada de addon.peerflix.mov
Addon Latam: la configuración que ya se suministró.
Torrentio free: sort=qualitysize|language=latino,spanish, sin clave de debrid.
El carácter | se guarda como %7C, equivalente y válido para URI.
MG, Progreso Latino y la instancia Nuve+ anterior dejan de insertarse y de
consultarse como fábrica, incluso si quedaron copiadas en listas antiguas.
Otras URLs personales permanecen. Se puede volver a agregar una retirada
explícitamente en Settings; esa acción se recuerda para no retirarla otra vez.
Los nuevos predeterminados siguen siendo eliminables desde Settings, también sin sesión; la exclusión se guarda localmente.

Límite funcional importante:
Esta versión reproduce enlaces HTTP(S). NO incorpora motor P2P.
Si Peerflix/Torrentio free devuelven infoHash/magnet sin URL directa, no son
reproducibles aquí. Se diagnostican como Requiere motor P2P y se explica el caso
cuando no hay enlaces directos. Agregar el manifest no convierte torrents a HTTP.
El Peerflix genérico y Torrentio respondieron 403 desde este entorno. El nuevo
Peerflix configurado sí devolvió torrents, como se indica arriba. No se garantiza que todos los nuevos proveedores reproduzcan.

Validación:
31 pruebas JVM correctas: 6 arranque/fábrica, 3 paginación, 1 transporte HTTP y
21 ordenación. La prueba de transporte usa un servidor local deliberadamente
lento para comprobar el vencimiento y los errores HTTP sin exponer URLs.
Parser Kotlin sin errores nuevos frente a RP11 y archivos nuevos sin errores.
No se compiló Android ni se generó APK. Faltan CI y pruebas reales de inicio,
foco y reproducción en TV. No requiere SQL ni Vercel ni añade consultas Supabase.
