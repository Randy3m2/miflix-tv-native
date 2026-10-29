# BruniO TV · RP151 / Build 151

Parche acumulativo para la rama main en RP149 de Randy3m2/miflix-tv-native. Integra la RP150 recuperada del chat anterior y añade las funciones descritas abajo. Versión TV 2.0.0-rp151; versionCode 22255. No contiene APK firmado ni publica cambios en GitHub.

## Instalar el código y compilar

1. Guarda una copia de tu proyecto RP149. Extrae este ZIP.
2. Copia su carpeta `app` sobre la carpeta `app` del proyecto completo, aceptando reemplazos y manteniendo las rutas. No crees una carpeta `app/app`.
3. Sube esos archivos al repositorio. No se requiere ejecutar SQL ni añadir una clave compartida de IA a GitHub.
4. Ejecuta el workflow existente `Build MiFlix Native TV APK` (`build-native-tv-apk.yml`). Usa las claves de firma y TMDB que ya tiene el repositorio. Este workflow publica el APK y actualiza el canal de actualizaciones. No se ejecutó desde este chat.

## Dónde encontrar las funciones

- **Franchises:** entra a una franquicia desde Collections. Timeline horizontal con nodos conectados, carátulas, foco blanco, información del título enfocado, progreso y selector de estreno/historia. Catálogo conserva el listado anterior. X-Men ofrece estreno; no se inventa una cronología para sus continuidades alternativas. Marvel cubre Infinity Saga, Star Wars el itinerario central; TWD/GOT usan recorridos curados parciales. Las series se solapan y no se reordenan todos sus episodios.
- **Botón ✦ AI en la barra de TV:** filtros locales ES/EN, Gemini opcional, presupuestos de 15/30/45/60/90/120 minutos y colecciones guardadas por perfil. Incluye episodios con duración conocida y contenido empezado que puedes terminar dentro del presupuesto. Audio español es una preferencia: se confirma en los enlaces/pistas disponibles.
- **Scene Finder dentro de AI:** escribe una serie y una pista breve de la escena. Selecciona la serie y la temporada inicial. Busca hasta tres temporadas por solicitud en títulos y sinopsis reales, permite abrir el episodio encontrado y excluye episodios futuros. Naruto Shippuden/Pain se precarga como ejemplo editable. Las coincidencias no prueban que una escena ocurra allí. No analiza el vídeo ni ofrece minuto exacto.
- **Modo invitado dentro de AI:** sesión en memoria, sin alterar el perfil habitual. Cierra el reproductor y sal de cualquier party antes de entrar; usa Salir invitado o Back desde Home para regresar. Hereda add-ons para reproducir, sin heredar sesión de nube ni clave Gemini.
- **Votar qué ver dentro de Amigos y Party:** el host inicia una ronda con hasta tres opciones; incluye el siguiente episodio cuando corresponde y películas del catálogo. Cada miembro tiene un voto y puede cambiarlo. El host reproduce la opción ganadora; decide en caso de empate. Las rondas vencen a los diez minutos.
- **Miniaturas al buscar:** activa la opción desde AI. En el reproductor de TV normal, mueve izquierda/derecha sobre la barra y espera sobre un punto: OK confirma el salto y Atrás cancela. Extrae fotogramas de la propia fuente en segundo plano, con una sola extracción simultánea y caché de hasta doce imágenes pequeñas. No descarga un modelo de IA. Es opcional, usa datos de vídeo y depende de la fuente/TV; si falla mantiene el tiempo y avisa que no hay miniatura. Android anterior a API 27 conserva solo el tiempo. Live TV, móvil y modo sencillo conservan sus controles habituales.
- **Aviso poscréditos:** aparece cerca de los créditos si IntroDB devuelve una marca válida para esa película o episodio. El botón permite ir a esa escena. No es análisis automático universal: sin marca no se afirma si hay o no escena.

## Gemini sin suscripción

Esta entrega usa `gemini-3.5-flash-lite`. La documentación actual de Google lo ofrece en Free Tier; disponibilidad y cuota dependen de la cuenta/proyecto. La app no puede comprobar que tu clave pertenece al nivel gratuito: para evitar cargos usa una clave de un proyecto sin facturación. No existe una promesa de servicio gratuito ilimitado.

Configura tu propia clave de Google AI Studio por QR en AI → Configurar Gemini. La clave se cifra con Android Keystore en la TV. Solo se envía la consulta al pulsar Buscar; no se envían historial, contactos, películas ni enlaces de reproducción. El nivel gratuito de Google puede usar el contenido para mejorar sus productos. El límite local es veinte solicitudes diarias, separadas al menos veinte segundos; si Google falla, se usan filtros locales. No se cambian automáticamente modelos/proveedores.

Referencias verificadas el 29 de octubre de 2026:
- https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite
- https://ai.google.dev/gemini-api/docs/pricing
- https://ai.google.dev/gemini-api/docs/generate-content/whats-new-gemini-3.6

## Consumo y compatibilidad

Discovery, colecciones locales, Scene Finder, invitado y miniaturas no añaden consultas a Supabase. La reproducción/fichas mantienen los eventos normales existentes. Voting reutiliza el polling del party: cada apertura, voto y cierre añade un evento; no añade un polling nuevo ni requiere tablas nuevas. Todos los participantes deben usar RP151 o posterior para ver los controles. Clientes viejos podrían mostrar mensajes técnicos de la votación en su chat.

Una persona que se reconecta solo recupera la ronda si el mensaje de apertura sigue entre los últimos sesenta eventos que el protocolo actual devuelve. Si el chat lo desplazó, el host debe cerrar/abrir otra ronda; esta entrega no añade consultas de recuperación.

## Comprobación en TV real

1. Alterna estreno/historia, recorre carátulas, abre detalles y vuelve; revisa foco y scroll.
2. Prueba una consulta de 30 minutos y una colección de terror de los 90.
3. Configura Gemini con clave propia Free Tier y comprueba tanto respuesta como fallback ante error de cuota.
4. Mira un título como invitado y comprueba que no cambia el Continue Watching del perfil habitual.
5. Con dos equipos RP151: abre una votación, cambia un voto, verifica empate y reproduce como host.
6. Activa miniaturas, prueba varias fuentes, confirma/cancela el salto y revisa que el vídeo siga estable.
7. Usa un título con marca poscréditos disponible para comprobar el aviso.

No se han probado físicamente una TV, dos dispositivos en party, fuentes reales, ni una clave real de Gemini. Scene Finder con reconocimiento visual y minutos exactos sigue pendiente de una fuente verificable de escenas/subtítulos.
