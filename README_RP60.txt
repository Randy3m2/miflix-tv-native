BruniO RP60 / 22164 — enfoque vertical inmediato

Aplicar sobre RP59 respetando rutas. 5 archivos.

Comparación de ambos videos: en Nuvio las tarjetas reciben enfoque al navegar mientras la lista las acompaña. En RP59 el código entrega el enfoque después de terminar una animación de 210 ms; mantener la flecha puede sustituir el destino repetidamente antes de enfocar la tarjeta.
Se retira ese controlador vertical y la captura manual de Arriba/Abajo. Compose vuelve a gestionar la búsqueda de enfoque y el desplazamiento de su LazyColumn, igual que la infraestructura usada en los laterales.
Se habilita BringIntoView vertical, antes bloqueado devolviendo cero. Un margen superior de hasta 48 dp conserva espacio para el título de fila. No hay solicitud de enfoque posterior a una animación manual.
Se mantienen tamaños de tarjetas y filas, código de anclaje/repetición horizontal, carga con brillo, retratos de directores y perfiles de versiones anteriores. Sin cambios en fuentes de contenido, reproductor o Supabase.

Revisado: videos por cuadros; eliminación del controlador manual y de sus llamadas; código horizontal y perfiles conservados; rutas/versión y ZIP.
Sin compilación, Gradle, APK ni prueba en TV. El entorno no dispone de compilador Kotlin/javac.
Validar en TV: subir/bajar rápido, mantener flecha, invertir dirección, regresar a barra superior y volver desde detalles. No se afirma rendimiento idéntico a Nuvio sin esa prueba.
