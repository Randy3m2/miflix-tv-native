# RC44: Chat QR dentro de Chat

Sube todos los archivos app/ y el workflow incluidos respetando rutas, commit y ejecuta Build MiFlix Native TV APK. RC44 / versionCode 22042 para Check for Updates. Este paquete incluye la reparación compatible de RC41–RC43 para evitar mezclar Screens y AppState. No requiere SQL ni redeploy de funciones.

Dentro del reproductor: Chat abre la conversación; arriba tiene Chat QR. Escanearlo abre el chat móvil de la sala actual. Cerrar el QR regresa al chat. Reacciones conserva solo emojis/frases y ya no tiene el acceso QR móvil; la etiqueta de su botón también pasa a Reacciones.

Verificación estructural: acceso QR trasladado, eliminado de PartyActionsDialog, estado del QR por sala, foco de cierre y copias app/main. Compilación y prueba en TV pendientes de GitHub.
