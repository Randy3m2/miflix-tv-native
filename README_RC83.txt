BruniO TV RC83 — Volver a la misma posición

Reemplaza los 15 archivos sobre RC82 conservando las rutas y genera el build habitual de GitHub.
También incluye los cambios de RC82 en Screens y HomeShelfFrame.
Versión 2.0.0-rc83 / versionCode 22082.
Sin SQL ni despliegue de Vercel.

Comportamiento:
- Al regresar de detalles, Home restaura la tarjeta enfocada y conserva el desplazamiento vertical y horizontal existente.
- Collections conserva el enfoque de géneros, plataformas, franquicias, años y próximos estrenos.
- Los catálogos de películas/años/géneros y las filas dentro de cada plataforma restauran el título enfocado.
- Reutiliza el estado de listas guardado por navegación y perfil; solo guarda IDs de enfoque, sin llamadas al servidor ni logs adicionales.
- Mientras se restaura Home, el enfoque inicial del menú y otras tarjetas no fuerzan un salto al inicio. Si el catálogo cambió y desapareció el elemento, la protección vence después de 1.5 segundos.
- Conserva la corrección horizontal de Streaming/Genres de RC82 y el diseño de RC81.

Verificación:
Sin nuevos errores de sintaxis Kotlin frente a RC82. Cinco verificaciones automatizadas de regresión aprobadas. APK no compilado aquí: sin SDK Android/Gradle wrapper; prueba en TV pendiente.

Prueba en TV:
1. Desde una fila baja de Home, desplázate varias películas a la derecha, entra a una y pulsa Atrás. Debe volver a esa misma tarjeta y fila.
2. Repite desde Continue Watching, Coming Soon, géneros y plataformas de Collections.
3. Dentro de Netflix u otra plataforma, repite desde una fila baja. Repite en cuadrículas de género/año.
4. Comprueba que navegar al menú superior voluntariamente todavía permite volver al inicio de Home y que Streaming/Genres avanzan hasta el último elemento.
