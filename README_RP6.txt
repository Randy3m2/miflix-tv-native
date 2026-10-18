BruniO TV RP6 — Prioridad de audio latino
Aplicar sobre RP5 conservando rutas.
Cuando Playback audio es español:
Enlaces: Latino > España/es-ES > etiqueta Castellano > español sin región > inglés.
España y Castellano suelen ser la misma variante; el orden distingue las etiquetas.
Audio declarado tiene prioridad sobre títulos del archivo; subtítulos no cuentan
como audio. Prioridad regional antes de proveedor/calidad. Mantiene todas las
fuentes españolas antes de alternativas inglesas.
Pistas del reproductor: prefiere códigos regionales latinos, después es-ES,
español genérico e inglés. Las pistas sin región no permiten saber su variante.
Selecciones de pista guardadas/manuales se respetan.

Validación: JUnit 4, 21 pruebas StreamOrderingTest correctas contra código RP6.
Parser Kotlin: sin nuevos errores en Screens frente a RP5.
No se ejecutó suite Android completa ni se generó APK.
Sin SQL ni Vercel. Reproducción y pistas reales pendientes de validar en TV.
