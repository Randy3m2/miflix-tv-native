BruniO Android TV - RP15 / 2.0.0-rp15 / 22119

Aplicar SOBRE RP14 conservando las rutas y reemplazando los archivos.

- Pildora compacta: 156 x 40 dp, logos y marcador, sin nombres visibles.
- Margenes interiores y separacion de logos reducidos; borde de pantalla 4 dp.
- Tres pulsaciones separadas de Arriba dentro de un segundo abren las
  opciones del marcador: Mover y Unpin. Mantener Arriba no cuenta.
- Mover: usar flechas; OK o Atras guardan la posicion. Sigue disponible
  arrastrar con puntero. Posicion y pin conservan su almacenamiento por perfil.
- OK sobre el marcador tambien abre las opciones. Unpin elimina el pin.
- Live TV: nuevo boton Live Scores junto a Favoritos. Desde alli se puede
  fijar un partido o quitarlo; el partido ya fijado muestra Unpin.
- Se conserva la frecuencia de actualizacion y cache de RP11. Este cambio
  no agrega consultas periodicas ni consultas a Supabase.

Verificacion: 5 pruebas JVM del atajo aprobadas; sintaxis de UI comprobada y
revision de foco/consumo de teclas. Validacion del control/foco en TV pendiente.
No se genero APK ni se ejecuto Gradle Android. Sin SQL ni Vercel.
