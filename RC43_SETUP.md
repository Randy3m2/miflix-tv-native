# RC43: controles y chat dedicado en el reproductor

Hotfix sobre RC42. Subir archivos respetando rutas, commit y ejecutar el workflow Android. RC43 / versionCode 22041 para Check for Updates. No necesita SQL ni cambios en funciones/Gmail.

Se quitan los iconos Speed y Zoom/Fit de la barra del reproductor. Se conservan los controles de reproducción, siguiente episodio, episodios, cola, audio/subtítulos, links, Info, PiP, multiview y administración de Party.

Dos accesos directos nuevos: Chat del Party abre mensajes y permite escribir/enviar desde la TV. Reacciones / Chat QR abre emojis, frases y el QR para entrar al chat desde el móvil. El botón Party conserva crear/gestionar la sala y amigos. Si todavía no estás en una sala, los nuevos botones abren la administración para crear/unirse. También se separan Chat y Reacciones / Chat QR dentro del menú social existente.

Se reutilizan los eventos y el polling ya existentes; no se añade un bucle de red adicional. El chat muestra los mensajes chat disponibles en la ventana de eventos de la sala, hasta el límite existente de 60 eventos. Los nombres desconocidos aparecen como Participante. Los cambios de posición/pausa y velocidad existente se conservan; solo se eliminan los iconos pedidos.

Verificación: retirados ambos botones, callbacks de Chat/Reacciones conectados, nombres de clases/copies app/main revisados. Compilación y prueba de TV pendientes en GitHub. Probar controles con mando, crear sala, enviar mensaje entre TV y chat móvil, abrir emojis/QR y cerrar cada panel sin alterar la reproducción.
