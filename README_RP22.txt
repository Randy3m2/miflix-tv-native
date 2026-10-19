BruniO Android TV - RP22 / 2.0.0-rp22 / 22126
Aplicar SOBRE RP21 conservando rutas y reemplazando los archivos.

Debajo del teclado se eliminan los accesos a generos/categorias y temporada.
Ese espacio ahora muestra hasta cinco sugerencias de titulos del resultado
actual. Al pulsar, abre los detalles de esa pelicula o serie.
Al escribir una sola letra se filtran titulos disponibles localmente.
Con dos o mas, la busqueda remota espera 350 ms de pausa de escritura.
Se conservan dictado, teclado, las funciones de busqueda por animo/gustos
y el guardado del historial, aunque este ultimo deja de ocupar ese espacio.

Errores tipograficos: Tremoors propone Tremors como primera consulta de
respaldo si la busqueda original no encontro una coincidencia cercana.
Se conserva la tolerancia local a letras faltantes, intercambiadas y acentos.
Maximo dos consultas alternativas, como antes; cache y cancelacion al seguir
escribiendo. No se descarga un catalogo completo ni se consulta Supabase.

Validacion: ocho pruebas JVM de busqueda aprobadas, sintaxis revisada y
rutas espejo comprobadas. Falta validar foco/comportamiento real en TV.
No se ejecuto Gradle Android ni se genero APK. Sin SQL ni Vercel.
