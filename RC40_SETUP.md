# RC40: opiniones justo debajo del reparto

Hotfix sobre RC39. Reemplaza los archivos respetando sus rutas, commit y ejecuta el workflow Android. RC40 / versionCode 22038 para Check for Updates. Mantiene los cambios anteriores.

El orden es Reparto > Opiniones · Trakt > Tráilers > Detalles. La sección tiene Actualizar comentarios, para poder alcanzarla con el control incluso en estados vacíos/cargando/error. Si no hay reparto, la sección igualmente aparece antes de los tráilers.

No requiere SQL ni cambios en emails. Si no desplegaste miflix-trakt de RC38/RC39, hazlo con supabase/functions/miflix-trakt/index.ts de esos paquetes para habilitar action comments. Si ya lo desplegaste, no necesitas repetirlo. La sección mostrará el estado de carga, ausencia de comentarios o error.

Verificación estructural: posición de la sección, destino de foco y copias app/main; compilación y prueba de TV pendientes en GitHub.
