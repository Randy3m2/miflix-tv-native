# BruniO TV RP138 — Ajustes, MotoGP y avatares

Versión TV 2.0.0-rp138 / 22242. Parche sobre RP137. Copia los archivos conservando sus rutas en la raíz de miflix-tv-native, sube los cambios y ejecuta el workflow habitual de TV para generar el APK firmado. Instala como actualización, sin desinstalar. No requiere SQL, claves nuevas ni reinstalación. Este ZIP contiene fuentes y recursos; no es un APK.

## Ajustes y navegación

- La página principal de Settings usa un formulario con scroll continuo. Se evita que una tarjeta alta sea tratada como un único elemento de una lista lazy y atraiga el foco hacia su inicio.
- Appearance, Add-ons y Language conservan sus opciones. La regla que revela un encabezado solo actúa al cambiar de control, evitando solicitudes repetidas al moverse el contenido.
- Audio principal y subtítulos usan filas con ancho limitado. El audio secundario queda en una tarjeta independiente, sin una fila horizontal gigante dentro de un solo elemento desplazable.
- El administrador de add-ons tiene una política local para revelar controles fuera de la ventana.
- Others / Live Sports: filtros, categorías y tarjetas comparten una sola cuadrícula desplazable. “Recordings by sport” deja de reducir permanentemente el espacio del listado. Se mantiene la composición lazy de los eventos y margen al final para revelar la tarjeta enfocada completa.

## MotoGP y logos

MotoGP se puede seguir desde Calendar > Subscriptions. Consume el calendario público de MotoGP, conserva el caché de calendario existente y usa sus avisos locales. Filtra Moto2/Moto3; normaliza los offsets horarios del circuito. Si solo se conoce el día, lo publica sin hora ni alarma inventada.

En Live > F1, las categorías y eventos de MotoGP del add-on ya no son descartados. F1 y MotoGP se distinguen por logos; los eventos aparecen cuando el add-on los publica. Las estadísticas de temporada de F1 siguen siendo de F1.

Logos locales: MotoGP, LIDOM, Grammys, Oscars y Miss Universe. Boxing usa un emblema propio del deporte, sin atribuirlo a una promotora. Los logos y sus imágenes originales conservan sus titulares; se incluyen los créditos y URLs consultadas en FUENTES_RP138.md.

## Avatares

Seis retratos locales por grupo (18 en total):
- Friends: Rachel, Monica, Phoebe, Joey, Chandler y Ross.
- VisionQuest: Vision, Ultron, Thomas, F.R.I.D.A.Y., E.D.I.T.H. y J.A.R.V.I.S.
- Loki: Loki, Mobius, Sylvie, Hunter B-15, Ravonna y Miss Minutes.

Son imágenes existentes de personajes, no ilustraciones generadas. Se cargan desde recursos pequeños incluidos en la aplicación, sin consultas de red al elegirlos. Los avatares y claves anteriores se mantienen.

## Verificación en la TV

Falta comprobar con un control físico: bajar hasta el último ajuste en Appearance y Add-ons, volver arriba, cambiar el idioma secundario, abrir una categoría de grabaciones y enfocar las últimas tarjetas. Comprueba también seguir MotoGP, sus logos en el calendario y los tres nuevos grupos del selector de avatares.

Se conserva el cambio automático de enlaces deportivos de RP137 y el teclado QR de RP136. VOD y listas M3U no cambian con este parche.
