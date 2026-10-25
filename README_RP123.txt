BruniO RP123 / versionCode 22227 / 2.0.0-rp123
Aplicar sobre RP122 conservando rutas. Sin SQL ni APK.

NBA CALENDARIO
La consulta anterior de ESPN con dates=YYYYMMDD-YYYYMMDD devuelve HTTP 400. Se sustituye por dates=YYYYMM, una consulta por mes, sin solicitar cada día individualmente.
La respuesta mensual real supera 2 MB: se permite hasta 8 MB solo para NBA, manteniendo el límite anterior para otras fuentes. Se conserva la descarga/procesamiento en segundo plano y la caché de seis horas.
Las horas publicadas por ESPN pueden omitir segundos (ej. 2026-10-25T23:00Z). OffsetDateTime ahora acepta ese formato y conserva la zona al convertir a Instant; el parser anterior las descartaba.
El resultado se deduplica y filtra por fecha local del período solicitado. Se conservan equipos, logos y estado de cancelación/aplazamiento.
Se invalida una vez la firma de caché anterior, para volver a consultar los horarios tras actualizar. No requiere dejar de seguir ni volver a seguir los equipos.

F1
Logo oficial rojo guardado como recurso local para calendario/suscripciones y demás vistas que reutilizan CalendarSourcesRP120.logo. Sin depender de una descarga en la TV.
Origen: https://www.formula1.com/etc/designs/fom-website/images/f1-logo-red.png

LIDOM
Games abre directamente el componente original Team Channels. Se eliminan el selector Sports add-on/Team channels y su vista adicional. Se conservan sus juegos/calendario, canales asignados, fuentes, QR y YouTube del componente existente.

VALIDACIÓN
15 pruebas JUnit pasaron: 7 de NBA (consulta mensual, cruce de año, fecha bisiesta, rango inválido, equipos/logos, evento inválido aislado y aplazamiento) y 8 de suscripciones/caché de RP122.
CalendarSourcesRP120 y los helpers Java compilados en Kotlin/JVM con adaptadores mínimos de Android/HTTP para pruebas.
También se ejecutó el parser real y la selección mensual con una respuesta real descargada de ESPN: 155 partidos, todos con dos IDs de equipo; 154 publican ambos logos. La falta de un logo internacional no elimina el partido.
No se compiló Gradle/Compose/Android ni se probó el APK en TV. Esa validación sigue pendiente.
