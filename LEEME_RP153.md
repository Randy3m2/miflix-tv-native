# BruniO TV · RP153 / Build 153

Versión TV 2.0.0-rp153 / versionCode 22257. Parche acumulativo con RP150–RP152.

## Aplicar

Copia `app` sobre `app` en tu proyecto completo RP149, RP150, RP151 o RP152, respetando las rutas. Conserva copia de cambios propios. Sube los archivos y ejecuta el workflow **Build MiFlix Native TV APK** (`build-native-tv-apk.yml`). Este ZIP contiene código; no incluye APK firmado ni publica cambios en GitHub. No requiere SQL ni dependencias nuevas.

## Guardado de Gemini

El validador anterior aceptaba solamente letras, números, guion y guion bajo, y el formulario recortaba el campo a 160 caracteres. Esta entrega admite puntos y otros caracteres de credenciales, como el formato AQ., y un máximo de 2048 caracteres. No intenta adivinar si la clave funciona: Google la valida cuando envías una búsqueda.

Se mantiene AES/GCM con Android Keystore. Antes de confirmar, BruniO comprueba que puede descifrar lo cifrado y que la escritura persistente termina correctamente. Los errores de formato, cifrado, escritura o QR no disponible se distinguen con mensajes fijos, sin mostrar la clave ni el texto de las excepciones. No hay fallback a guardar claves sin cifrar.

Tras instalar: abre Settings → BruniO AI → Configurar Gemini, escanea el QR nuevo, pega la clave completa otra vez y guarda. No reutilices la pestaña del QR anterior: si el navegador recortó tu clave, debes copiarla de Google de nuevo. Espera la confirmación de guardado; cierra el panel y activa Gemini · ON.

## Interfaz

- AI aparece dentro de la estructura normal de BruniO, debajo de la barra superior. Ya no usa una ventana separada para su pantalla principal.
- Fondo negro, conservando las tarjetas con imágenes y carátulas.
- El campo recibe foco o hover sin abrir el teclado. Solo al pulsar OK o hacer clic entra en edición. Search/Done o Atrás cierran la edición.
- El micrófono y QR continúan disponibles junto al campo.
- Los botones Eliminar clave y Cerrar están agrupados con separación horizontal/vertical y pueden pasar de fila si falta espacio.
- También se ajustaron grupos sin separación en búsqueda de calendario, mensajes/reintento de sagas, errores de LIDOM y encabezado de Trakt.
- La navegación guarda por separado el estado de AI y de la pantalla previa. Las secciones de la barra superior siguen funcionando.

## Verificación en TV

1. Abre AI desde Home: comprueba que la barra superior permanezca visible y el fondo sea negro.
2. Mueve el foco al buscador: el teclado debe permanecer cerrado. Pulsa OK: debe abrirse. Atrás cancela la edición.
3. Revisa Eliminar clave/Cerrar en Gemini y su separación al enfocar.
4. Guarda la clave usando un QR nuevo y comprueba la confirmación o el error específico.
5. Realiza una búsqueda con Gemini activado para verificar tu clave y cuota de Google.

Las pruebas usan credenciales ficticias y servidores de prueba en localhost; no se ha recibido ni probado tu clave real. No se ha probado físicamente en TV el teclado, el diseño ni Android Keystore de tu dispositivo. El resultado de compilación y pruebas está en VALIDACION_RP153.txt.
