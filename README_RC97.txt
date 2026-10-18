RC97 — revertir selección por compatibilidad de audio
Aplicar sobre RC96, conservar rutas y generar APK por GitHub.
Elimina la inspección de decodificadores y la clasificación de fuentes por códec de audio. Revierte también los nuevos rechazos por pistas no compatibles/español compatible y los reintentos específicos por errores 5001/5002 añadidos junto a esa función.
Conserva prioridad de idioma, lógica previa de inglés/español, selección opcional según conexión, reintentos previos y todas las mejoras de RC95/RC96.
No requiere SQL ni Vercel. La clase AudioSourcePolicy puede permanecer como código inactivo, sin referencias desde el selector.
Pruebas de recuperación de fuentes, trailers e integración aprobadas; sintaxis de StreamOrdering aprobada. APK no compilado en este entorno; validar en TV.
