# BruniO TV RC64 — aplicar sobre RC63 / RC63.1

Reemplaza los archivos de este ZIP conservando las rutas. Sube a GitHub y ejecuta el build habitual. Incluye el hotfix de compilación de SupabaseRepository. No requiere una migración SQL adicional ni redespliegue de Vercel. La migración RC63 del party debe estar instalada previamente.

Cambios:
- Watch with the party abre nuevamente el reproductor incluso si el mismo título ya estaba cargado; vuelve desde mini player y recupera la posición del party.
- Estado de reproducción cada 500 ms más el tiempo de red. Compensa la antigüedad del snapshot; corrige diferencias pequeñas a 0.98x/1.02x y salta si superan 900 ms. Alinea una vez el fotograma al pausar. Actualizar todas las TVs del party. Los relojes de Android deben tener fecha y hora automáticas. No garantiza sincronía exacta entre dispositivos o durante buffering.
- Pausa/reanudación del host publica inmediatamente el estado. Los avisos se filtran para coincidir con la reproducción local. Eventos cada 500 ms durante reproducción, miembros cada ocho ciclos. El tiempo real depende de la red; no son intervalos garantizados. Aumentan solicitudes/logs durante party. Se mantienen solicitudes de acceso manuales y límites de presencia/perfil.
- Tarjetas más redondeadas, sin marco gris permanente; menú y hero con transición de color y degradado superior.
- Repeticiones del mando espaciadas para evitar ráfagas; pulsaciones nuevas y cambio de dirección inmediatos.
- Canales: conserva URL HTTP/HTTPS original y cabeceras del proveedor, agente de cliente compatible con IPTV, fallback HLS una sola vez si falla la detección de formato. También en multiview. Muestra código HTTP cuando está disponible. La causa de todos los fallos de canales no está confirmada: requiere probar en TV. Fuentes vencidas, restringidas o retiradas no se reparan con estos cambios.

Validación: pruebas existentes de acceso/SQL del party, comprobaciones de avisos y regresiones de código. Serialización de progreso verificada por separado del modelo de party. Archivos espejo idénticos. Sin compilación Android ni prueba física en TV en este entorno.

Prueba en TV: salir del player y volver con Watch with the party; pausar/reanudar desde host y guest; comprobar desfase con dos TVs; mantener el mando en las cuatro direcciones; abrir canales de cada proveedor y multiview. Si un canal falla, conservar el código de error mostrado.
