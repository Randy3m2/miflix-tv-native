# BruniO Mobile M3 / RP139

Versión móvil: 1.0.2-m3-rp139 / versionCode 22243.
Aplicación estable: com.brunio.mobile. Android 8.0 o posterior (minSdk 26, como M2).

## Aplicar y generar el APK estable

Este ZIP es un parche de código, no un APK. Se aplica al repositorio miflix-tv-native actualizado a TV RP138, que ya contiene los recursos y el motor compartido RP135–RP138.

1. Copia los diez archivos de código/configuración conservando sus rutas.
2. En GitHub Actions, ejecuta **Build BruniO Mobile APK** (build-brunio-mobile-apk.yml).
3. El workflow usa las mismas claves de firma y el token de catálogo ya configurados. No requiere nuevos secretos ni SQL.
4. Instala el APK estable resultante sobre la versión móvil estable anterior, sin desinstalar ni borrar datos. El identificador de la aplicación estable y el sistema de firma no cambian.

No ejecutes el workflow de TV para esta actualización. La versión y las notas actuales de TV se mantienen en RP138. El APK Debug/Preview usado en validación tiene un identificador separado y no sustituye al APK estable firmado.

## Cambios trasladados y adaptados

- Motor compartido RP137: recuperación automática de enlaces deportivos en el reproductor principal, siguientes enlaces y consulta de enlaces nuevos cuando se agotan. Se mantienen las grabaciones y VOD fuera de esta recuperación deportiva.
- Aviso de enlaces agotados con acciones táctiles para elegir otro juego/canal o cambiar la fuente. El menú de Más opciones suspende los cambios automáticos mientras está abierto; una respuesta atrasada no debe interrumpir una elección manual.
- El selector de contenido del reproductor abre juegos al ver deportes y canales al ver Live TV.
- Live > F1 ahora se identifica como F1 · MotoGP. Usa la clasificación y los logos RP138, conservando estadísticas de F1 como estadísticas de F1.
- Others: grabaciones por deporte, con filtros y tarjetas dentro de una sola cuadrícula desplazable. Los controles no ocupan un bloque fijo que recorte el listado.
- Calendario: todas las suscripciones de TV, incluidos MotoGP, Boxing, Grammys, Oscars y Miss Universe, con logos locales. Los días pueden mostrar dos logos distintos y un contador cuando hay más.
- Los eventos especiales permiten configurar la misma fuente HTTPS JSON opcional del calendario de TV. MotoGP consume su fuente pública propia y no necesita esa URL.
- Perfiles: selector por grupos, incluidos los seis retratos de Friends, seis de VisionQuest y seis de Loki añadidos en RP138, además de los grupos anteriores. El formulario completo se desplaza para mantener accesible Guardar.
- Audio secundario: Desactivado se muestra claramente y no se ofrece como secundario el mismo idioma principal.
- Notas de actualización específicas para Mobile M3 / RP139, conservando el historial de M2.
- Compatibilidad de bibliotecas Java alineada con TV mediante core library desugaring.

La escritura del buscador móvil sigue usando el teclado nativo. El QR RP136 se utiliza desde la TV: al escanearlo con el celular, se escribe en la página local que controla la búsqueda de la TV. No se añade un QR redundante para escribir en el propio teléfono.

## Conservación y prueba en dispositivo

Se mantienen los ajustes, add-ons, perfiles, descargas, calendario y funciones de M2. El parche no cambia esquemas de servidor, fuentes M3U ni claves de perfiles/avatares anteriores. Los recursos de TV RP138 se reutilizan desde los sourceSets compartidos.

Falta validación física en un teléfono: retratos y Guardar con teclado abierto, selección de idioma secundario, MotoGP y varios logos por día, filtros de grabaciones, aviso de enlace agotado y cambio manual de juego/fuente. Las pruebas automáticas y el empaquetado no sustituyen esta revisión táctil.
