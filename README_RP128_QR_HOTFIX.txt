RP128 - Local QR HTTP hotfix
Aplicar sobre RP128 conservando rutas. Misma versión 22232 / 2.0.0-rp128. Sin SQL ni APK.

El log situaba ambos fallos en la obtención del formulario GET y su token, antes de validar el manifiesto. El receptor ahora mantiene su propio ticket temporal de 2 minutos y ya no depende de LocalCastAccess para esta sesión. Admisión IPv4 privada/subred implementada en el receptor, preservando el filtro LAN en la entrada pública. El API que usa la pantalla QR no cambia.
Las pruebas HTTP ya no buscan una interfaz LAN del runner ni usan Assume para omitirlas. Usan un factory package-private que solo enlaza loopback; el constructor público utilizado por la app sigue rechazando loopback y direcciones públicas. Se comprueba explícitamente esta separación y la admisión de subredes.
Se conservan origen, secreto de formulario, Host, expiración, entrega única y reintento cuando la TV rechaza el envío. Los mensajes de aserción ahora incluyen solo la línea de estado HTTP, para diagnosticar sin imprimir enlaces privados o tokens.

VALIDACIÓN
Javac contra JSON Android: compilaron el receptor, parser y pruebas. Pasaron las 19 pruebas: 7 del QR y 12 del manifiesto, sin omitir ninguna. Ambos flujos HTTP se repitieron 100 veces: 200/200 pasaron.
No se ejecutó la suite Gradle completa de 410 pruebas ni se probó el flujo con TV/celular reales; sigue pendiente confirmar el workflow y el dispositivo. No se generó APK.
