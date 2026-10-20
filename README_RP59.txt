BruniO RP59 / 22163 — navegación vertical de Home

Aplicar sobre RP58 respetando las rutas. 5 archivos.

Revisado el video adjunto: pausas entre filas y desplazamientos bruscos. Se cambia el control vertical de Home en ambos sentidos.
Un único controlador recibe las flechas, alinea la fila y después entrega el enfoque a sus tarjetas, sin esperar imágenes. Evita que búsqueda de enfoque y alineación manual compitan durante el mismo movimiento.
Transición finita de 210 ms; una nueva pulsación sustituye el destino anterior. No acumula animaciones. Se conserva el límite de repeticiones de RP58.
Al llegar al primer row, Arriba permite volver a la barra superior. No se avanza más allá de la última fila. Las filas informativas sin elementos enfocables se saltan.
Movimiento lateral, anclaje horizontal, cadencia horizontal, carátulas, brillo, directores y perfiles se conservan de RP58.

Verificado estáticamente: rutas, versión RP59/22163, ambos sentidos, conservación del código horizontal y perfiles. Sin Gradle, APK ni prueba en TV. No hay compilador Kotlin/javac disponible.
Pendiente probar en TV: pulsaciones cortas y mantenidas, invertir dirección durante una transición, extremos y vuelta a Home desde detalles. La suavidad real requiere esa validación.
