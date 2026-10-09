# BruniO RC19 · 2.0.0-rc19 / 22017

Sube el proyecto y ejecuta Build MiFlix Native TV APK con los mismos secrets de firma. Si ya configuraste RC18, no repitas SQL ni Pages por este cambio. La carga de avatar sigue necesitando el SQL y Pages de RC18 si aún no los configuraste.

Búsqueda conserva el campo arriba y mueve el historial dentro del mismo scroll que los resultados. Así se puede desplazar el historial fuera del viewport y usar el área disponible para las carátulas; la última fila conserva padding inferior. El historial mantiene sus diez entradas y su navegación horizontal.

Las carátulas verticales de Home pasan de 146 × 220 dp a 130 × 198 dp; las demás de 170 × 252 dp a 154 × 230 dp. También se reducen las horizontales y la altura reservada a sus filas. Se conserva espacio para el highlight blanco y márgenes. La búsqueda y los catálogos usan columnas adaptativas de 168 dp.

Incluye todos los avatares y la corrección de compilación de RC18. Se revisaron sintaxis y empaquetado; la compilación y la comprobación de navegación en TV quedan para Actions y el dispositivo real.
