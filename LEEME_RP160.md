# BruniO RP160 · Teclado, dictado y regreso a Home

Paquete acumulativo de fuentes RP153–RP160. Copia `app` sobre el repositorio y ejecuta tu workflow habitual. Versión 2.0.0-rp160, versionCode 22264 / Build 160. No es APK.

## Eliminar

En la tecla de borrar del teclado en pantalla: una pulsación corta borra una letra; mantener OK durante tres segundos borra todo el texto actual. El borrado completo se ejecuta al cumplir los tres segundos y no borra otra letra al soltar. Cancelar, perder el foco o cerrar la pantalla cancela la espera. También admite mantener pulsado con el puntero. No elimina historial, favoritos ni otros datos del perfil.

## Dictado automático

Se corrigió una carrera entre el teclado compartido y la copia de texto del asistente: un dictado nuevo podía perder su marca de solicitud completada antes de que ambos textos estuvieran sincronizados. Ahora se conserva hasta que la consulta se sincroniza, y se ejecuta automáticamente sin tener que pulsar Buscar o activar Assistant de nuevo. Los envíos duplicados del teclado rápido se ignoran.

La frase del ejemplo «can you find for me Jason Statham movie» se interpreta como búsqueda de filmografía, no como título literal de una película que contenga toda la frase. También admite formulaciones como «show me Tom Holland movies» y «Tom Holland movies». Cuando el catálogo devuelve homónimos, usa el departamento de actuación o dirección según la petición. Esto evita tratar al actor y a un director del mismo nombre como una ambigüedad sin salida.

## Consulta del reproductor separada

La entrada del botón AI ya no escribe en la consulta del buscador normal. Las modificaciones dentro del asistente conservan el texto anterior de búsqueda normal; al salir se recupera ese texto. Las solicitudes desde el reproductor quedan dentro del panel del reproductor.

## Home con una pulsación

La navegación del encabezado cierra explícitamente el asistente y su entrada pendiente antes de abrir el destino, incluso cuando el destino ya era la pantalla interna activa. Esto evita tener que abrir otro tab antes de pulsar Home.

## Verificar en TV

1. Escribe varias letras: pulsa Borrar brevemente y después mantén OK tres segundos sobre esa tecla.
2. Pulsa AI y dicta una búsqueda por actor: debe ejecutar la consulta automáticamente.
3. Desde el reproductor pide otra búsqueda; cierra el panel y abre el buscador normal: debe conservar su consulta previa.
4. Desde una búsqueda dictada pulsa Home una vez: debe verse el Home, sin un panel de AI encima.

No se hicieron pruebas físicas con el control remoto, micrófono o Firestick ni llamadas usando una clave personal de Gemini.
