BruniO RP127 / versionCode 22231 / 2.0.0-rp127
Aplicar sobre RP126 conservando rutas. Sin SQL ni APK.

AJUSTES
Contenedor común SettingsSectionCard con separación vertical de 12 dp, antes inexistente. Afecta las secciones principales, fuentes nativas y M3U incluidas en tarjetas, sin cambiar sus opciones.
Paneles comunes SettingsDialogRC103 con 16 dp entre controles y margen interno de desplazamiento; más espacio para enfoque y contornos.
Categorías principales revisadas: cuenta, reproducción, idioma, apariencia, add-ons y sistema. La selección de idioma admite salto de línea en vez de forzar una fila demasiado ancha. Mayor separación entre tarjetas y filas compactas.
Administrador de add-ons: cabecera separada de sus acciones, acciones en FlowRow y presets que envuelven al no caber; separación entre campo personalizado y Guardar.
Prioridad de add-ons, preferencias locales y fuentes LIDOM: más separación y filas adaptables en grupos de botones. Perfil social: mayor espacio dentro de los grupos de ajustes.
No cambia datos guardados, límites, herencia, métodos de sincronización ni el comportamiento de las opciones.

CHECK UPDATES
Modal oscuro/glass con icono BruniO, versión instalada, estado principal y descripción legible.
Estado de descarga con barra y porcentaje acotados cuando existe tamaño total; bytes descargados cuando todavía no se conoce el total.
Acciones de comprobar/descargar/instalar/cancelar envuelven y mantienen su lógica existente. X de cierre siempre arriba, contenido desplazable.
Ver novedades abre el changelog existente. Detalles de instalación plegados y leídos en IO solo al abrirlos.
Conserva verificación, repositorio de actualizaciones y flujo del instalador Android. No cambia URLs de descarga ni comprobaciones de integridad.

VALIDACIÓN
Revisión del espaciado y comparación automática de llamadas state/repo/extras: se conservan las invocaciones de acciones de los ocho archivos de UI auditados. Parser Kotlin sin errores de sintaxis.
No se ejecutó compilación Gradle/Compose/Android, ni medición visual o de foco en TV. La revisión de código no garantiza ausencia de superposiciones en todas las resoluciones/escalas de texto; falta validar el APK en el dispositivo. No se generó APK.
