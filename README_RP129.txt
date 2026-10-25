BruniO RP129 / 22233 / 2.0.0-rp129
Aplicar sobre RP128 conservando rutas. Incluye la versión corregida del receptor QR del hotfix RP128. Sin SQL ni APK.

HOME
La tarea Seguir viendo publicaba sus resultados después de esperar siete catálogos. Ahora publica al terminar su propia resolución, con la misma cantidad de consultas y guardas de cuenta/perfil/idioma.
Se reserva la clave/fila continue mientras llega el historial: al insertarse una fila encima de Pick your vibe, LazyColumn ya no conserva accidentalmente esa fila posterior como ancla visual. Si no hay historial al terminar, no crea títulos ficticios.
Cuando llega el contenido en el inicio sin interacción del usuario ni restauración de foco pendiente, se confirma scroll=0. No fuerza el regreso al inicio si ya estás navegando o volviendo a una tarjeta de detalles. Mantiene Continue Watching primero mediante la política de orden existente. Se respeta la ocultación explícita de filas del perfil.
La bandera de carga se reinicia con los mismos cambios de perfil/configuración que invalidan el Home. No cambia la política de progreso ni genera consultas nuevas a Supabase.

AJUSTES
Se retiran espaciadores manuales redundantes del contenido principal de Ajustes; las tarjetas conservan la separación común de 12 dp en vez de sumar múltiples márgenes.
Se registra el viewport de la lista y se revela la parte superior de una tarjeta recortada cuando se enfoca uno de sus primeros controles, solo si encabezado y control caben juntos. No fuerza a fijar tarjetas grandes al inicio mientras se navegan controles inferiores.
Los pedidos de desplazamiento se cancelan al cambiar foco o salir del componente; no se reinician cada frame. Se añade focusGroup por tarjeta y se comprueba que las coordenadas sigan adjuntas antes del desplazamiento.

QR / RED LOCAL
El QR de add-ons usa la red activa Ethernet o Wi-Fi y busca interfaces privadas disponibles como respaldo, sin consultar SSID ni banda. Omite loopback, interfaces apagadas, virtuales y nombres de túneles VPN en el respaldo.
Los receptores de formularios de add-ons, deportes, TorBox, canales manuales/LIDOM y listas M3U ahora aceptan clientes IPv4 privados aunque tengan otro prefijo/subred. Se conserva Host, ticket temporal, Origin y secreto de formulario. No abre acceso a clientes con IP pública.
El servicio de casting/control LocalCastServer no cambia. No se modifican los validadores del contenido enviado ni los métodos de guardado.
2.4 y 5 GHz no requieren configuraciones distintas en BruniO. El router tiene que permitir que ambos dispositivos se comuniquen: aislamiento de clientes, redes de invitados o VLAN sin ruta no se pueden superar desde la app. Solo IPv4 privada; no añade transporte IPv6.
No añade polling remoto, backend ni registros de URLs/tokens. Comprobar el manifiesto y guardar ajustes mantienen su flujo previo.

VALIDACIÓN
29 pruebas focalizadas pasaron: las 3 previas de orden de Home, 7 nuevas de arranque/encabezado/red, 7 de QR HTTP/admisión y 12 de manifiestos. Las pruebas HTTP usan loopback y no se omitieron por falta de interfaz LAN.
Fuentes Java de los receptores y helpers compiladas con javac contra firmas JSON Android. Kotlin del parche revisado con parser sin errores de sintaxis.
SettingsFocusRP129 y SettingsLayout compilados con el plugin Compose y las APIs reales 1.11.4 usadas por la app, usando colores de tema aislados de prueba. Esa comprobación parcial no compila Screens/AppState ni el módulo Android completo.
Falta confirmar el workflow Gradle completo, el comportamiento de foco/scroll en TV y conectividad con TV/celular reales, especialmente routers con bandas/subredes separadas. No se generó APK.
