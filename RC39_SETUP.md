# RC39: protección contra spoilers de episodios

Este hotfix acumula RC36 (tiempo de uso), RC37 (audio español prioritario) y RC38 (comentarios de Trakt). Sube sus archivos a GitHub respetando rutas, haz commit y ejecuta Build MiFlix Native TV APK. Versión 2.0.0-rc39 / versionCode 22037 para Check for Updates.

## Episodios
En detalles de la serie, el selector del reproductor y el preview del siguiente episodio, los no vistos esconden el título y la sinopsis. Conservan temporada, número y fecha. La imagen se difumina con cubierta oscura en Android 12+; en Android anteriores se muestra una cubierta sin cargar la imagen. El texto alternativo no contiene spoilers. Seleccionar/enfocar no revela los datos; Mostrar detalles (spoilers) en el diálogo del episodio permite revelarlos deliberadamente.

Visto = progreso del episodio >=96% o reproducción terminada. Se utiliza el progreso por episodio del perfil actual; no se supone que los episodios anteriores ya se vieron. Se conserva un registro local de completados para evitar ocultarlos otra vez al iniciar una repetición. El progreso sincronizado existente también puede desbloquear episodios completos; el registro adicional es local por cuenta/perfil. No se importa automáticamente el historial visto de Trakt.

## Comentarios de Trakt (incluidos de RC38)
Si ya desplegaste miflix-trakt con el código de RC38, no hay cambios nuevos que desplegar. Si todavía no lo hiciste: Supabase > Edge Functions > miflix-trakt > Code; reemplaza index.ts con supabase/functions/miflix-trakt/index.ts de este ZIP y Deploy. Conserva su autenticación. Requiere TRAKT_CLIENT_ID ya utilizado por la integración, y sesión en BruniO; no requiere vincular Trakt personal ni SQL. No modifiques la función de emails ni el cron.

## Validación
Revisadas las conexiones de protección en cards/lista/dialog/preview, ocultación de textos y contenido accesible, guardado de episodios completados y copias app/main. No se compiló el APK aquí: requiere GitHub/Android SDK. Prueba con un episodio sin progreso (datos ocultos), uno completo (visibles), reabrir/volver a verlo (siguen visibles) y cambiar de perfil (se evalúa su propio progreso). Prueba Android anterior a 12 para confirmar cubierta sin imagen.
