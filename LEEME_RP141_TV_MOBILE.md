# BruniO RP141 · Filmografía del reparto en TV y móvil

TV: 2.0.0-rp141 / 22245.
Mobile M5: 1.0.4-m5-rp141 / 22245.

Parche de código acumulativo sobre RP139 o RP140. Incluye los cambios móviles de RP140 para poder aplicar esta revisión aunque todavía no hayas subido ese ZIP. No es un APK firmado.

## Corrección

En móvil, tocar un actor ya no abre el buscador por nombre: abre un panel de filmografía con el identificador exacto de la persona seleccionada. Así no se reutiliza una búsqueda anterior ni se mezclan títulos coincidentes con su nombre u otras personas.

En TV y móvil se usa la misma consulta de créditos personales de TMDB. La respuesta se valida contra el identificador solicitado. Los créditos de reparto no se mezclan con trabajos de productor o director. Se excluyen noticias, talk shows, apariciones como Self/Himself/Herself e imágenes de archivo. Se conservan voces, personajes y trabajos sin acreditar que consten en el reparto. Película y serie con el mismo número de TMDB son entradas distintas.

El panel cambia su estado por persona, rol e idioma. Al cambiar/cerrar la selección se cancela su carga para evitar mostrar una respuesta atrasada. En móvil hay reintento si falla la consulta. Una lista vacía se muestra vacía: no se reemplaza por recomendaciones genéricas. Kids mantiene su validación de títulos; la filmografía de Kids revisa los primeros 40 créditos ordenados por popularidad, como la búsqueda anterior.

Los directores de TV también usan sus créditos exactos de dirección, incluyendo cine y TV. Los resultados dependen de los créditos registrados por TMDB; no son una búsqueda de disponibilidad en los add-ons.

## Aplicar y generar APK

1. Copia todos los archivos del parche conservando sus rutas al repositorio actualizado al menos a RP139.
2. Ejecuta los workflows habituales de TV y **Build BruniO Mobile APK** para generar los dos APK estables con la firma existente. No hay SQL ni secretos nuevos.
3. Actualiza las apps sin desinstalar ni borrar datos. No se cambian sus identificadores ni certificados.

Este parche conserva también RP140: historial móvil, selector de add-ons, Cast visible arriba, botón de mute eliminado, Tráiler/Party con acciones visibles y compartir carátula/sinopsis con dos opciones: BruniO o IMDb.

Si todavía no publicaste los dos archivos web de RP140, publica web/watch.html y web/.well-known/assetlinks.json para los enlaces BruniO. El JSON debe servirse en HTTPS, sin redirecciones ni login. IMDb no requiere esa asociación. El certificado público incluido corresponde a signing_certificate_sha256.txt.

Cast móvil abre la transmisión de pantalla Android del dispositivo; no añade un SDK de Chromecast directo. La presentación final de imagen y texto compartidos la decide la aplicación receptora.

Pendiente de dispositivo: abrir varios actores sucesivos desde películas/series diferentes, volver a la ficha y elegir otro actor, cambiar idioma/perfil, revisar navegación de TV y cierre/reintento en móvil. No se publica un APK firmado ni se despliega la web desde esta sesión.
