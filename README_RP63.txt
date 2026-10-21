BruniO RP63 — Social, perfiles, My List y reloj de 12 horas
VersionName 2.0.0-rp63 / versionCode 22167.
Aplicar SOBRE RP62, conservando todas las rutas del ZIP.

Incluido
- Panel de amigo al abrir su tarjeta: imagen del contenido, avatar, estado,
  Watch, Request Access, View details y Recommendations.
- Request Access usa joinWatchParty y la aprobación del anfitrión existentes.
  Solo se habilita cuando el amigo tiene sala, no en Kids, y muestra la
  solicitud pendiente. No concede acceso automáticamente.
- Conserva confirmación de salida del party al ver independientemente.
- Perfil social moderno: apodo, agregar amigo y frases separados visualmente.
  Conserva hasta ocho frases y la identidad independiente del perfil.
- Administrar perfiles: tarjetas con editor lateral para el perfil elegido.
  Nombre, avatar, subida de foto y eliminación con confirmación.
- Crear perfil: formulario propio con avatar, nombre y herencia de add-ons.
  Para foto propia, el QR se abre DESPUÉS de crear el perfil: la subida
  existente necesita un perfil real. El botón informa "Crear y subir foto".
- Fondos Top 10 en Administrar y Crear: mismo Top 10 de tendencias utilizado
  por el selector, caché compartida de seis horas, rotación cada 12 segundos,
  fundido de 1.1 segundos y movimiento lateral suave. Se pausa al salir al
  fondo. Sin sondeos nuevos a Supabase; si no hay imágenes, fondo negro.
- My List: cuatro columnas, primeras cuatro filas (16 títulos) y Cargar 16
  más. Tarjetas de póster del componente compartido de la app, sin expandir
  sobre sus vecinas. Se conserva pulsación larga, progreso, filtros locales,
  orden de adición, películas/series y retorno desde detalles. Se desplaza
  verticalmente: cuatro filas de tarjetas NO caben completas a la vez en
  una TV manteniendo su tamaño habitual.
- Hora de finalización en ambos controles del reproductor: "Finaliza 4:13 PM"
  o "Ends 4:13 PM". Reloj del panel de pausa también en 12 horas.
  Respeta zona horaria de la TV. Duraciones/posición siguen mm:ss / h:mm:ss.
- Se conserva RP62: Mandanguita con su nombre, prioridades e idioma
  secundario. P2P continúa retirado. No se añade el cliente Xuper pendiente.

Validación realizada
- Sintaxis Kotlin verificada con el analizador 2.3.21 del proyecto.
- Formateador Java compilado y ejecutado: medianoche, mediodía, tarde,
  cambio de día y zona horaria de Santo Domingo.
- Revisión de conexiones a acciones existentes, flujo de aprobación,
  creación/subida de perfil, caché/rotación y paginación de cuatro columnas.
- Sin SQL ni cambios a Vercel.

Estado del APK
NO se generó APK ni se ejecutó la compilación Android completa aquí.
Este entorno no tiene Android SDK, Gradle configurado ni las variables de
firma permanente de BruniO. No se creó otra firma incompatible.
Para producir el APK actualizable, aplicar este ZIP a tu repositorio RP62
y ejecutar tu workflow habitual "Build MiFlix Native TV APK", que usa los
secrets de firma ya configurados en GitHub. Falta validar layout, foco del
control y comportamiento real en TV.
