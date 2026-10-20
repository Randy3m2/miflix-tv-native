BruniO RP46 — Live: proporciones, regreso y navegación TV
Aplicar sobre RP45. Versión: 2.0.0-rp46 (22150).

- Los nueve cuadros conservan una proporción uniforme de imagen, con cuadrícula centrada, menor ancho y margen lateral. Se evita estirar los logos al ancho de la pantalla.
- Atrás desde el reproductor vuelve a la sección de origen (MLB, NBA, F1, TV, etc.). Se conserva la categoría durante esta sesión, separada por cuenta/perfil. Atrás dentro de una sección vuelve a los nueve cuadros.
- Live TV abre categorías y cuadrícula de canales, sin reproducir automáticamente. Primera selección: Más vistos cuando está disponible. Al regresar recuerda la categoría escogida.
- Nauta e IPTV permanecen en una misma pantalla de navegación; IPTV se carga al elegir su categoría. Búsqueda, favoritos con pulsación larga y carga adicional se conservan.
- Categorías de TV usan los chips oscuros de Others. Se conserva la barra normal de BruniO.
- Se conservan los selectores manuales de Live Sports y los canales personalizados por QR de RP45.

Validación: 3 tests JVM del código real de memoria de navegación; sintaxis de las fuentes Kotlin; integridad de rutas duplicadas. No se ejecutó Gradle ni se generó APK. Falta validar dimensiones/foco y reproducción en TV real.
No requiere SQL ni Vercel. Copiar conservando rutas.
