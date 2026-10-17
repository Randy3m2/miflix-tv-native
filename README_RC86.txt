BruniO TV RC86 — Enlace actual resaltado (sobre RC85)

Reemplaza los 13 archivos conservando sus rutas y genera el build habitual de GitHub.
Versión 2.0.0-rc86 / versionCode 22085. Sin SQL ni despliegue de Vercel.

Playback links:
- Fuente actual con etiqueta NOW PLAYING / REPRODUCIENDO, borde y fondo del color de enfoque elegido.
- El resaltado permanece cuando mueves el control a otra fuente.
- Al abrir la lista, desplaza y enfoca la fuente actual; si no está en la lista, comienza por la primera sin marcar otra por error.
- Verifica título, temporada y episodio para evitar marcar enlaces de otro contenido.
- Para fuentes nativas de TorBox conserva localmente la URL de la entrada original al resolver el enlace de reproducción; así la comparación sigue siendo exacta tras materializar. No publica esa identidad en Supabase.
- Usa la solicitud de reproducción vigente, incluidos cambios automáticos de RC85. Conserva la etiqueta separada de PARTY SOURCE.
- No añade peticiones al servidor.

Verificación:
Selector modificado sin errores de sintaxis; seis verificaciones de regresión aprobadas y fuentes espejo iguales. APK no compilado aquí, sin SDK Android/Gradle wrapper; pendiente validar en TV que abrir Playback links enfoque y marque el enlace actual, también después de un cambio automático y con fuentes nativas.
