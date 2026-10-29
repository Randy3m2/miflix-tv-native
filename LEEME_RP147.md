# BruniO TV RP147 · Naruto con temporadas originales

Aplicar los 4 archivos después de RP146, respetando sus rutas. Generar APK con build-native-tv-apk.yml. TV 2.0.0-rp147 / 22251. No APK firmado incluido.

Naruto (TMDB 46260) y Naruto Shippuden (31910) dejan de usar el override de lista absoluta. Se cargan por su temporada original, sin descargar ni validar toda la serie antes de mostrar episodios. Los add-ons reciben las coordenadas originales de temporada y episodio. El selector de temporadas, siguiente episodio y episodio aleatorio vuelven a sus rutas normales existentes.

Dragon Ball Z (12971) conserva exactamente su override de 291 episodios y su resolución S1/E absoluto. No se cambian sus validaciones, progreso ni lógica de enlaces.

La captura muestra un error de validación interno de BruniO: Anime episode numbering contains gaps. Este parche elimina esa validación para los dos Naruto restaurando el formato original. No modifica TMDB ni garantiza disponibilidad de enlaces del proveedor.

Los overrides previos conservaban season/episode canónicos en las filas; no se borran datos ni se requiere migración del progreso. Cerrar/reabrir la app después de instalar y entrar a Naruto/Shippuden para cargar la ficha nueva.

Pruebas cubren rutas exactas por temporada para ambos Naruto, respuesta con numeración no consecutiva, ausencia de consultas adicionales al catálogo completo y conservación de las validaciones de DBZ. Falta verificar con los proveedores y la TV reales.
