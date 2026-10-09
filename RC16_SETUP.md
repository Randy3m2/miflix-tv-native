# BruniO TV Native RC16

## Instalación

Sube el proyecto completo y ejecuta **Build MiFlix Native TV APK**. Instala encima de la versión anterior con el mismo paquete y firma. No hace falta una nueva migración SQL ni volver a desplegar Trakt/Pages para estos cambios. El enlace permanente de Downloader sigue siendo el mismo.

## Cambios

- Se reemplazó el borde global que perseguía las coordenadas de las tarjetas por un borde blanco propio de cada carátula. Entrada y escala de 80 ms, sin animación global en Settings, Friends, perfiles ni el reproductor. La transición del menú superior existente se conserva. Se quitaron sombras animadas de las carátulas.
- OK/centro pausa o reanuda directamente cuando la barra está oculta. Las flechas muestran la barra. Si está visible, OK activa el botón enfocado. Si aparece la información de pausa, la primera pulsación la cierra y devuelve el foco a Play/Pause.
- Los controles no se ocultan por el temporizador mientras está en pausa. Se corrigió la sinopsis que volvía a ocultar los controles después de haber sido cerrada. El guest puede enviar pausa y reanudar consecutivamente: se envían ambos pedidos en orden y una respuesta fallida antigua no elimina el último pedido.
- Se quitó el volumen de la barra y sus opciones. Los botones de volumen del control remoto pasan al sistema.
- Historial de hasta 10 búsquedas, de más reciente a más antigua, sin duplicados por mayúsculas. Se guarda al cerrar/confirmar la escritura y al seleccionar una búsqueda anterior. Se conserva en la TV por cuenta y perfil, incluso tras reiniciar, y no se borra al limpiar la caché de imágenes.
- Down desde cualquier opción del menú superior entra explícitamente al contenido actual: no requiere seleccionar la pestaña donde estás. Home entra al botón del banner; las otras pantallas entran al grupo de contenido visible.
- La foto enviada aparece como **Especial 3 / Special 3**. Se conserva la imagen original y el selector usa el mismo recorte de los demás avatares.

## Verificación en TV

1. Navega rápido entre carátulas y comprueba el borde blanco y la respuesta del desplazamiento. Settings debe tener solo el highlight directo de sus botones.
2. Desde cada botón superior pulsa Down en Home, Search, Collections, Friends/Party, Live TV, My List y Settings, sin activar esa pestaña antes.
3. Con los controles ocultos, pulsa OK reproduciendo. Debe pausar; comprueba también las flechas y la reanudación desde Play.
4. Como guest, pausa y toca una flecha antes de cinco segundos. Espera más de cinco segundos y vuelve a reanudar. Repite esperando la sinopsis y cerrándola con OK. Prueba también pausa/reanudar rápidamente.
5. Confirma 11 búsquedas: deben quedar las últimas 10. Repite una con mayúsculas y reinicia para comprobar persistencia.
6. Cambia al avatar Especial 3 y comprueba el perfil y la cabecera.

Las comprobaciones locales de sintaxis, recursos y contenido del ZIP no sustituyen la compilación. GitHub Actions ejecuta los tests Kotlin y compila el APK. El rendimiento real, foco y sincronización deben verificarse en las TVs.

## Privacidad de RC15

La eliminación de Torrentio/Comet embebido y el filtro TB Download siguen incluidos. El SQL de limpieza de RC15 sigue siendo necesario si otras cuentas ya tenían tu manifiesto compartido; no es una nueva migración de RC16. Para revocar credenciales distribuidas con APKs antiguos, regenera la credencial de debrid y configura tu nuevo manifiesto personal.
