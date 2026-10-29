# BruniO TV RP146 · Teclado Bluetooth opcional

Aplicar los 3 archivos después de RP145, respetando rutas. Generar el APK con build-native-tv-apk.yml. TV 2.0.0-rp146 / 22250; versión móvil sin cambios.

En el buscador, debajo de Voz / QR, aparece Teclado Bluetooth. Abre los ajustes Bluetooth de la TV; si el fabricante no expone esa pantalla, abre ajustes generales. Si tampoco están disponibles, muestra instrucciones con el aviso existente de 8 segundos y Dismiss.

Empareja un teclado físico Bluetooth, regresa al buscador con Back y escribe. El buscador ya admite letras y retroceso de teclados físicos sin un segundo campo de texto. Conserva la búsqueda escrita al salir hacia ajustes. El acceso está disponible también en Easy Mode.

Es una alternativa opcional: no activa Bluetooth automáticamente ni requiere nuevos permisos. No convierte el celular en un teclado Bluetooth, no captura el micrófono Alexa y no necesita QR ni comunicación LAN para escribir.

No incluye APK firmado. Falta verificar emparejamiento y navegación en el Firestick real.

## Dictado desde el celular

La página QR del buscador muestra Dictar con mi teclado. Enfoca el campo de búsqueda y abre el teclado del celular; toca el micrófono de ese teclado para dictar (por ejemplo Gboard o Samsung). El texto usa el mismo envío local del formulario y permite borrar/corregir. No hay grabación de audio dentro de BruniO, ni carga de audio a su servidor. El servicio de dictado y sus permisos pertenecen al teclado del celular.

El botón no puede activar automáticamente el micrófono nativo: requiere tocar el micrófono del teclado. No usa Web Speech ni getUserMedia sobre HTTP local. Si falta el icono, habilita dictado/permiso de micrófono en el teclado del teléfono. No evita bloqueos de red: el formulario QR debe poder abrir primero.

Referencia oficial de Gboard: https://support.google.com/gboard/answer/2781851?hl=es
