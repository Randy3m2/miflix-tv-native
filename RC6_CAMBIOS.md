# BruniO PC RC6 — Primer screenshot

## Qué cambió
- Shell horizontal sustituido por un **sidebar vertical fijo**, con estado seleccionado y acceso a todas las pantallas que existían.
- Tema global midnight/glass: negro azulado, paneles transparentes, píldoras violeta y contraste mejorado.
- **Inicio:** hero cinematográfico con cinco títulos navegables, degradado oscuro, detalles, CTAs, servicios de streaming en carrusel, continuar viendo y demás estanterías.
- **Búsqueda:** campo central, búsqueda con debounce, resultados en pósteres, historial reciente y filtros funcionales de todo / películas / series / actores / directores / géneros. Los resultados de personas enlazan sus créditos TMDB.
- **Perfiles:** pantalla inmersiva sin sidebar, fondo espacial generado con Qt, avatares circulares y controles de administración.
- **Detalle:** fondo cinematográfico, controles de reproducción/fuentes/lista/rating/descarga; series con episodios horizontales y miniaturas.
- **Mi Lista:** cuadrícula y filtros independientes (Todo, Películas, Series, Próximamente).
- **Rediseño base de Qt:** bordes suaves, focos, botones de navegación integrados en estanterías.
- Versión y GitHub Actions para `1.0.0-rc6`; canal de release PC separado de Android.

## Alcance
No se modificaron los motores `core.py`, `desktop_features.py`, `downloads.py`, `playlist.py`, `updater.py` ni `vault.py`. Tampoco se modificaron archivos Android/TV. El catálogo usa la cuenta/configuración TMDB actual; no se han añadido fotografías ficticias.

## Validación y limitaciones
- Validación estática con `python -m compileall`: OK.
- No se pudieron ejecutar los tests PySide6 localmente: el entorno de preparación carece de PySide6 y no permite descargarlo.
- Actions ejecutará los tests existentes y dos nuevos antes del instalador. No se afirma que haya sido compilado o probado visualmente en Windows.
- El filtro Próximamente se basa en el año de estreno; los estrenos del año actual podrían no aparecer.
