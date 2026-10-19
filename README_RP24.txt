BruniO Android TV - RP24 / 2.0.0-rp24 / 22128
Aplicar SOBRE RP23, conservando rutas y reemplazando archivos.

Nuevo diseno para Local Filters de My List y los catalogos que lo usan:
- Panel negro con tarjetas por tipo, visibilidad, orden y ano.
- Opciones compactas con seleccion marcada y borde blanco al enfocar.
- Contador de resultados y descripcion de la opcion enfocada a la derecha.
- Contenido desplazable, acciones Cerrar, Restablecer y Listo siempre visibles.
- En ventanas pequenas la ayuda pasa debajo de los controles.

Se conserva la logica anterior de filtrado y orden, y las preferencias por
perfil/catalogo. Se aplica inmediatamente sobre titulos cargados, sin nuevas
busquedas. No cambia My List, el historial ni el progreso.

Validacion: sintaxis Kotlin revisada, entrada del dialogo conectada y politica
de filtrado comparada con la anterior sin cambios. Rutas espejo verificadas.
Falta validar distribucion y foco en TV real. No se genero APK ni se ejecuto
Gradle Android. Sin SQL, Vercel ni nuevas consultas a Supabase.
