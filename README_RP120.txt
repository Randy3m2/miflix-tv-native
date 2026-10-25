BruniO RP120 / versionCode 22224
Aplicar sobre RP119, respetando rutas. Parche de código, no APK. No requiere SQL ni Vercel.

CALENDARIO Y SUSCRIPCIONES
- Friends & Party > Calendar > Suscripciones, o Live > Others > Calendario y suscripciones.
- Sigues ligas o equipos NBA, MLB, LIDOM y F1, o guardas un evento individual desde su ficha.
- Equipo + liga no duplica el evento ni el aviso. Un equipo F1 añade las sesiones de los grandes premios en los que participa el campeonato.
- 77 equipos/escuderías con imágenes: 30 NBA, 30 MLB, 6 LIDOM, 11 F1.
- Logos en los días del calendario y detalle por día. El buscador permite equipo, liga o enfrentamiento (ejemplo: Warriors Lakers). Al escribir busca entre las actividades ya guardadas; al cambiar de mes consulta ese mes si falta caché.
- En primera actualización carga los próximos 93 días deportivos; un mes diferente se consulta al abrirlo. No se inventan juegos futuros no publicados.
- Fechas y horas en zona horaria del dispositivo, hora de 12 horas. F1 incluye carrera, clasificación, prácticas y sprint cuando la fuente los publica.
- Los eventos personales anteriores siguen en el calendario, sin migraciones destructivas.

AVISOS
- Avisos locales al comenzar y entradas en la campana, con leído/limpiar. Deduplicados por evento y hora.
- Por cuenta + perfil, guardados en esta TV; no se sincronizan entre dispositivos ni se recuperan tras desinstalar. El respaldo automático de Android depende del dispositivo.
- Solo avisa el perfil activo. Cambiar de perfil cancela sus alarmas pendientes y programa las del nuevo perfil.
- Pide permiso de notificaciones desde Suscripciones si hace falta. En Android 12+ usa Permitir avisos puntuales para autorizar alarmas exactas. Si no se autoriza o el fabricante restringe actividad en segundo plano, Android puede retrasar los avisos. No funciona con la TV apagada completamente o la app forzada a detenerse.
- Las fechas sin hora confirmada se muestran como Hora por confirmar y NO generan avisos de medianoche.
- Un único próximo aviso y una alarma de mantenimiento por perfil activo; no se programan cientos de alarmas individuales. Reprograma tras reinicio, actualización y cambio de hora.

POSICIONES
- Live > Others > Tabla de posiciones.
- NBA por conferencia (W/L/GB/L10), MLB por división (W/L/GB/PCT), LIDOM (W/L/GB/PCT), F1 pilotos y constructores (puntos/victorias).
- Selector de temporada actual y hasta cinco anteriores; scroll lazy y filas con logos.
- Caché pública local de seis horas por liga/temporada y Actualizar manual. Si falla la red conserva el último dato con aviso.

EVENTOS ESPECIALES / BOXEO
- Incluye fechas oficialmente anunciadas de Oscars 2027, Grammys 2027 y final de Miss Universe 2026. Solo Oscars tiene hora confirmada en el catálogo incluido.
- Ceremonias futuras y boxeo usan catálogo editorial, NO un scraping automático completo. Para ampliarlo sin otra build, sube BruniO_Events_Feed_Example_RP120.json a GitHub y pega la URL raw HTTPS en Suscripciones > Guardar fuente de eventos.
- Cada evento tiene id estable, league (BOXING, GRAMMYS, OSCARS, MISS_UNIVERSE u otra liga admitida), title, startsAt ISO con zona/UTC. Si solo conoces fecha: date YYYY-MM-DD y timed=false.
- Opcionales: teams (IDs de equipos), logos (URLs de imágenes HTTPS), cancelled=true. Conserva el mismo id al corregir horarios; el feed tiene prioridad sobre las fechas incluidas con esa liga/id.
- Debes seguir esa categoría para añadirla al calendario. No se reproduce contenido automáticamente.

CONSUMO Y FUENTES
- Sin nuevas llamadas o escrituras a Supabase para estas funciones. Los eventos personales conservan su integración previa.
- Horarios se refrescan como máximo cada seis horas automáticamente para el perfil activo; sin peticiones de red por cada movimiento del mando. Visitar meses nuevos y Actualizar manual sí consulta las fuentes.
- Datos deportivos públicos: ESPN NBA, MLB StatsAPI (MLB/LIDOM), Jolpica F1. No se garantiza disponibilidad de APIs no contractuales.
- Logos: ESPN NBA/MLB, recursos LIDOM existentes, URLs de TheSportsDB para F1. Ceremonias/F1 en días usan iconos temáticos propios. Si una imagen falla se conserva un identificador de texto.
- Fuentes de fechas especiales comprobadas:
  https://press.oscars.org/news/academy-and-abc-announce-show-dates-99th-and-100th-oscarsr
  https://www.grammy.com/news/2027-grammys-show-air-date-nominations-announced/
  https://missuniverseorg.com/press/f/for-immediate-releasemiss-universe-organization-announces-dates-f

VALIDACIÓN
- 24 pruebas JUnit de políticas y parsers pasaron en JVM independiente.
- Compilados CalendarSources, CalendarStore y Standings con adaptador de preferencias Android en memoria; prueba de aislamiento, entrega única y leído pasó.
- Verificados payloads reales: NBA 2 conferencias/30 equipos, MLB 6 divisiones/30 equipos, LIDOM 6 equipos y 115 sesiones de F1. No son datos hardcodeados en la app.
- Parser Kotlin sin errores de sintaxis; manifiesto y seis iconos vectoriales XML válidos.
- NO compilación Gradle/Android ni prueba con mando, notificación real o alarma en TV. Pendiente esa validación; sin Android SDK en este entorno.
