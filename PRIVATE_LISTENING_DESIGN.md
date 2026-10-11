# Audio privado TV → teléfono (diseño pendiente de implementación)

Objetivo: mantener el video en la TV y transmitir la pista de audio elegida al teléfono, con audífonos conectados a este. No implementado ni habilitado en RC22.

Ruta propuesta: usar AudioSink/AudioProcessor de Media3 para capturar PCM de la pista decodificada (sin abrir una segunda descarga del proveedor); codificar Opus y enviar una pista WebRTC solo de audio. Emparejamiento por QR con secreto de sesión efímero, aprobación local, sin guardar URLs/credenciales de proveedores en una página pública. Señalización autenticada y sesión revocable. Modo local o servidor de relay según red; definir consumo/costo antes de desplegar.

La TV necesita ajustar retardo del video frente al audio usando reloj compartido y reportes del receptor. El teléfono requiere gesto de usuario para iniciar reproducción y controles para ajustar desfase/reconectar. Al detener sesión, perder receptor o salir del contenido, debe volver el audio de TV automáticamente. Respetar cambios de pista, pausa, seek, velocidad y Party. Probar películas MKV/MP4 con AAC/AC3/DTS y video HEVC/H264; Android/iOS y audífonos Bluetooth, que añaden latencia propia.

Un teléfono con soporte de dos salidas Bluetooth puede enviar el audio recibido a ambos audífonos; de otro modo la alternativa es un receptor por persona. Esto requiere pruebas con el modelo real de TV, versión Android y teléfonos antes de prometer compatibilidad.

Referencias oficiales:
- https://developer.android.com/reference/androidx/media3/exoplayer/audio/AudioSink
- https://developer.mozilla.org/en-US/docs/Web/Media/Guides/Formats/WebRTC_codecs
- https://webrtc.org/
