# BruniO RP165 — Home y YouTube LIDOM

Aplica las carpetas app/ y mobile/ sobre el repositorio actual, conservando los demás archivos. Base revisada: 843055e48df64c2a9a2d56506361c9efea626ca5, con RP164. No requiere SQL nuevo. Este paquete contiene código fuente para compilar, no APK firmado. No se publicó nada en GitHub.

## TV: regresar de Details

Home retiene sus estados de lista en AppState, separados por cuenta, perfil e idioma: desplazamiento vertical y horizontal, tarjeta enfocada y selección de descubrimiento. La barra superior no reclama el foco al regresar ni reinicia el desplazamiento al recibir foco. Se evita vaciar la fila de descubrimiento y las recomendaciones durante la carga de la misma cuenta. El Home anterior se recupera mientras sigue abierta la misma sesión de la app; si el catálogo elimina una tarjeta, usa el mando para seleccionar otra. No promete conservar una pantalla exacta después de que Android mate el proceso.

Prueba: baja varias filas, desplázate a una tarjeta que esté lejos a la derecha, entra en Details y vuelve con Back. Repite con otra serie, luego cambia de perfil para comprobar el aislamiento. Comprueba también navegación desde la barra superior y regreso después de ver un episodio.

## LIDOM: YouTube dentro de BruniO

En Live → LIDOM → equipo → fuente YouTube, elige el video del partido de la lista o pega su URL con el flujo existente. Se reproduce mediante el reproductor oficial embebido dentro de la app. Abrir el canal fuera de BruniO queda como acción opcional al final del selector.

El reproductor inicia la reproducción al estar listo, admite Play/Pause del mando, pantalla completa y reintento. Si bloquea autoplay, pulsa Pausa / Reproducir. Los errores distinguen video privado/eliminado, bloqueo de reproducción embebida y verificación del reproductor. Para error 153 revisa Android System WebView del dispositivo.

Un canal/RSS no garantiza que su video más reciente sea el partido en directo. Selecciona la emisión correcta. Si el dueño prohíbe embeber, exige autenticación o impone otras restricciones, BruniO no las evita: elige otra fuente o abre YouTube manualmente. No extrae streams de YouTube ni usa ExoPlayer para sus videos.

Referencias técnicas oficiales:
https://developers.google.com/youtube/iframe_api_reference
https://developers.google.com/youtube/terms/required-minimum-functionality#embedded-player-api-client-identity

## Alcance de verificación

La compilación y las pruebas locales no equivalen a una prueba con mando/TV o a una transmisión real de LIDOM. Deben verificarse en tu dispositivo; el paquete no afirma haber probado tu cuenta o tus transmisiones.
