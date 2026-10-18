BruniO TV RC96
Aplicar sobre RC95 conservando las rutas; subir a GitHub y generar APK.
Dazzle Me utiliza el mood seleccionado explícitamente en Pick your vibe; sin selección usa una mezcla aleatoria del catálogo. Historial local por selección. Si el mood no tiene tráileres, muestra estado vacío y reintento; no mezcla otros moods.
La ventana fullscreen permanece abierta mientras se prepara el siguiente tráiler: ya no se alternan dos ventanas sobre Home. El WebView se renueva por video para conservar aislamiento y liberar el anterior.
Controles de YouTube: controls=0, disablekb=1, fs=0 ya configurados. No se ocultan branding ni avisos obligatorios, ni se usan parámetros obsoletos.
Sin SQL ni Vercel; sin nuevas consultas de Supabase. Obtiene metadatos del mood al abrir Dazzle Me.
Pruebas de integración de navegación, filas, audio, moods, ventana persistente y espejos aprobadas; sintaxis de los dos archivos del reproductor aprobada. No compilado en este entorno (sin SDK Android/Gradle). Pendiente validar reproducción y transiciones en TV.
