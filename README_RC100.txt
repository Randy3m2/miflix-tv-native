BruniO RC100 — Funciones locales + idioma por perfil
Aplicar sobre RC99, conservando las rutas. No contiene APK ni proyecto completo.

AJUSTES
Settings > Playback / Appearance / System > Funciones locales · Listas y preferencias.
Las ocho funciones son opcionales y empiezan en OFF. Los interruptores son del dispositivo; las listas, ocultos, filtros y pistas se guardan por cuenta/perfil en esta TV.

1. Sleep timer: activar en ajustes. En el player, Funciones locales permite 30/60/90 minutos, detener tras 1/2/3 episodios/títulos o cancelar. Pausa y frena el siguiente automático; no apaga físicamente la TV. El temporizador es de la sesión y no se rearma al reiniciar la app.
2. ¿Sigues viendo?: elegir 2/3/5 episodios sin usar el mando. Bloquea el siguiente automático y espera respuesta; sin respuesta durante 60 segundos se detiene. Usar el mando reinicia el conteo. Estas protecciones funcionan fuera de Watch Party.
3. Listas personales: crear, renombrar y eliminar listas; añadir desde Detalles > Más opciones. Abrir desde Home o ajustes; quitar títulos sin afectar My List. Visualización en tandas de 25. No hay sincronización entre dispositivos.
4. Hoy no quiero ver esto: Detalles > Más opciones permite ocultar de las recomendaciones de Home por 1/7/30 días. No marca como visto ni cambia valoraciones. Búsqueda y listas siguen permitiendo encontrarlo. Restaurar todos desde ajustes.
5. Filtros persistentes: botón Filtros locales en catálogos para tipo, año, orden y ocultar vistos sobre títulos cargados. Conserva el contexto del género/catálogo. También recuerda tipo, género, rango de años, valoración mínima y orden del filtro avanzado al aplicarlo; restaurar preferencias no ejecuta una búsqueda. Actor/director no se guardan en esta versión. OFF conserva los datos pero desactiva los filtros locales y su memoria.
6. Audio/subtítulos por serie: recuerda idioma y etiqueta de la pista de audio, idioma/off de subtítulos y desfase al cerrar el panel o usar Atrás. Intenta restaurar la pista cuando está disponible en el archivo nuevo; nunca reutiliza URLs de subtítulos de otro episodio ni cambia la selección de códecs de RC97. Se puede borrar desde ajustes.
7. Panel técnico: player > Funciones locales > Panel técnico. Resolución, códecs, audio, idioma, bitrate y buffer según los datos disponibles. Actualiza la lectura local solo mientras está abierto; sin test de velocidad.
8. Backup JSON: exportación/importación manual con el selector de archivos de Android. Incluye listas y preferencias locales del perfil, orden/visibilidad de Home, idioma de interfaz, audio/subtítulos globales, estilo de subtítulos, color de foco y ajustes de autoplay/fuente. Sin contraseñas, sesiones, favoritos cloud, progreso ni URLs de reproducción. Restaurar solicita confirmación y requiere la misma cuenta/perfil. Tope de archivo: 2 MB. Si la TV no tiene selector compatible, se muestra un aviso.

IDIOMA POR PERFIL
El idioma de la interfaz queda separado por cuenta y perfil, localmente en la TV. Al cambiar de perfil se recupera su idioma y se actualiza el catálogo traducido. La configuración global anterior se usa como valor inicial y no se modifica al cambiar el idioma de un perfil. Es una corrección permanente, sin interruptor.

ENTREGA
15 archivos, con copias app/src/main/... y main/java/... como en los parches anteriores. Utiliza las rutas de tu proyecto y conserva su estructura.
Sin SQL, cambios web ni despliegue Vercel. No introduce polling ni llamadas Supabase en las funciones locales. Abrir fichas/reproducir/cargar más y refrescar el catálogo por idioma conservan las llamadas habituales.

VALIDACIÓN
Comprobaciones estáticas de integración RC100 y regresión de paginación RC98 aprobadas. Gramática Kotlin de archivos nuevos comprobada; archivos existentes sin diagnósticos de gramática adicionales frente al ZIP de entrada. No sustituye una compilación de Android ni prueba en TV.
No se generó build. Pendiente compilar y validar foco con mando, pistas reales, temporizadores, cambio de idioma entre perfiles y selector de archivos en el dispositivo.
