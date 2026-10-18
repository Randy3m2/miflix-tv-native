BruniO TV RC95 — navegación Home

Aplicar sobre RC94, conservando las rutas y reemplazando los archivos incluidos. Subir el parche a GitHub y generar el APK mediante el flujo habitual.

Cambios:
- La entrada hacia una fila confirma el foco antes de alinear su primera tarjeta. Evita cancelar la búsqueda de foco de filas fuera de pantalla.
- Cancela movimientos pendientes de la cabecera al cambiar de dirección.
- Conserva la restauración de posición al regresar de detalles.

Sin SQL, secretos ni cambios en Vercel. No añade consultas a Supabase.

Verificación: comprobaciones de navegación, diseño, recuperación de fuentes, paneles y espejos aprobadas; sintaxis de SmoothRowFocus aprobada. No se ha compilado el APK: entorno sin Android SDK/Gradle. Pendiente validar en TV con pulsaciones y dirección mantenida arriba/abajo, filas desplazadas a la derecha y regreso desde detalles.
