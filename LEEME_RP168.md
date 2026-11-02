# BruniO RP168

## Remote móvil

- Rediseño del control en grupos: navegación, reproducción/volumen y contenido.
- Botones con iconos, jerarquía visual y estados más claros.
- **Escanear TVs cercanas** busca únicamente hosts privados en la subred Wi‑Fi local y sólo acepta la respuesta identificable de BruniO en el puerto local del remoto.
- La conexión sigue usando el código de seis dígitos mostrado en la TV; la IP manual permanece disponible.
- No se transmiten credenciales ni se usa Supabase para descubrir o controlar la TV.

## Android TV Home

Se revisó la integración existente: BruniO publica canales Preview separados de Continue Watching, My List y Recommendations, con IDs estables, deep links y limpieza por perfil. Esto es compatible con launchers Android TV que consuman `TvProvider` (incluidos launchers de terceros que muestran canales como Nuvio/Stremio cuando los habilitan). La visibilidad final la controla el launcher; se puede pedir desde Settings → Android TV Home.

El launcher stock de Fire TV queda fuera de esta ruta porque no expone los canales Android TV estándar; requeriría la integración de catálogo propia de Amazon.

## Validación

RP168 conserva el contrato del remoto RP167 y aumenta la versión a `22281`. No se ensambló APK firmado en este entorno. Probar en el teléfono y TV reales: escaneo en la misma red, código expirado, subredes aisladas y visibilidad de canales del launcher.
