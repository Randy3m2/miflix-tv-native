# BruniO TV RC21 · 2.0.0-rc21 / 22019

El selector de episodios ahora se abre en un diálogo modal por encima del video y los controles. El control remoto entra al episodio actual (o al primero de la temporada); si la lista está vacía, al botón Cerrar. Al cambiar de temporada se desplaza la lista y se devuelve el foco a los episodios. Cerrar/Back regresa a los controles del reproductor.

Conserva los cambios RC20: Siguiente, estilo de subtítulos y renombrar perfiles. No requiere SQL nuevo. Sube el contenido conservando desktop y el workflow PC, y ejecuta Build MiFlix Native TV APK desde main.

Prueba en TV: abre Episodios reproduciendo y en pausa; navega con las flechas, cambia de temporada, selecciona un episodio y prueba Back/Cerrar. Comprueba también la pulsación larga para elegir enlaces. Compilación en GitHub Actions y navegación real en TV pendientes; sintaxis Kotlin e integridad del ZIP revisadas localmente.
