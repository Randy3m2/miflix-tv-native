# BruniO RP164 — TV y móvil

Parche acumulativo sobre miflix-tv-native 9a5197d66fba0d82d021b4963e1aacc76541a149. Incluye los arreglos de foco y teclado de RP163. Copia app/ y mobile/ sobre el repositorio, conservando el resto. Compila e instala las dos versiones para recibir todos los cambios. No se ha publicado en GitHub ni desplegado el backend.

## Cambios

- Mi lista: guardado centralizado, cambios pendientes persistentes, lectura al abrir y volver al primer plano. Los errores de red conservan los cambios locales. TV y móvil: 30 títulos iniciales y otros 30 con Load more. Un fallo de metadatos muestra un título pendiente en vez de ocultarlo.
- Trakt: botón de importación en Mi lista; combina la watchlist con la lista compartida sin duplicados. No es sincronización bidireccional de eliminaciones con Trakt.
- Móvil: botón Rate visible en Details, puntuaciones de 1 a 10. Guarda en las tablas existentes de valoraciones, con la misma cuenta Y el mismo perfil que TV. Se actualiza al volver a la app o entrar a Details; no requiere SQL nuevo.
- Notificaciones del sistema para nuevos estrenos/episodios de Mi lista, además de la campana interna. Permiso desde Mi lista en Android 13+. Canal: BruniO · My List releases. La primera observación crea una referencia sin avisar de estrenos antiguos. Las fechas provienen de TMDB y no garantizan disponibilidad del stream.
- Trabajo de notificaciones cada hora, sujeto al planificador y batería de Android, con lotes rotativos de 30 títulos. Una lista grande tarda varias rondas. Tras añadir desde TV, abre Mi lista en el móvil para actualizar las suscripciones locales. El trabajo de fondo consulta TMDB; no son llamadas periódicas a Supabase.
- Dos widgets Android: Your calendar y Coming up. Mantén pulsado el Home de Samsung, entra en Widgets y selecciona BruniO. Usan eventos personales y suscripciones del calendario del perfil activo; al tocarlos abren Calendar. Refresco de caché local y trabajo de red cada 6 horas, sujeto a Android; botón de refresco manual.
- TV: se conserva la posición y foco al regresar de Details a Home. El Home explícito mantiene su función de volver al inicio.
- TV: bordes de foco y botones usan el color de tema seleccionado; guardado local inmediato y sincronización de ajustes.
- TV: miniaturas al avanzar con menor espera, caché y reutilización del extractor, recuperación ante fallos temporales y tarjeta que sigue la posición. Si una fuente no permite extraer imágenes, se muestra el aviso y el tiempo; no se garantiza soporte de cada proveedor.

## Verificación manual recomendada

1. Misma cuenta y perfil en TV/móvil: añadir, quitar, volver a abrir Mi lista e importar Trakt. Probar más de 30 títulos.
2. Puntuar desde el móvil, abrir Details en TV; cambiar desde TV y volver al móvil.
3. Volver desde Details a una fila lejana de Home; comprobar el foco con mando y teclado.
4. Cambiar tema, reiniciar y comprobar botones y menús.
5. Añadir ambos widgets en Samsung, cambiar perfil y comprobar eventos y apertura de Calendar.
6. Conceder notificaciones y revisar batería/permisos de BruniO.
7. Avanzar sobre una fuente compatible; probar fallo temporal y otra fuente.

Las comprobaciones automáticas no sustituyen pruebas en TV/Samsung reales ni validan una cuenta de producción. No se han usado credenciales personales nuevas.
