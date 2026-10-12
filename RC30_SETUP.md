# RC30 — Recomendaciones entre amigos + Watch

Versión 2.0.0-rc30 (22028). Incluye RC29 y su inicio animado. Conserva la lógica del reproductor RC25; se amplía solamente su panel de amigos.

## Instalación
1. En Supabase → SQL Editor, ejecuta `supabase_rc30_upgrade.sql` completo. Requiere las migraciones sociales anteriores que ya usa tu app. Es reutilizable: no borra datos y mantiene los endpoints de versiones anteriores.
2. Sube el contenido de este ZIP al mismo repositorio de TV.
3. Ejecuta **Build MiFlix Native TV APK**, conservando los secretos de firma existentes.
4. Instala la nueva APK. Las otras personas necesitan RC30 para ver estos botones.

No hay que publicar funciones Edge ni configurar nuevos secretos.

## Dónde está cada opción
- Ficha de película/serie → **Recomendar a amigos / Recommend to friends**. Confirma para guardarla en tu lista pública para amigos aceptados. No envía mensajes ni cambia Mi lista.
- **Friends & Party → Recomendaciones de amigos**: lista compartida con carátulas, títulos y quién recomienda cada uno. Incluye tus recomendaciones y las de tus amigos; cambia a Mis recomendaciones para ver solo las tuyas. Puedes abrir la ficha o quitar recomendaciones propias. Máximo 500 recomendaciones recientes en la vista compartida.
- Selecciona un amigo → **Ver recomendaciones**: solo las de esa persona, aunque esté desconectada.
- Selecciona un amigo viendo una película/serie → **Watch / Ver por mi cuenta**: inicia ese título con tus add-ons. Para series usa la temporada/episodio mostrado y tu progreso guardado; no toma la posición de tu amigo ni entra en su sala.
- **Request Access / Solicitar acceso**: sigue solicitando aprobación para sincronizarte con su party. Solo aparece si hay una sala disponible.
- Estas opciones también están dentro del reproductor → Party → selecciona un amigo.

Si ya estás en un party, Watch pide confirmar la salida. Si eres host, indica que se cerrará tu sala. Live TV/Live Sports no están incluidos en el Watch independiente de esta versión.

No se agrega ninguna sección nueva al menú superior. Actualizar sigue siendo manual; las recomendaciones se cargan al abrir su ventana y tienen su propio botón Actualizar.

## Privacidad y compatibilidad
Cada cuenta administra sus recomendaciones; la clave cuenta+título impide duplicados. Solo el dueño puede borrar/modificar las suyas. Solo amigos con amistad aceptada pueden leerlas; solicitudes pendientes y otros usuarios quedan fuera. No se comparten URLs de reproducción, tokens ni credenciales de add-ons. La actividad v30 agrega únicamente el identificador de catálogo y conserva la expiración de 90 segundos.

## Verificación
Revisadas la sintaxis Kotlin/SQL y las diferencias frente a RC29. No hay SDK Android ni servidor Supabase local: compilación y pruebas reales pendientes en Actions/TV.

Prueba con dos cuentas amigas: recomendar un título dos veces (una sola entrada), actualizar la lista en la otra cuenta, abrirlo, filtrar por amigo y borrar la recomendación propia. Una cuenta no amiga no debe verla. Prueba Watch con ambas cuentas en contenidos distintos y confirma que no se crea membresía ni sincronización; luego prueba Request Access y aprobación. En un party existente, cancelar Watch debe conservarlo y confirmar debe salir/cerrar según el rol.
