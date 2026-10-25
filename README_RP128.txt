BruniO RP128 / 22232 / 2.0.0-rp128
Aplicar sobre RP127. Incluye los hotfixes de dependencias de Add-ons y JSONException; no hace falta aplicarlos por separado. Conserva las rutas. Sin SQL ni APK.

QR LOCAL PARA ADD-ONS
Ajustes > Add-ons > Administrar > Agregar por QR local. También disponible dentro del formulario Agregar add-on con Pegar desde el celular · QR.
Escanear abre un formulario servido directamente por la TV en la red local. Se pega la URL configurada manifest.json o enlace stremio:// y se envía a la TV.
El QR rellena el campo; no instala ni guarda directamente. Volver al formulario > Comprobar add-on > revisar nombre/logo > Instalar. Así se conserva la validación y confirmación en la TV.
Requiere la misma red local Wi-Fi/Ethernet y una sesión iniciada en Ajustes. El receptor se cierra al salir del QR, cambiar perfil, salir de Ajustes o perder el estado de primer plano. No abre un servicio persistente.
QR temporal, entrega única y Nuevo QR para otro envío. Formulario oscuro adaptable a celular. El enlace privado viaja por POST y no aparece en la URL del QR ni en la respuesta del formulario. Conserva las comprobaciones LAN, Host, Origin y token de formulario del receptor local existente.
El envío local no usa Supabase, Vercel ni un servidor externo. Instalar después conserva el mecanismo de guardado existente de la cuenta/perfil; comprobar el add-on consulta su servidor, como antes.
La conexión debe usar una IPv4 privada de la red activa; si la red activa es VPN o no ofrece IPv4 local, se muestra un aviso.

COMPILACIÓN JSON ANDROID
AddonManifestRP126 captura JSONException al construir JSONObject y normaliza JSON inválido como IllegalArgumentException. Corrige la diferencia entre JSON Android (excepción comprobada) y org.json moderno de JVM (no comprobada).
Se incluyen el parser, el repositorio de metadatos y el administrador con el espaciado de RP127, evitando dependencias nuevas omitidas en este parche.

VALIDACIÓN
Suite de 17 pruebas con el port Android JSON: 15 pasaron (12 de manifiesto y 3 de validación/origen/token QR); 2 pruebas de socket se omitieron porque este runner no dispone de IPv4 LAN privada. Las pruebas de socket incluidas verifican entrega única sin eco de URL y reintento cuando la TV rechaza la recepción; deben ejecutarse en una máquina con LAN privada.
Los 12 tests del parser también pasaron con org.json moderno durante la corrección. Parser y receptor Java compilados con javac contra firmas JSON Android. Kotlin sin errores de sintaxis.
No se compiló el módulo Android/Compose ni se probó el QR con un celular/TV reales. Falta confirmar el workflow completo y esa validación de dispositivo. No se generó APK.
