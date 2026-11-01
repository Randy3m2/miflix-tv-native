# BruniO RP163 · Foco y selección en TV

Corrige el foco de botones y teclas del buscador. El control pulsado recibe el foco real de navegación y conserva su borde mientras permanece visible y seleccionado. Las flechas continúan desde ese control.

## Cambios

- Teclado de búsqueda: pulsar **L** la mantiene marcada al actualizar los resultados. Espacio, Borrar, voz y acciones del buscador comparten la corrección.
- Botones compartidos: se elimina el segundo destino de foco que creaban `focusable` y `clickable` sobre el mismo control.
- Clic de mouse/air mouse: establece el mismo foco utilizado por el mando; mover el puntero fuera no borra esa selección.
- Memoria por pantalla dentro del estado de navegación existente, separada por cuenta y perfil. Al volver, intenta recuperar el control anterior si sigue disponible. La navegación manual cancela una restauración pendiente.
- Settings, perfiles, navegación, Live TV, tarjetas, acciones sociales y otros controles compartidos usan esta corrección.
- Reproductor: los botones recuperan su selección al cerrar menús. Play/Pause comparte una identidad estable aunque cambie el icono. Se conserva el autoocultado habitual del reproductor.
- OK/Enter consume una pulsación completa; los eventos repetidos, cancelados o liberados tras perder el foco no deben ejecutar una segunda acción. Se mantienen las acciones de pulsación larga y Borrar todo tras 3 segundos.

## Aplicar

Base: RP162, commit `9a5197d66fba0d82d021b4963e1aacc76541a149`.

1. Copia `app/` del ZIP a la raíz del repositorio, respetando las rutas y sobrescribiendo sus archivos.
2. Ejecuta el workflow habitual de **Android TV** e instala la nueva build.
3. Verifica la versión **2.0.0-rp163 / Build 163** (versionCode 22267).

**No hay SQL nuevo ni hace falta actualizar el móvil para esta corrección.** Gemini y Now Playing conservan la configuración anterior. No se ha publicado nada en GitHub ni en Supabase.

## Comprobación en tu TV

- Busca, pulsa L y espera la actualización de resultados: L debe seguir marcada. Mueve las flechas desde esa tecla.
- Pulsa varias veces una letra o un interruptor: cada pulsación debe ejecutar una sola acción.
- Cambia un ajuste y comprueba que el foco permanezca en su botón.
- Abre y cierra un diálogo o vuelve de otra pantalla: comprueba la selección anterior si el control sigue presente.
- Desde el reproductor, abre Audio o Subtítulos y vuelve con Atrás. Comprueba también Play/Pause y las pulsaciones largas.
- Mantén Borrar 3 segundos: debe limpiar todo y conservar el foco de esa tecla.

La verificación local consta en VALIDACION_RP163.txt. La prueba física con tu mando/air mouse queda pendiente; los controles eliminados por una acción usan el destino disponible de la pantalla.
