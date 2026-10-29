# BruniO RP148 · Francés y coreano

Aplicar los 16 archivos después de TV RP147 y móvil M6/RP142 (incluyendo los parches compartidos TV RP143–147), respetando rutas.

TV: 2.0.0-rp148 / 22252. Móvil: 1.0.6-m7-rp148 / 22252. Generar por los workflows existentes de TV y móvil. No APK firmado incluido.

## Interfaz

Ajustes → Idioma → Idioma de la app permite Español, English, Français y 한국어. El nombre del idioma seleccionado se muestra correctamente en TV y móvil. Se guarda por perfil, se acepta en la configuración sincronizada y restaura al cambiar de perfil. TMDB recibe es-MX, en-US, fr-FR o ko-KR para catálogo y descripciones cuando existan traducciones.

Incluye 226 entradas traducidas manualmente para cada idioma nuevo: navegación principal, reproducción, perfiles, búsqueda, calendarios, deportes y opciones habituales. Traducción parcial: mensajes avanzados, textos dinámicos y etiquetas antiguas que no usan tr() pueden permanecer en inglés. Se conserva inglés como fallback, nunca un texto vacío. Los títulos y textos de proveedores no se traducen automáticamente. No se usa un servicio de traducción en ejecución.

## Audio y subtítulos

Français ya estaba disponible en los selectores. Se conserva para audio principal, secundario y subtítulos; ahora la selección de enlaces da prioridad a la evidencia de audio francés incluso sin idioma secundario. Reconoce fr/fr-FR/fr-CA/fra/fre/French/Français, VFF/VFQ/TRUEFRENCH y bandera francesa. VOSTFR o subtítulos franceses no se confunden con audio francés. Metadatos explícitos de audio tienen prioridad sobre el nombre del archivo.

Media3 recibe fr-FR/fr-CA/fr como preferencias de audio. Subtítulos externos franceses reconocen fr-CA y muestran Français cuando no hay etiqueta. Audio y subtítulos coreanos existentes siguen disponibles.

Idioma de interfaz y de reproducción son independientes. Francés no puede garantizarse si la fuente no incluye esa pista. Los ajustes de Easy Mode que fuerzan español conservan su comportamiento.

## Verificación pendiente

Pruebas locales validan traducciones base, fallback, códigos de idioma, coincidencia y prioridad francesa, idioma secundario y distinción entre audio y subtítulos. Falta verificar las pistas reales de cada proveedor y el diseño en dispositivos físicos.
