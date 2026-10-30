# BruniO RP162 · Now Playing en el móvil

La fila **Now Playing / Reproduciendo ahora** aparece encima de Continue Watching cuando una TV de la misma cuenta reporta contenido activo. Usa tarjetas horizontales, con imagen, título, perfil y temporada/episodio cuando están disponibles. Tocar una tarjeta del catálogo abre sus detalles en el móvil. No inicia ni interrumpe la reproducción de la TV.

- **Sin botón nuevo.** Pulsar Home de nuevo refresca la fila, incluso estando ya en Home.
- También consulta al entrar a Home y al volver a la app con Home visible.
- Sin contenido, la fila desaparece completamente; no hay placeholder ni encabezado vacío. Los filtros Movies/Series también aplican a la fila.
- Se excluyen las reproducciones de teléfonos y las cuentas ajenas. Puede mostrar la sesión más reciente de cada perfil de esa cuenta. No se muestra en Kids, invitado sin sesión ni modo fácil.
- Gemini conserva su configuración y funcionamiento de RP160. La integración GPT fue cancelada y no está incluida.

## Instalar

1. Parte de RP160, commit `ec7410822653f9bc3e8947942ec5498e0721d93a`. Este paquete se aplica directamente: no necesitas RP161 Private Kit.
2. Copia `app/` y `mobile/` del ZIP al repositorio respetando sus rutas y sobrescribiendo archivos. No se ha escrito en GitHub desde este chat.
3. En tu proyecto Supabase existente, abre **SQL Editor**, pega todo `supabase_now_playing_rp162.sql` y ejecuta una vez. Requiere la migración RP1 de perfiles/social que ya usa la app; no la sustituye. El SQL se puede ejecutar de nuevo sin duplicar datos.
4. Ejecuta los workflows habituales para **Android TV** y **BruniO Mobile**. Instala ambas builds. TV: `2.0.0-rp162`; móvil: `1.0.7-m8-rp162`; versionCode 22266 en ambas.
5. Abre la TV con la misma cuenta y empieza una reproducción hasta que aparezca imagen. Abre Home en el móvil o púlsalo de nuevo. La TV también necesita esta actualización para identificar sus eventos como TV.
6. Cierra el reproductor normalmente en la TV y pulsa Home en el móvil: la fila debe desaparecer. Los dispositivos pueden estar en redes diferentes; usan tu cuenta de Supabase.

## Consumo y comportamiento

Se reutilizan los eventos de presencia que la TV ya publica al comenzar/salir de la reproducción; solo se añade una etiqueta de tipo de dispositivo a su identificador. **No se añaden heartbeats, tablas, cron ni escrituras periódicas.** El móvil hace una lectura pequeña por entrada/regreso a Home o nueva pulsación de Home. Las imágenes y metadatos se resuelven con el catálogo TMDB existente, con caché y máximo tres peticiones de metadatos simultáneas.

Esta fila refleja la **sesión de contenido activa que reporta la TV**. No es un seguimiento continuo del segundo exacto ni un control remoto. Una sesión pausada puede seguir apareciendo mientras permanezca abierta en el reproductor. Si la TV pierde corriente o red antes de enviar el evento de salida, puede permanecer hasta vencer la vigencia de presencia existente. Esa vigencia depende de la duración de la sesión y tiene un máximo de 12 horas. No se ha añadido polling para detectar esos cierres abruptos. Cada perfil conserva una presencia actual; dos TVs usando el mismo perfil comparten ese último registro.

Si falta el SQL, la sesión ha caducado, no has iniciado sesión o falla la conexión, la fila se oculta y el resto de Home sigue funcionando. No se muestran errores técnicos en Home. Tras configurar SQL y actualizar la TV, pulsa Home para volver a consultar.

## Validación

Ver `VALIDACION_RP162.txt`. Las pruebas de SQL utilizan PostgreSQL local con fixtures; no se ha ejecutado SQL ni publicado cambios en tu Supabase. La comprobación física entre tu TV y tu teléfono queda pendiente.

Para reproducir las pruebas de aislamiento SQL localmente (Node 24):

```bash
npm ci --prefix tests/now-playing-rp162
npm test --prefix tests/now-playing-rp162
```
