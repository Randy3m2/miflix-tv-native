# BruniO TV Native RC17

## Antes de la build: configurar la firma una sola vez

Las builds anteriores usaban assembleDebug sin conservar debug.keystore. Los runners nuevos pueden generar certificados diferentes, lo que explica el conflicto de paquete al actualizar. Una actualización debe conservar el applicationId, tener un versionCode mayor y la misma firma.

RC17 usa un APK release con una clave permanente. La clave privada está en **BruniO_SIGNING_PRIVATE.zip**, separado del proyecto. No lo subas al repositorio, artifacts ni releases. Guarda una copia privada.

1. Descomprime ese ZIP privado en tu PC.
2. GitHub → repositorio **randy3m2/miflix-tv-native** → **Settings → Secrets and variables → Actions → New repository secret**.
3. Crea los cuatro secrets siguientes. Para cada valor copia TODO el contenido del TXT con el mismo nombre, sin añadir comillas:

| Name | Archivo con el valor |
| --- | --- |
| MIFLIX_SIGNING_KEYSTORE_BASE64 | MIFLIX_SIGNING_KEYSTORE_BASE64.txt |
| MIFLIX_SIGNING_STORE_PASSWORD | MIFLIX_SIGNING_STORE_PASSWORD.txt |
| MIFLIX_SIGNING_KEY_ALIAS | MIFLIX_SIGNING_KEY_ALIAS.txt |
| MIFLIX_SIGNING_KEY_PASSWORD | MIFLIX_SIGNING_KEY_PASSWORD.txt |

No cambies los valores y no generes otra clave para las próximas RC. El certificado público **signing_certificate_sha256.txt** sí forma parte del proyecto; el workflow comprueba que la firma real del APK coincida con él. Sin secrets la build se detiene antes de publicar otro APK con una firma distinta.

4. Sube SOLO el proyecto de **BruniO_TV_Native_RC17.zip** y ejecuta **Build MiFlix Native TV APK**.
5. Instala el APK publicado por ese workflow. La transición desde la firma debug antigua probablemente exige una última desinstalación; no tenemos su clave privada para firmar una actualización compatible. Anota las preferencias locales antes. Desinstalar elimina los datos que solo están en la TV; al iniciar sesión se recupera lo que ya esté sincronizado con Supabase.
6. Desde RC17 conserva siempre esos mismos secrets, la clave y el package com.miflix.native2. Aumenta versionCode con cada versión nueva. No publiques APKs debug como actualizaciones de este canal.

**No requiere SQL nuevo ni deploy nuevo de Pages o Trakt.**

## Actualizaciones desde la app

Settings → Check for Updates → Download → Install update.

- El workflow publica update.json con versión, versionCode, tamaño y SHA-256, junto con el APK.
- Se compara versionCode: una RC anterior nunca se ofrece como actualización por tener un nombre diferente.
- La descarga muestra porcentaje y MB, permite cancelar y no bloquea el resto de la app. El archivo se descarga temporalmente a la caché privada.
- El APK se descarga desde la release de esa versión. Se verifican tamaño, SHA-256, paquete, versión y certificado antes de ofrecerlo al instalador.
- En Android 8+ puede solicitar permiso para instalar desde BruniO. Actívalo, vuelve a Settings y pulsa Install update de nuevo. Después confirma la instalación en la ventana de Android.
- Si cancelas la ventana de Android, el APK sigue disponible en esta sesión para volver a intentar. Si cierras por completo la app, vuelve a comprobar y descargar.
- Los errores de conexión/almacenamiento/archivo/firma se muestran y se permite reintentar; no se intenta instalar un APK incompatible.
- La primera instalación de RC17 no tendrá una versión nueva que descargar hasta publicar RC18 u otra posterior. La integración del instalador necesita esa prueba real en TV.

El canal permanente y el Downloader conservan:
https://github.com/randy3m2/miflix-tv-native/releases/download/native-latest/MiFlix-TV-Native.apk

## Home

- Carátulas de Home aproximadamente 13–14% más estrechas; las de otros catálogos mantienen su tamaño.
- Espacios simétricos a ambos lados del viewport, fila recortada dentro de sus márgenes y espacio vertical después de la cabecera y antes del borde inferior. El highlight tiene sitio dentro de la fila; un avance parcial de la siguiente tarjeta puede aparecer dentro del viewport, como pista del scroll, sin invadir el margen exterior.
- Banner más compacto: 320 dp. Se conserva la navegación de Down y la animación rápida de RC16.
- Nuevas filas: Now playing, Top rated series, Action tonight, Science fiction, Time for a comedy, Thrillers & mystery. Se cargan en segundo plano con los extras; no bloquean la primera carga del catálogo principal.

## Reproducir aleatorio

El botón elige una película y usa los mismos enlaces/orden/calidad que la reproducción normal. Primero se apoya en las recomendaciones derivadas de puntuaciones positivas; si faltan, usa películas del historial (al menos 10% visto) y favoritos como referencias. Busca una puntuación TMDB próxima a la de esas películas, evita títulos terminados (80%+) o puntuados 4/10 o menos y reduce repeticiones de las últimas 20 selecciones.

Si no hay gustos registrados, elige entre películas del catálogo con buena puntuación. Esto es una selección por recomendaciones y reglas, no un modelo de IA entrenado. Necesita que la cuenta tenga su propio Torrentio/Comet; no garantiza que cada título tenga enlaces disponibles. Si no encuentra uno, muestra el error para volver a intentar.

El host puede usarlo para cambiar contenido en su sala. El guest conserva el contenido compartido que selecciona el host.

## Validación

El workflow ejecuta unit tests de release, compila assembleRelease y verifica APK/certificado/paquete/versión con las herramientas Android antes de publicar. Los tests nuevos cubren comparación de versiones/URLs y selección aleatoria. Las comprobaciones locales de sintaxis y empaquetado no sustituyen esta build ni la prueba en TV.
