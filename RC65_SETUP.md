# BruniO TV RC65 — instalar sobre RC64

Reemplazar conservando las rutas, subir a GitHub y ejecutar el build habitual. No requiere SQL ni Vercel.

Corrección dirigida al error fotografiado: ERROR_CODE_FAILED_RUNTIME_CHECK.
- Los canales del player principal usan ahora DefaultRenderersFactory de Media3, sin el wrapper de subtítulos ni el AudioSink personalizado de películas/series. Mantiene los headers originales del proveedor y el User-Agent normal, retirando el cambio especulativo de RC64. El player de películas/series y su ajuste de auriculares conservan su implementación.
- El error interno muestra causa y punto de fallo local de Media3; elimina URLs y valores de credenciales etiquetadas. No envía estos detalles a Supabase.
- Esto evita la ruta personalizada sospechosa, pero el código genérico de la captura por sí solo no identifica la excepción exacta. La recuperación de canales requiere probar en la TV. Si persiste, fotografiar todo el nuevo mensaje, que incluye la causa.

Visuales:
- El fondo de Home comienza detrás de la navegación; desaparece el hueco superior de 84 dp. Menú con fondo transparente gradual, y gradiente sobre el mismo artwork.
- Pósteres cerrados en proporción cercana a 2:3 y sin títulos/gradientes oscuros duplicados. Las tarjetas de Continue Watching mantienen formato panorámico.
- La ampliación tras tres segundos dibuja sobre espacios de tamaño fijo; ya no mueve las tarjetas vecinas. La ampliación se ancla hacia la izquierda cerca del borde derecho.

Navegación:
- Repeticiones del mando esperan a que termine el desplazamiento actual de Home/fila. Las pulsaciones nuevas siguen siendo inmediatas.
- El regreso al principio al entrar en la barra se dispara al entrar, no en cada cambio de foco entre botones.
- Mantener abajo desde la barra no genera varias solicitudes de entrada.
- El hero espera 350 ms de foco estable antes de cambiar artwork, evitando decodificar fondos durante una ráfaga del mando.

Validación realizada: sintaxis de componentes modificados y bloques de player/tarjetas; regresiones de party RC63/RC64 y nuevas comprobaciones de RC65; igualdad de archivos espejo y ZIP. No se ha compilado Android ni medido FPS o reproducido canales en una TV en este entorno.

Pruebas necesarias en TV: un canal de Channels, TV Channels y Live Sports; mantener derecha/izquierda y abajo; dejar una tarjeta tres segundos y desplazarse a otra; comparar el difuminado con la captura anterior. No se garantiza un resultado físico sin esa prueba.
