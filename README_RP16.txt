BruniO Android TV - RP16 / 2.0.0-rp16 / 22120

Aplicar SOBRE RP15, conservando rutas y reemplazando los archivos.

ULTIMO ENLACE
Al reanudar una pelicula o el mismo episodio con progreso, se prueba primero
el ultimo enlace que mostro un fotograma correctamente, sin consultar antes
todos los add-ons. Una seleccion manual sigue teniendo prioridad.

Se guardan URL, cabeceras, subtitulos y datos del torrent/archivo P2P en esta TV.
Separados por cuenta, perfil, titulo y episodio. Cambiar idioma, complementos,
limite de tamano o configuracion del motor impide reutilizar datos incompatibles.
Se conservan hasta 60 fuentes, con un maximo total de 1 MB.
No se sincronizan enlaces con Supabase ni con otros dispositivos.

Si falla, se elimina el enlace recordado. Con siguiente enlace automatico
activado, se buscan fuentes nuevas y se respetan las prioridades de idioma.
Desactivado: se muestra el error para elegir otro enlace manualmente.
No evita tiempos propios del proveedor o la conexion con peers P2P.
Se aprende a partir de esta version, tras reproducir correctamente cada titulo.
No se reutiliza automaticamente en Watch Party, TV en vivo o momentos compartidos.

DOBLE ATRAS DURANTE LA CARGA
Primer Atras: muestra aviso. Segundo Atras dentro de 2.2 segundos: cancela el
inicio y vuelve a los detalles del contenido en carga. El trabajo invalidado
no puede volver a abrir el player al terminar una consulta tardia.
Al salir del player P2P se conserva su limpieza de cache habitual.

VALIDACION
52 pruebas JVM aprobadas: 9 nuevas de persistencia y doble Atras, mas las
regresiones de enlaces, P2P y atajo de marcador. Sintaxis de los cambios revisada.
No se ejecuto Gradle Android ni se genero APK. Falta validar en TV real.
Sin SQL, cambios en Vercel ni nuevas consultas periodicas a Supabase.
