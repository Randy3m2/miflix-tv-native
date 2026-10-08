# BruniO TV Native RC9

Proyecto completo acumulativo. Incluye RC6, RC7 y RC8.

## Instalar

Si ya ejecutaste `supabase_rc6_upgrade.sql`, no necesitas SQL adicional. Si no, ejecútalo para habilitar la eliminación de perfiles. Reemplaza los archivos del repositorio, lanza Build MiFlix Native TV APK e instala el APK con la misma firma. Pages y Trakt no requieren redeploy. El código de Downloader y la URL permanente no cambian.

## Cambios

- Playback links: tarjetas alineadas a la izquierda con proveedor, nombre y título en varias líneas. Al enfocar se muestra el texto completo. Se añade peso en GiB/MiB cuando el complemento informa `videoSize`, y nombre de archivo cuando está disponible. Los detalles de resolución, idioma, códec y otras etiquetas se conservan tal como los entrega el proveedor. No se inventa peso cuando falta.
- Inicio: navegación flotante sobre el fondo de la portada; su velo oscuro aumenta al bajar. La barra no reserva una franja opaca fija. Buscar usa una lupa blanca dibujada en la interfaz, con contraste al recibir foco.
- Regreso al banner: al enfocar Ver detalles o la navegación superior se vuelve al comienzo del listado. El desplazamiento por foco deja de recentrar el botón a mitad de portada.
- Géneros: nueva fila en Inicio, con imágenes de películas obtenidas por el género correspondiente en TMDB. Se comparten con Colecciones. Se cargan en segundo plano y se reutilizan durante la sesión; requieren el token TMDB y conexión. Si no se puede obtener/cargar una imagen, sigue visible el título del género.
- Directores: Christopher Nolan, los hermanos Russo, Steven Spielberg, Denis Villeneuve, Martin Scorsese, Quentin Tarantino y Greta Gerwig. Las fotos se obtienen de TMDB. Cada tarjeta abre los créditos de películas donde figuran como Director; se excluyen créditos de actuación/producción y se deduplican películas compartidas por los Russo. Esta fila es de películas, no incluye créditos de series.
- Mi apodo/frases: cada campo y botón se distribuye en una columna con 16 dp de separación, y 24 dp entre secciones. El diálogo sigue siendo desplazable.
- Perfiles: nombre y foto en una fila; elegir avatar y crear perfil en otra. Se añade espacio entre acciones de perfiles existentes y entre la creación y Volver.

## Comprobación

Se verifica sintaxis Kotlin/XML/SQL, recursos, rutas de navegación y ZIP. Este entorno no tiene Gradle ni Android SDK: no se compiló el APK y no se verificó el diseño en una TV. Tras instalar, revisar la navegación con el control, regreso al banner, desplazamiento del selector de enlaces y aparición de imágenes/directores con conexión.
