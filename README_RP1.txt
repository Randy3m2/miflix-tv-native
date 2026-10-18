BruniO Android TV — RP1
Parche SOBRE RC105. Nueva serie de versiones: RP1, RP2, RP3…
Versión 2.0.0-rp1 / versionCode 22105.

IMPORTANTE: ESTA VEZ SÍ REQUIERE SQL
1. Ejecuta SUPABASE_RP1_PROFILE_SOCIAL.sql completo en el SQL Editor del proyecto Supabase.
   Requiere las migraciones sociales anteriores, incluidas RC63, RC70 y RC73.
2. Copia las carpetas app/ y main/ del ZIP conservando las rutas.
3. Actualiza a RP1 las TVs que participarán en las nuevas amistades y salas.
4. En cada perfil, abre Friends & Party y configura su propio nombre de usuario.

SELECTOR DE ADD-ONS
- En el selector de enlaces: All o un complemento individual.
- Solo muestra como complementos de fuentes los que declaran el recurso stream;
  los catálogos sin reproducción no añaden una pestaña.
- Usa nombres de manifiesto y un número para distinguir instancias/configuraciones.
- Las opciones con cero resultados permiten ver que ese add-on no devolvió enlaces.
- Cambiar de filtro usa los resultados ya cargados; no vuelve a consultar proveedores.
- Los manifiestos se consultan como máximo una vez cada seis horas por instancia de repositorio.
  Sus nombres y capacidades quedan en memoria. Estas consultas van al proveedor, no a Supabase.

ESPAÑOL ESTRICTAMENTE PRIMERO EN LA SELECCIÓN AUTOMÁTICA
- Si el idioma de audio preferido es español, espera la consulta de todos los add-ons.
- Ordena el audio identificado como español por delante de inglés, independientemente del add-on.
- Si está activado Siguiente enlace automático, recorre todas las URLs españolas devueltas,
  eliminando duplicados de reproducción, antes de pasar a inglés.
- Elimina el límite anterior de tres fuentes españolas y el corte global de diez para ese grupo.
- Conserva límites acotados para los candidatos de inglés/sin idioma y para sesiones no españolas.
- Se basa en etiquetas de audio y nombres de archivo. Subtítulos o MULTI solos no prueban audio español.
- No puede encontrar fuentes que el proveedor no devuelve ni garantizar etiquetas correctas.
- Un enlace seleccionado manualmente sigue respetando tu elección.

IDENTIDAD SOCIAL POR PERFIL
- Nombre de usuario, amigos, presencia, recomendaciones, invitaciones, calendario social,
  momentos compartidos y Watch Party se separan por perfil bajo la misma cuenta.
- La autenticación sigue usando el correo y token de la cuenta: no se crean cuentas de acceso adicionales.
- La identidad del principal conserva el nombre y amistades existentes al migrar.
  Si el nombre ya había sido sobrescrito antes de RP1, cámbialo una vez en el perfil principal.
- Los demás perfiles empiezan con identidad propia y deben escoger un usuario único.
- Las frases y la referencia de sala locales también se guardan por perfil.
- Al cambiar de perfil se abandona/cierra el party anterior y se vacían los datos sociales en pantalla.
- Kids continúa sin acceso social. El servidor verifica que el perfil pertenece al correo autenticado.
- Si falta SQL, las funciones nuevas fallan sin escribir en la identidad social anterior.

COMPATIBILIDAD SOCIAL
- RP1 utiliza tablas/RPC sociales nuevas: los datos anteriores se copian una sola vez al principal.
- No borra ni cambia las tablas, políticas o funciones sociales anteriores.
- Desde la migración, el espacio social RP1 y el anterior evolucionan por separado.
  Las salas nuevas requieren RP1 en las TVs participantes; versiones anteriores y el web companion
  sin adaptar continúan usando el sistema anterior. Este ZIP actualiza Android TV.
- No añade polling, cron, envío de correos ni un heartbeat social nuevo.
- La inicialización de identidad añade una llamada por perfil utilizado y sesión del repositorio.

VALIDACIÓN
- Compilación JVM aislada de los cinco archivos de datos modificados y SourceFallbackBudget,
  usando adaptadores de plataforma/red: aprobada.
- Pruebas: proveedor español lento frente a inglés rápido, espera global, caché de manifiestos,
  exclusión de catálogos, 16 enlaces españoles antes de inglés, reintentos y actores por perfil.
- SQL ejecutado en PostgreSQL local mediante PGlite: migración y reejecución; separación de
  nombres, amigos y salas; rechazo de perfil ajeno/Kids; conservación de datos originales.
- Incluyó clonación y ejecución de los RPC reales de RC63/RC70/RC73 para presencia, invitaciones
  y momentos compartidos, con políticas de acceso.
- Análisis de sintaxis Kotlin: sin errores nuevos respecto de las fuentes previas.
- No se ha compilado la app Android ni generado APK. Falta validar UI, D-pad, reproducción
  y migración contra el esquema real de tu Supabase.

No requiere desplegar Vercel. 23 archivos, con rutas duplicadas idénticas.
