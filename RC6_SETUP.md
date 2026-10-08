# BruniO TV Native 2.0 RC6

## Instalar sobre RC5

1. En Supabase → SQL Editor ejecuta `supabase_rc6_upgrade.sql`. Usa la cuenta del proyecto, no el usuario de la TV. Requiere la tabla existente `miflix_user_state` y la migración de puntuaciones RC5. Se puede repetir.
2. Reemplaza los archivos del repositorio con este proyecto completo.
3. Ejecuta **Build MiFlix Native TV APK** e instala el APK actualizado.

No hay cambios en la web de QR/chat ni en la función de Trakt: si ya están en RC4/RC5, no hace falta volver a desplegarlas. El identificador de Android y la URL `native-latest/MiFlix-TV-Native.apk` se mantienen. El código de Downloader no cambia. No es necesario desinstalar la app anterior si el APK usa la misma firma.

## Perfiles y avatares

- En Perfiles → Eliminar perfil se pide confirmación. Elimina el perfil, sus favoritos, progreso y puntuaciones. Se conserva al menos un perfil. Si eliminas el principal, otro pasa a ser principal; si era el activo, se selecciona el sustituto.
- El SQL elimina los datos en una transacción limitada a la cuenta autenticada, y conserva la configuración privada de complementos.
- Avatar → Quitar foto del perfil muestra la inicial.
- Eliminar debajo de un avatar lo quita de la galería de **esta TV**. Restaurar galería vuelve a mostrarlo. No cambia la foto de otros perfiles ni borra imágenes incorporadas al APK.
- La cuarta captura se incorpora sin modificar como **Especial 2**. Se conservan los cinco avatares anteriores.
- La barra lateral muestra la foto del perfil activo en todas las pantallas con menú.

## Idioma

Ajustes → Idioma de la app → Español / English. La preferencia es local a la TV, se conserva al cerrar la app y es independiente del idioma de audio/subtítulos. Los textos de interfaz cambian al seleccionar. El catálogo solicita títulos, géneros y sinopsis a TMDB en es-ES/en-US; cuando no existe traducción, el proveedor puede devolver el original. Nombres de perfiles, mensajes, frases personalizadas, títulos de canales y nombres de enlaces conservan su texto. Los errores técnicos externos pueden conservar el idioma del servidor.

## Pantallas

- Ficha: foco inicial en Play/Continuar con la vista al comienzo y sin recentrar el botón automáticamente. Se mantienen visibles título, datos y sinopsis. Los títulos largos tienen interlineado explícito y hasta dos líneas con elipsis.
- Acciones de la ficha y creación de perfil se distribuyen en filas cuando falta ancho.
- Avisos usa una campana de contorno que sigue los colores del menú.
- Ajustes tiene secciones separadas, espacio entre controles, filas de idiomas desplazables y márgenes al desplazar el foco. El título tiene su propio elemento de lista.
- El teclado sigue requiriendo OK para abrirse.

## Validación y límites

Se comprobaron sintaxis Kotlin, XML, SQL, referencias de recursos y contenido del ZIP. Se ejecutaron pruebas PostgreSQL de borrado de perfiles y pruebas de regresión de salas, puntuaciones, chat y Trakt con servicios simulados. La nueva migración se ejecutó dos veces en las pruebas.

Este entorno no dispone de Gradle/Android SDK: no se compiló un APK ni se validó visualmente en una TV. La compilación corresponde al workflow de GitHub. Tras instalar, comprobar con el control: ficha al inicio, títulos de dos líneas, desplazamiento en Ajustes, ambas opciones de idioma y borrado de un perfil de prueba.
