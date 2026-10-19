BruniO TV RP11 — PIN Live score
Acumulativo sobre RP6–RP10. Incluye RP7/RP8 y fixes completos, SplitScreen LIDOM
y Continue Watching desde 5 segundos. Subir TODOS los archivos conservando rutas.

Acceso:
- Botón de marcador en la barra superior del reproductor.
- LIDOM > PIN Live score.
- SplitScreen > PIN Live score.
- Settings > Fuentes / Complementos de reproducción > PIN Live score.

Elige NBA, MLB o LIDOM y una fecha; selecciona Fijar marcador.
Una píldora negra muestra logos pequeños, abreviaturas, marcador y período/entrada.
Puede verse dentro de BruniO, incluso durante reproducción; no es una superposición
del sistema sobre otras aplicaciones y no requiere permisos nuevos.
Solo un marcador fijado por perfil en esta versión.
Mover píldora: flechas del mando para desplazar, OK/Atrás para guardar.
Con ratón/pantalla táctil también se puede arrastrar. Quitar PIN elimina el marcador.
El interruptor permite ocultarlo y detener sus consultas conservando la selección.
Posición y PIN son locales por cuenta/perfil en esa TV. No se comparten entre perfiles.
Se oculta en selección de perfiles y en Kids. Horas previas al juego en AST.

Fuentes reales:
NBA: scoreboard de ESPN, verificado accesible desde este entorno.
MLB y LIDOM: MLB Stats API. Logos LIDOM usan los recursos existentes de los equipos;
NBA/MLB usan imágenes del proveedor, con caché de imágenes de la app.
La apariencia recuerda el PIN de Google, pero NO extrae ni reutiliza datos de Google.
El CDN NBA devolvió 403, por eso se usa ESPN. Los servicios pueden variar su acceso.

Consumo:
Marcador fijado y app visible: 30 s en juego, 120 s antes del juego.
Se detiene al finalizar; pausa al ir al fondo, desactivar, quitar PIN o entrar en
selección de perfiles/Kids. Reintentos escalonados hasta 5 minutos ante errores.
Caché local de 30 s por liga/fecha, compartida entre selector y píldora.
Sin marcador fijado no hay actualización periódica.
El selector consulta al abrir/cambiar liga o fecha; no tiene polling adicional.
No usa Supabase, tablas, funciones, Realtime ni consultas para marcadores.
Los errores conservan el último resultado con aviso, sin inventar nuevos puntos.

Validación:
6 pruebas JUnit JVM correctas contra respuestas reales de las tres ligas y casos
de caché, guardado/lectura, intervalos, errores, límites de posición y NBA en juego.
Fuentes Kotlin analizadas sin errores nuevos frente a las bases; métodos nuevos
y rutas espejo comprobados. Se incluye prueba y fixtures en test/resources.
NO se compiló Android ni se generó APK. Faltan CI y validación en TV del tamaño,
capas de ventanas, mando, arrastre, logos y reproducción simultánea.
No requiere SQL ni Vercel.
