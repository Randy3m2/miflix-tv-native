BruniO TV RC90 — Movimiento más continuo

INSTALACIÓN
Reemplaza los 20 archivos sobre RC89 conservando sus rutas y genera el build habitual de GitHub.
Versión 2.0.0-rc90 / versionCode 22089. Sin SQL, Edge Functions ni despliegue de Vercel.

CAMBIOS
- El movimiento con el mando acelera progresivamente después de 0.9 y 1.8 segundos de mantener la misma dirección. Pulsaciones nuevas, liberación y cambio de dirección reinician la aceleración.
- Horizontal, durante scroll: 180 → 150 → 120 ms entre repeticiones aceptadas. Vertical, durante scroll: 240 → 210 → 170 ms. Se conservan límites para evitar una cola de pulsaciones y cambios de selección después de soltar.
- Home sustituye la transición vertical de duración fija por un resorte críticamente amortiguado, con una cámara compartida entre filas. Un cambio de fila puede heredar la velocidad reciente en la misma dirección, en lugar de comenzar siempre desde cero.
- La velocidad transferida está limitada según distancia y se descarta al invertir la dirección, al llegar a un límite o si pertenece a un movimiento antiguo. El resorte frena hacia el encabezado de la fila sin rebote deliberado.
- Al soltar el control se detienen los nuevos cambios de selección; la animación vigente termina de acomodar la fila enfocada. No es un modo de scroll libre ni introduce selección por arrastre: adapta esa sensación al control remoto.
- Conserva el anclaje horizontal, las tarjetas que empujan a sus vecinas, los 3 segundos de sinopsis, los logos animados y la restauración de posición de RC89/RC88. No añade peticiones ni logs de Supabase.

VERIFICACIÓN
Ejecutados en JVM HoldMotionCheck, BrowseMotionCheck y KidsAndNavigationCheck: aceleración, reinicio al soltar/invertir dirección, límites de repetición, geometría y transferencia de velocidad. La ecuación del resorte con los límites usados se comprobó para evitar sobrepasar el destino. Ocho comprobaciones de integración/regresión aprobadas. Sintaxis de HomeShelfMotion, HomeShelfFrame y el bloque Home de Screens comprobada; fuentes espejo iguales.
No se compiló APK ni se midieron FPS/tiempos de fotograma en una TV real aquí. La sensación final requiere build y prueba en el dispositivo; no se promete equivalencia exacta con Apple TV/HBO.

PRUEBA EN TV
Mantén derecha y luego izquierda durante 3–5 segundos: debe acelerar gradualmente. Suelta y verifica que la selección no siga avanzando. Repite arriba/abajo en Home y cambia de dirección rápidamente. Confirma que al detenerse queden encabezado y tarjetas completos. Abre detalles y regresa a la misma tarjeta; conserva la prueba de expansión a los 3 segundos.
