# RC43: reparación de archivos mezclados

El error Unresolved reference refreshPlaybackLinks significa que Screens.kt es más nuevo que AppState.kt: Screens usa el método añadido en RC42, pero ese AppState no lo incluye.

Este parche reúne los archivos de RC41, RC42 y RC43 con sus versiones actuales compatibles. Subir TODOS los archivos app/ y el workflow incluidos, conservando rutas. No reemplazar solo Screens.kt. Las copias main/ también están sincronizadas. Commit y ejecutar de nuevo el workflow.

Conserva RC43 / versionCode 22041: la compilación fallida no publicó una versión nueva. No hay SQL ni cambios de Supabase. Incluye controles Party/Chat, caché de links y reducción de peticiones.

Verificación: refreshPlaybackLinks es público dentro de AppState, llamado desde Screens; PlaybackLinkCache.java está incluido; SessionRenewal/SocialRepository/MiFlixNativeApp se incluyen para sus dependencias de RC41. Copias app/main coinciden. Compilación Android pendiente de GitHub.
