BruniO RP45 — Mis canales por QR
Versión: 2.0.0-rp45 (22149). Aplicar sobre RP44. Incluye el hotfix de import LiveChannel.

Uso:
1. Live / En Vivo → Others → Mis canales → Agregar canal por QR.
2. Conecta el celular y la TV a la misma red. Escanea el QR.
3. Escribe un nombre y pega la URL HTTP(S) directa del stream. Pulsa Guardar.
4. Pulsa Volver en la TV; el canal aparecerá en Mis canales. OK reproduce.
5. Mantén OK sobre el canal para editarlo por QR o eliminarlo. Nuevo QR renueva un QR vencido (2 minutos).
6. Durante la reproducción, OK o derecha abre Mis canales. Arriba/abajo cambia dentro de tu lista, sin saltos al llegar al extremo.

Los canales se guardan únicamente en ese perfil, cuenta y dispositivo. Máximo 100 por perfil. Reenviar una URL existente actualiza su nombre sin duplicar la entrada. No se instala ningún canal del ejemplo por defecto.
Las URLs se reproducen en Media3; .m3u8 usa HLS y .mpd usa DASH. No se abren como página web. Una página HTML o un enlace vencido no se convierte automáticamente en video. Los enlaces temporales pueden reemplazarse con Editar.
El formulario solo está activo con el QR abierto y BruniO en primer plano; URL/canal no se envían a Supabase. No disponible para invitados de Party ni Kids.

Validación realizada:
- Compilación Java aislada del receptor y 3 tests JUnit de validación de URL.
- GET/POST y cierre reales en loopback con adaptador de red de pruebas: validación de origen, formulario, escape HTML y preservación exacta de tokens.
- Código real de almacenamiento Kotlin contra SharedPreferences simuladas: aislamiento por cuenta/perfil, edición, duplicados, borrado, límites, MIME y navegación.
- Sintaxis Kotlin y comparación de todas las rutas duplicadas.
No se ejecutó Gradle ni se generó APK. Falta validar QR celular/TV, interfaz y reproducción en la TV real.
No requiere SQL ni Vercel. Copiar conservando rutas.
