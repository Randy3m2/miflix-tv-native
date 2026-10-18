BruniO TV — RP2 hotfix de pruebas
Aplicar sobre RP2, conservando las rutas y sustituyendo los dos archivos de pruebas.
No cambia la versión ni el código de reproducción. No requiere SQL ni Vercel.

Actualiza las dos expectativas antiguas: se conservan todas las fuentes españolas
antes de hasta 10 alternativas en inglés/desconocidas.
Añade regresiones para 16 fuentes españolas, orden personalizado de proveedores
y deduplicación de enlaces entre proveedores. Conserva las pruebas anteriores.

Validación: JUnit 4, StreamOrderingTest: 19 pruebas correctas contra StreamOrdering
de RP2, con los modelos y clases Java de prioridad/budget. Ejecución JVM aislada;
adaptador de política de ancho de banda sin efecto en estos casos (bandwidthBps=0).
No se ejecutó la suite completa de 81 pruebas ni un build Android/APK.
