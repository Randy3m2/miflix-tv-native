# BruniO TV RP137 — Recuperación de enlaces deportivos

Versión TV: 2.0.0-rp137 / versionCode 22241.
Actualización de código sobre RP135 o RP136. Incluye el teclado QR RP136, para que ambos cambios entren en una sola build. Conserva la compatibilidad de Firestick RP135 que ya está en el repositorio. No requiere SQL ni configuración nueva.

Extrae el ZIP en la raíz de miflix-tv-native, conserva las rutas y sube los 12 archivos de código/configuración. Ejecuta el workflow habitual de TV para generar el APK firmado e instala como actualización, sin desinstalar. Este ZIP no es un APK ni un proyecto completo.

## Comportamiento

- Para juegos del add-on de deportes en el reproductor principal, tanto normal como fácil. No modifica VOD, grabaciones deportivas, IPTV ni SplitScreen.
- Conserva los cinco enlaces alternativos al empezar a reproducir; antes se borraban al recibir el primer cuadro, impidiendo recuperar un juego después de horas.
- Ante un error se conserva el reintento único de la fuente actual. Si persiste, continúa con el siguiente enlace de la lista, sin repetir los fallidos dentro de la misma tanda.
- Si permanece cargando sin avanzar durante 20 segundos, también prueba el siguiente. La comprobación continúa después del inicio del juego, incluso horas más tarde.
- Después de agotar la tanda, consulta una vez al add-on usando el mismo ID y tipo del evento y el manifest configurado. Pide no reutilizar caché HTTP y conserva los headers de reproducción actualizados de cada fuente.
- Prueba hasta cinco enlaces de la respuesta nueva. Una URL idéntica puede recibir una segunda oportunidad en esta tanda, porque su autorización podría haberse renovado. Máximo dos tandas de cinco enlaces por caída; cada reproducción mantiene su reintento único existente.
- Tras 30 segundos continuos reproduciendo, restablece el presupuesto para poder recuperarse de otra caída posterior. Un primer cuadro aislado no lo restablece.
- Si se agotan los intentos o no puede obtener fuentes, deja un aviso persistente: “No hay enlaces disponibles para este juego”. No abre un diálogo ni bloquea el control remoto. OK abre juegos; izquierda/derecha muestran controles y permiten cambiar la fuente manualmente.
- La pausa, segundo plano y menús suspenden la recuperación. Antes de aplicar una respuesta comprueba el juego, la reproducción, la cuenta/perfil y el manifest. Una respuesta atrasada no cambia el nuevo juego. Si la señal original se recuperó durante la consulta, la conserva.

No añade consultas periódicas al add-on mientras el juego funciona ni polling nuevo a Supabase. Los avisos y el detector son locales. Se conserva el comportamiento social que ya existía cuando empieza una reproducción.

## Prueba física sugerida

1. Reproduce un juego; tras unos minutos provoca un fallo de la fuente. Debe cambiar a otra sin volver al hub.
2. Comprueba que una pausa o abrir el selector manual no cambie la fuente automáticamente.
3. Prueba un juego sin fuentes válidas: debe dejar el aviso y permitir salir, seleccionar otro juego o abrir los enlaces manuales.
4. Cambia de juego mientras está buscando otra fuente: no debe volver al anterior.

La validación automática usa fuentes simuladas locales. No certifica que el proveedor publique enlaces válidos para un juego real ni garantiza ausencia de interrupción al cambiar de fuente. No se ha probado en una TV física.
