BruniO Android TV - RP18 / 2.0.0-rp18 / 22122

Aplicar SOBRE RP17 conservando rutas y reemplazando los archivos.

DEEPFLIX
Excepcion de tamano desconocido SOLO para el manifiesto del dominio exacto
mg.homelabx.qzz.io, igual que Addon Latam y Nuve+. Los enlaces con tamano
conocido conservan el limite configurado. No se permiten dominios parecidos,
ni se confia en el nombre visible del add-on. Tampoco se omiten los filtros
de ano, descargas sin cache ni errores HTTP del proveedor.
El nombre de respaldo de ese manifiesto ahora es Deepflix.

DIRECTOR EN REPARTO
Las tarjetas de director aparecen primero, con nombre, rol y foto cuando
TMDB la ofrece. Solo se usa el credito Director, sin confundir productores
ni creadores con directores. Al pulsar, se abren las peliculas dirigidas.
Se aprovecha credits, ya incluido en la respuesta de detalles: no se agrega
una consulta por tarjeta. Si TMDB no aporta director, no se inventa.

AVATARES PERSONALIZADOS - SQL RECUPERADO
Incluido: supabase_rc18_upgrade.sql, el archivo original al que se refiere
el aviso de la captura. Para habilitar ese almacenamiento, copiar su
contenido a Supabase > SQL Editor y ejecutarlo en el proyecto de BruniO.
Crea/configura el bucket publico miflix-avatars, JPEG hasta 512000 bytes,
y recrea sus tres politicas con subida/borrado limitados a la carpeta del
usuario autenticado. No se ejecuto SQL desde aqui.
Despues, pulsar New QR y volver a subir la imagen.

El HTTP 400 por si solo no prueba que falte SQL. Antes todos los fallos
recomendaban ejecutar ese archivo; ahora se distingue bucket faltante,
permisos, sesion, tamano y MIME sin mostrar rutas ni tokens del servidor.
Si el fallo continua despues del SQL, el nuevo mensaje ayudara a precisarlo.

VALIDACION
62 pruebas JVM aprobadas, incluidas excepciones de tamano, tarjetas de
director y diagnosticos de avatares. Sintaxis y rutas espejo verificadas.
No se ejecuto Gradle Android ni se genero APK. Falta validar APK/TV real.
SQL solo para avatares; Deepflix y director no requieren cambios de backend.
