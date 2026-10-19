BruniO TV RP35 — Cursor del buscador
Aplicar sobre RP34. Versión 2.0.0-rp35 / 22139.

El cursor del buscador de películas/series ahora se dibuja como línea de 1dp sobre la posición real calculada por TextLayoutResult.getCursorRect. Ya no inserta el carácter de ancho propio │ entre las letras. El scroll horizontal también usa la posición real, en lugar de estimar 10dp por carácter. No modifica el texto, los espacios escritos, las flechas, la búsqueda por voz, el teclado ni las sugerencias de títulos.

Revisión estática sin nuevos diagnósticos Kotlin respecto a base y espejos verificados. Sin Gradle ni APK; pendiente revisión visual en TV. Sin SQL ni Vercel.
