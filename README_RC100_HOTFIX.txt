BruniO RC100 — Hotfix de compilación

Aplicar sobre RC100 conservando las rutas. Sustituye AppState.kt por la copia correspondiente a tu proyecto.
Corrige restoreCustomFilterRC100: minimumRating se restaura como Int con optInt, usando el valor por defecto del modelo. Antes se usaba optDouble, incompatible con el tipo del modelo.

3 archivos; único cambio de código: lectura de minimumRating. No cambia versión ni incorpora Canal anterior.
Sin SQL, Vercel ni build. Diferencia de una línea y copias idénticas verificadas; compilación Android pendiente.
