# BruniO TV Native 2.0 RC13

Proyecto completo acumulativo. Mantiene el diseño compacto de RC12 y las funciones anteriores.

## Qué se revisó respecto a RC5

Se comparó el código del reproductor y de apertura con el ZIP RC5. El motor, fuentes de datos, preferencias de pistas, renderers con fallback y Media3 no cambiaron de forma relevante entre RC5 y RC12; por tanto no se atribuye toda la lentitud a un cambio de motor.

## Cambios

- Apertura: los subtítulos externos se buscan en paralelo, pero solo se espera hasta 800 ms adicionales cuando el enlace ya está disponible. Antes se esperaba toda la consulta y una segunda búsqueda si estaba vacía, cada una con múltiples peticiones HTTP.
- Los resultados tardíos siguen accesibles desde CC. Seleccionar uno explícitamente incorpora ese subtítulo y vuelve a preparar su fuente manteniendo la posición; no se reinicia el vídeo automáticamente cuando termina una búsqueda lenta.
- Caché de hasta 16 consultas de subtítulos en memoria de la sesión. No guarda URLs de vídeos en disco ni cambia el ranking de enlaces.
- Superficie de vídeo propia por instancia del player, sincronización de SurfaceView con Compose habilitada y cambios de tamaño aplicados solo cuando cambia el modo. Conserva el SurfaceView nativo de RC5; no se sustituye por TextureView.
- Buffer: 1 segundo para empezar y 1.5 segundos para reanudar tras buffering; buffer mínimo 15 s y máximo 50 s. Un inicio más rápido puede implicar más interrupciones con un enlace muy lento. Cambiar de audio selecciona solo la pista; no vuelve a resolver enlaces ni recrea el player.
- Spinner nativo durante buffering y aviso con código de error, Reintentar / Otro enlace / Salir cuando ExoPlayer reporta un fallo. El aviso tiene foco propio.
- Se conserva la protección de la pausa del guest de RC12.

## Instalar

Subir los archivos reemplazando las rutas existentes y ejecutar Build MiFlix Native TV APK. Instalar como actualización. No hay SQL nuevo ni despliegue de Pages/Trakt. El paquete Android y native-latest/MiFlix-TV-Native.apk siguen iguales: mismo Downloader.

## Comprobar en la TV

1. Probar los mismos contenidos y enlaces que funcionaban en RC5. Comparar tiempos con la misma conexión; distintos proveedores y tamaños no son una comparación equivalente.
2. Película y episodio: arranque, audio e imagen; varias pistas de audio; CC, pausa/reanudación, cambio de enlace, zoom y maximizar/restaurar.
3. Abrir CC después de iniciar: si el servicio está lento muestra búsqueda mientras puedes seguir reproduciendo. Sus pistas aparecen al terminar.
4. Host y guest: pausa/reanudación con fotograma visible y sincronización al reanudar.
5. Si sigue negro: anotar modelo de TV, versión de Android, nombre del enlace, si hay sonido, si avanza el contador y cualquier código de error. Eso distingue fallo de superficie, formato o proveedor.

Validación local: sintaxis Kotlin/XML y ZIP. No hay Gradle ni Android SDK aquí; APK y rendimiento real, cambio de audio y pantalla negra necesitan confirmación en la TV. No se garantiza que todos los formatos de cada proveedor sean compatibles con su decodificador.
