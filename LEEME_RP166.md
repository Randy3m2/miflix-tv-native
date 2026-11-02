# BruniO RP166 — Sports add-on

Copia las carpetas app/ y mobile/ sobre el repositorio y crea la build de TV. Compatible con la base RP164 o RP165: este paquete incluye los cambios de RP165 de Home y YouTube LIDOM. Versión TV 2.0.0-rp166, móvil 1.0.8-m9-rp166, código 22270. No se ha publicado en GitHub.

El enlace enviado queda como default de Sports, con CDN Bunny y calidad descendente. Las lecturas de configuración migran los defaults históricos al nuevo. Los enlaces personalizados se conservan: si habías guardado otro manualmente, cambia el manifest en Configurar deportes para usar este. No requiere SQL nuevo.

Verificación: compilación Java de la política de Sports; default exacto, migración de defaults antiguos, repetición sin cambios, conservación de un manifest personalizado y validación HTTPS. Configuración decodificada comprobada. Integridad de los cambios de RP165 conservada. No se recompiló el APK completo ni se probó reproducción real en esta entrega; RP165 tenía compilación de TV/móvil y 580 pruebas aprobadas.
