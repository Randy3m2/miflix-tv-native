BruniO TV RP8 — Hotfix acumulativo completo
Aplicar todos los archivos conservando rutas. Sirve sobre RP6, RP7 o RP8.
Reúne RP7 + RP8 + hotfix del control visual; sustituye los ZIPs separados.
Incluye AppState con addonLimitRP7/saveAddonLimitRP7, el helper de límites,
StreamRepository y StreamFilters, el modelo actualizado y Screens con el
control de límites integrado. Puede dejarse el control externo anterior:
su nombre es distinto y no genera declaraciones duplicadas.
Conserva límites por proveedor/perfil y excepción de tamaño Latam/Nuve+.

Comprobado: referencias nuevas y sus implementaciones presentes en las rutas
Android; archivos espejo idénticos. Mantiene las implementaciones verificadas
en RP8 con 28 pruebas JVM. Este empaquetado no ejecuta build Android ni crea APK.
No requiere SQL ni Vercel. Versión sigue siendo RP8.
