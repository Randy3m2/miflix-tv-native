BruniO Android TV - RP23 / 2.0.0-rp23 / 22127
Aplicar SOBRE RP22 conservando rutas y reemplazando archivos.

Friends vuelve a mostrar cuatro pestanas, en este orden:
Friends / Requests / Moments / Calendar (traducidas segun idioma).

Moments muestra dentro de la pantalla las tarjetas de momentos compartidos:
caratula cuando ya esta en el catalogo local, avatar/remitente, titulo,
episodio, tiempos y duracion; acciones Ver momento y Eliminar.
Se reutiliza el diseno de la bandeja anterior y se conserva su version modal.

Actualizar momentos esta visible en la cabecera y dentro de Moments.
Solo una pulsacion explicita consulta los momentos; abrir la pestana no
consulta ni agrega temporizadores. Mientras carga se evitan solicitudes
simultaneas. El Refresh de otras pestanas conserva su comportamiento.
La identidad del cache de momentos se comprueba por perfil social.

Validacion: sintaxis Kotlin, conexion de la pestana, refresco manual y rutas
espejo revisados. Falta comprobar foco/diseno real en TV.
No se ejecuto Gradle Android ni se genero APK. Sin SQL ni Vercel.
