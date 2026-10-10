# BruniO TV RC20 · 2.0.0-rc20 / 22018

## Cambios

- Series: botón Siguiente inmediatamente después de Play/Pause y botón Episodios directamente en la barra. Siguiente se activa al encontrar un episodio emitido. En Watch Party el host cambia el contenido; el guest conserva pausa/reanudación y su selección de enlace. Películas y Live TV no muestran estos botones.
- Ajustes → Estilo de subtítulos: blanco, amarillo, cian, crema o verde; cuatro tamaños y restauración del estilo. Se guardan en la TV. Fondo y ventana transparentes, contorno negro; se ignoran estilos/tamaños embebidos de subtítulos de texto para respetar la selección. Subtítulos de imagen o incrustados en el video no pueden recolorearse con estos controles.
- Perfiles → Cambiar nombre, incluido el principal. Conserva ID, avatar, favoritos, progreso y puntuaciones. Se guarda en Supabase si hay sesión y localmente para perfiles sin sesión.

## Build e instalación

Sube el contenido del ZIP al repositorio existente conservando la carpeta desktop y el workflow PC. Ejecuta Build MiFlix Native TV APK desde main. Instala encima con la firma estable habitual o usa la actualización integrada después de publicar. Downloader conserva el mismo enlace. No requiere migración SQL ni cambios de Pages/Trakt.

## Comprobación en TV

1. En una serie muestra la barra: después de Play/Pause deben aparecer Siguiente y Episodios. Prueba el cambio de temporada y el último episodio emitido; Siguiente debe quedar desactivado cuando no hay otro. Autoplay existente continúa disponible.
2. Abre Episodios durante reproducción y durante pausa; cerrar el selector directo debe conservar el estado de reproducción. Navega con el control y elige un episodio.
3. Cambia color y tamaño en Ajustes, reinicia y reproduce subtítulos de texto externos e internos. Deben mantener fondo transparente y el tamaño/color elegidos. La imagen/PGS y los subtítulos quemados en el video conservan su apariencia original.
4. Renombra Principal y otro perfil; reinicia/sincroniza y verifica nombre, avatar, historial, favoritos y puntuaciones.
5. Con dos cuentas en Party, el host prueba Siguiente; comprueba sincronización del guest y pausa/reanudación.

La revisión local verifica sintaxis Kotlin, recursos y coherencia de versión/ZIP. La compilación Android se realiza en GitHub Actions; foco, reproducción y render de subtítulos requieren prueba real en TV.
