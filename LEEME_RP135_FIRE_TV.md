# BruniO TV · RP135 · Compatibilidad Fire TV

Versión: 2.0.0-rp135 · código: 22239.
Preparado sobre main 438c4a9 (RP134). Es un update de archivos para el repositorio existente, no un proyecto independiente.

## Qué se encontró

- La aplicación declara minSdk 23, pero no habilitaba core library desugaring.
- Las secciones deportivas introducidas en RP120/RP121 usan java.time (API 26): LocalDate, ZoneId, Instant y DateTimeFormatter.
- SportsBrowseMemoryRP121 calcula la fecha al entrar a NBA, MLB, F1 y LIDOM en modo normal.
- El catálogo deportivo común y sus tarjetas también usan estas funciones; esa ruta se reutiliza en Others y MLB del modo fácil.
- Se descargó y examinó el APK público native-latest, que al revisar era RP132 / 22236. Su bytecode conserva referencias a java.time en las clases anteriores, sin la implementación compatible.

Esto es una incompatibilidad real para dispositivos API 23–25. Puede explicar el cierre en Fire TV antiguos. NO confirma el motivo de ese Firestick concreto: falta conocer su API o capturar su error. No se atribuye el problema a falta de RAM sin evidencia.

## Cambios

1. Habilita coreLibraryDesugaringEnabled y desugar_jdk_libs 2.1.5 en el módulo TV, manteniendo minSdk 23 y todas las categorías.
2. Añade un diagnóstico local del último error Java no controlado. Al abrir de nuevo la app muestra versión, API, modelo, ruta y clases/líneas del error.
3. Excluye mensajes de excepción, URLs, tokens y datos de cuenta del diagnóstico. No envía datos a Supabase ni a otro servidor. Solo escribe un reporte pequeño cuando ocurre un error fatal.
4. Conserva el manejador fatal original de Android. No oculta errores ni intenta continuar un proceso dañado.
5. Actualiza el changelog de TV a RP135, manteniendo las notas de Mobile M2 para su propia versión.

El diagnóstico NO puede registrar todos los cierres nativos o procesos terminados directamente por el sistema, y no garantiza resolver cualquier causa posible.

## Aplicar y probar

1. Copia los archivos de este ZIP en el repositorio, respetando sus rutas.
2. Ejecuta el workflow habitual de la versión TV: Build MiFlix Native TV APK.
3. Instala el APK firmado como actualización. No desinstales ni borres datos.
4. En el Firestick afectado, prueba NBA, MLB, F1, LIDOM y Others, además de MLB en modo fácil.
5. Si vuelve a cerrarse, abre BruniO otra vez. Si aparece Diagnóstico / Diagnostics, toma una foto antes de cerrar el mensaje.

No requiere SQL ni cambios en Supabase. No se publicó una release ni se modificó GitHub desde esta sesión.

## Fuentes técnicas

- https://developer.android.com/studio/write/java8-support#library-desugaring
- https://github.com/google/desugar_jdk_libs/blob/master/CHANGELOG.md

Los resultados de las comprobaciones locales aparecen en VALIDACION_RP135.txt. No se probó en el Firestick del usuario.
