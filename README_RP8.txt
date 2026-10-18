BruniO TV RP8 — Excepción de tamaño desconocido
Aplicar sobre RP7, conservando rutas.
Addon Latam y Nuve+ no se ocultan por el límite de GB cuando no informan tamaño.
Si indican tamaño y superan el límite, siguen ocultándose.
Identificación por host exacto del manifest, no por nombre visible ni URL del vídeo:
addonlatam.duckdns.org y plus.nuvehub.app.
No modifica los demás proveedores, filtros de año/descargas, idioma ni límites
de cantidad por add-on de RP7. La reproducción rápida usa la misma regla.
Se conserva solo el host en StreamChoice, sin tokens ni rutas de configuración.
El modelo parte del último Models.kt de RC93, preservando sourceUrl.

Validación: 28 pruebas JUnit correctas (4 excepción de tamaño, 21 ordenación y
3 límites de cantidad). StreamRepository compilado en comprobación JVM aislada.
Parser Kotlin sin errores nuevos frente a la base.
No se ejecutó build Android ni se generó APK. Pendiente validar en TV.
No requiere SQL ni Vercel.
