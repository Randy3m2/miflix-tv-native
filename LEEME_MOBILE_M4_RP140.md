# BruniO Mobile M4 / RP140

Versión móvil 1.0.3-m4-rp140, versionCode 22244. Parche de código sobre Mobile M3 / RP139; no es un APK firmado.

## Aplicar

1. Copia los archivos conservando sus rutas al repositorio ya actualizado con RP139.
2. Publica también web/watch.html y web/.well-known/assetlinks.json en la web actual de BruniO (https://miflix-tv-native-web.vercel.app). La ruta /.well-known/assetlinks.json debe responder HTTP 200, JSON, sin redirecciones ni login.
3. Ejecuta GitHub Actions: **Build BruniO Mobile APK**. Conserva los mismos secretos y certificado de firma. No necesita SQL nuevo.
4. Instala el APK estable sobre la app móvil anterior, sin desinstalar. Identificador estable: com.brunio.mobile. El workflow de TV no es necesario.

## Cambios

- Historial de búsqueda local por usuario/perfil, reutilizando el almacenamiento existente. Guarda consultas de al menos dos caracteres después de 1,2 segundos sin escribir; al vaciar la búsqueda aparecen las búsquedas recientes. El modo fácil conserva su política de no guardar historial.
- Selector horizontal de add-ons en Cambiar fuente, con Mandanguita TV separado. Se pueden ver todos o filtrar por add-on. Los instalados sin enlaces muestran un aviso en lugar de desaparecer.
- Cast visible arriba a la derecha en los controles del reproductor. Abre la función de transmisión de pantalla del sistema Android; no añade un SDK Chromecast de transmisión directa. Si no está disponible, muestra un aviso.
- Eliminado el botón móvil de mute/sonido; el volumen se controla con el dispositivo.
- Compartir desde la ficha y desde Más opciones del reproductor VOD: primero ofrece BruniO o enlace de IMDb. Ambas opciones comparten título, sinopsis y carátula adjunta; IMDb usa la ficha exacta del título y no necesita instalar BruniO. Si no hay ID de IMDb disponible, se avisa sin inventar una URL. La descarga de carátula se hace fuera del hilo de interfaz, con timeout de ocho segundos y límite de 4 MB; si falla, se comparte texto y enlace. No se comparten enlaces privados de reproducción ni tokens de add-ons.
- Botones de la ficha con etiquetas. Tráiler consulta alternativas si la ficha aún no lo tiene y muestra aviso cuando no está disponible. Party abre la sala y su pestaña, en lugar de crearla sin una transición visible.
- Notas de actualización propias de Mobile M4; TV continúa en RP138.

## Enlaces compartidos

Ejemplo: https://miflix-tv-native-web.vercel.app/watch.html?type=movie&id=42

Con la web publicada y la app estable instalada con el certificado habitual, Android puede verificar el dominio y abrir la ficha dentro de BruniO. Android permite cambiar esta asociación en “Abrir de forma predeterminada”. El enlace no inicia la reproducción automáticamente ni elude los controles Kids.

Si se abre en navegador, la página ofrece “Abrir en BruniO móvil” (brunio://content/...) o “Continuar en la web”, usando las rutas de contenido existentes. El enlace espera la selección de perfil si la app acaba de arrancar. La firma de Preview/Debug no está asociada al dominio de producción.

assetlinks.json contiene el certificado público de signing_certificate_sha256.txt y el paquete móvil estable; si cambias la firma de producción, debe actualizarse. No contiene claves privadas.

La aplicación receptora decide cómo presentar imagen, texto y enlace; no se puede imponer el formato visual de WhatsApp u otra app. La portada se adjunta como archivo, no depende de que la app receptora genere una vista previa web.

## Pendiente de dispositivo

Verificar táctilmente Tráiler, crear/entrar a Party, cambiar add-on durante reproducción, compartir con y sin carátula, recepción del enlace con la app cerrada/abierta y con selección de perfil, y Cast en un dispositivo que lo soporte. No se ha publicado ningún APK ni desplegado la web desde esta sesión.
