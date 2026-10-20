BruniO RP47 — Corrección Save channel + tamaño de tarjetas
Aplicar sobre RP46. Versión: 2.0.0-rp47 (22151).

- Corrige el rechazo 403 del formulario cuando Chrome envía Origin: null o no envía Origin. La política no-referrer puede producir este comportamiento.
- Cada formulario incluye una clave independiente del QR; el guardado exige esa clave, la URL del QR vigente, el host correcto y la misma red local. Los orígenes externos explícitos siguen rechazados.
- Tras instalar, abre Mis canales y genera/escanea un QR nuevo. El formulario anterior no contiene la clave nueva.
- Tarjetas del menú Live más anchas, con solo un margen pequeño a ambos lados. Las imágenes principales conservan su proporción, con fondo ambiental oscuro para completar el ancho.
- Se mantienen las nueve tarjetas sin scroll, el regreso a la sección y las categorías de TV de RP46.

Validación: compilación Java aislada y prueba HTTP real en loopback con adaptador LAN de pruebas: guardado con Origin normal/null/ausente, rechazo de claves ausentes/incorrectas y de orígenes externos, conservación exacta de la URL. 3 tests JUnit de autorización del formulario. Sintaxis Kotlin e integridad de rutas duplicadas.
No se ejecutó Gradle ni se generó APK. Falta validar el formulario desde el celular y el aspecto final en TV.
No requiere SQL ni Vercel. Copiar conservando rutas.
