# BruniO PC · Windows 10/11 x64 · 1.0.0-rc1

Cliente de escritorio real en Python + Qt (PySide6) con LibVLC integrado en la ventana. Comparte el backend y el protocolo de la versión TV. Este ZIP contiene el proyecto para generar el instalador, no un instalador ya compilado.

## Generar el .exe con GitHub

1. Descomprime BruniO_PC_Windows_RC1.zip.
2. En el mismo repositorio **randy3m2/miflix-tv-native**, añade la carpeta **desktop/brunio-pc** completa y el archivo **.github/workflows/build-pc.yml**. No sustituyas los archivos de Android ni sus workflows. El ZIP ya tiene esa estructura.
3. Actions → **Build BruniO PC Windows** → Run workflow.
4. El workflow instala dependencias fijadas, ejecuta tests, prueba Qt, descarga VLC x64 de VideoLAN verificando SHA-256, prueba su decodificador, genera la app y su instalador con Inno Setup. Si falla cualquier check, no publica.
5. Al terminar descarga **BruniO-PC-Setup-1.0.0-rc1.exe** del artifact **BruniO-PC-Windows-RC1** o de la release **pc-v1.0.0-rc1**.
6. Ejecuta el instalador. Se instala para tu usuario, con acceso directo. No necesitas instalar Python ni VLC por separado. Es x64; no incluye build ARM64 ni 32 bits. El instalador inicial no está firmado con certificado comercial de Windows y el sistema puede pedir confirmación.

No requiere secrets nuevos, SQL nuevo ni cambiar la TV. Para subir avatares se usa el bucket/SQL de RC18 que ya utiliza TV. QR de acceso y chat móvil reutilizan las páginas de pairing/party existentes.

**Los cuatro secrets de firma Android no se usan ni se alteran.** El workflow PC publica solo `pc-v1.0.0-rc1` y `pc-latest`; no toca `native-latest` ni el enlace del Downloader de TV.

## Primer acceso

Settings → inicia sesión con tu cuenta de BruniO TV o usa QR sign-in. La app lee perfiles, token TMDB y complementos privados de esa cuenta desde Supabase. Si no hay token/complementos, introdúcelos en Settings. La app no lleva tu Torrentio ni claves TMDB preinstalados para otros usuarios.

Los tokens de sesión y preferencias sensibles se conservan con Windows DPAPI en `%LOCALAPPDATA%\BruniO\account.dat`. Solo se pueden descifrar desde la misma cuenta Windows. No se guarda la contraseña. Sign out borra esta sesión local.

## PC ↔ TV Watch Party

**Usa dos cuentas distintas para host y guest.** El backend identifica al host por usuario, no por dispositivo; abrir la misma cuenta en PC y TV haría que ambos fueran host de su propia sala.

- **TV host → PC guest:** crea una Party en TV; en PC pulsa Friends / Party → Party → Request access y escribe los seis dígitos. El host acepta en TV. PC lee el contenido, resuelve sus propios enlaces y se sincroniza.
- **PC host → TV guest:** crea la Party desde PC (puede hacerse con el video abierto). Introduce su código en TV o solicita acceso desde Friends. Aparecerá una ventana de aprobación en PC. Acepta y reproduce cualquier película/episodio; TV seguirá el contenido de la sala.
- El guest puede pausar y reanudar. PC envía las mismas llamadas `miflix_request_playback` de TV; los hosts consumen `miflix_take_playback`. No usa un protocolo separado.
- Cambiar el título o episodio como host conserva la sala. Cada participante elige su propio Playback link; no se transmite ni comparte el URL privado del host.
- Cada ~1 segundo se consulta/sincroniza reproducción. Se corrigen diferencias superiores a 1.8 segundos durante reproducción; se evita buscar posiciones antiguas mientras hay una pausa local esperando confirmación.
- Leave Party libera inmediatamente el video local y la sala local. Las respuestas anteriores se invalidan; la limpieza del servidor se hace en segundo plano. Una desconexión puede impedir esa limpieza remota, pero no debe restaurar el reproductor abandonado.
- Chat, emojis y frases usan los mismos eventos. El QR de Party abre la web móvil de chat existente. Los avisos de Joined se muestran en la banda inferior; una solicitud de acceso abre una ventana modal por encima de la app.

## Funciones incluidas

- Home con tendencias, películas/series populares, top rated, now playing, continuar viendo y recomendaciones basadas en puntuaciones/favoritos.
- Carátulas pequeñas seleccionables, resaltado blanco, scroll y navegación con mouse/teclado.
- Search con historial de diez búsquedas por usuario/perfil y resultados flexibles mediante normalización y variantes de varias palabras.
- Colecciones por plataforma, películas y series, filas por géneros, años y próximos estrenos.
- Detalles, episodios por temporada, reproducción rápida y selección manual de enlaces, también dentro del reproductor.
- VLC integrado: play/pausa, barra de avance, ajustar proporción, speed, audio, subtítulos embebidos/archivo y OpenSubtitles EN/ES. F11 = pantalla completa; Space = play/pausa; Left/Right = ±10 segundos. Un cambio de enlace vuelve a la posición actual.
- Audio/sub preferidos, filtro de GB, orden calidad → tamaño, descarte TB Download y año de película. Un límite activo excluye tamaño desconocido. Los episodios no se descartan por el año inicial de la serie.
- IntroDB (botones Skip intro/credits cuando se han obtenido segmentos), autoplay siguiente episodio y recomendaciones al finalizar. La disponibilidad depende de que IntroDB tenga ese título.
- Pausa: hora, tiempo restante y sinopsis tras cinco segundos, con sinopsis de episodio cuando la consulta devuelve sus datos.
- Perfiles sincronizados: selección, creación, eliminación, galería completa de TV, quitar foto y subida directa desde PC. Fotos nuevas se recortan a cuadrado en JPEG; el avatar es público como en TV.
- Favoritos, progreso y puntuación 1–10 sincronizados por perfil. Selección aleatoria apoyada en recomendaciones y excluyendo títulos terminados o puntuados 4 o menos.
- Friends: presencia, preview de contenido, solicitudes/aceptación de amigos y acceso privado a Party. El listado se refresca manualmente; la presencia, solicitudes y chat siguen consultándose en segundo plano.
- Live TV / sports con categorías y búsqueda local conservada. Se puede configurar un manifest de deportes personalizado en Settings. La búsqueda filtra el catálogo cargado; esta RC aún no pagina todos los canales de proveedores grandes.
- Notificaciones de estrenos/cambios en favoritos, badge y botón Clear. Se toma una referencia inicial y se revisa cada 30 minutos (hasta 40 favoritos por revisión).
- Trakt: conexión con el PKCE ya desplegado, confirmación, importación de watchlist y registro watched al 80% de reproducción. No necesita Client Secret en PC.
- Check for updates: canal exclusivo PC, comparación numérica de versiones y verificación de hash/tamaño antes de abrir el instalador. En esta RC se descarga en segundo plano con aviso, todavía sin porcentaje/cancelación.

## Qué falta verificar

Es la **primera RC de PC**, no una certificación de que todos los detalles sean idénticos a TV. El diseño y algunos textos de controles son propios de escritorio; la traducción EN/ES todavía es parcial. Joined/reacciones son avisos en la banda inferior, no overlays animados sobre el video. El orden de Home y la selección aleatoria son más simples que las reglas de TV; el motor de reproducción es VLC, no Media3.

Las pruebas locales usan Qt real sin pantalla, fixtures de APIs y el esquema SQL local de TV. No se usaron cuentas reales ni se desplegó nada. La sincronización en una PC Windows y una TV real aún debe probarse; diferencias de buffering/códecs/red pueden requerir ajustes. Algunas cabeceras especiales de streams en vivo distintas de User-Agent/Referer no están implementadas en el motor VLC de esta RC.

La build Windows y la prueba de decodificación de VLC se ejecutan en Actions. Después prueba: instalar → login → reproducir película MKV y serie → cambiar audio/subtítulos/enlace → Party TV host / PC guest → pausar/reanudar ambos → invertir host → salir y reproducir fuera de la sala.

## Desarrollo local en Windows / VS Code

```powershell
cd desktop\brunio-pc
py -3.12 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
python -m unittest discover -s tests -v
```

Para ejecutar desde el código instala VLC x64 o usa la carpeta `vendor/vlc-3.0.23` descargada por el script de build. Para generar el instalador local ejecuta `./scripts/build_windows.ps1` desde esa carpeta (Inno Setup 6 debe estar instalado, o Chocolatey para instalarlo).

El proyecto se separa en core.py (API/Party), pairing.py (QR cifrado), vault.py (DPAPI), updater.py (updates PC), ui.py (Qt/reproductor), scripts (build/installer) y tests (contrato/GUI). `VERSION`, workflow, build_windows.ps1 e installer.iss deben cambiar juntos en nuevas versiones; conserva AppId del instalador.
