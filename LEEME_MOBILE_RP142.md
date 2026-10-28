# BruniO Mobile M6 / RP142

Versión 1.0.5-m6-rp142 / versionCode 22246. Parche de código sobre RP141, solo para generar la actualización móvil. No es un APK firmado.

Añade el ajuste que faltaba en la interfaz móvil:
Ajustes → Inicio y perfiles → Recordar último perfil.
Settings → Startup and profiles → Remember last profile.

Al tocarlo, cambia entre Activado y Desactivado. Activado abre Home con el último perfil elegido al iniciar BruniO; desactivado conserva el selector de perfiles al iniciar en modo normal. El modo fácil conserva su comportamiento existente.

Se reutiliza la persistencia de perfiles existente. En móvil, la elección de recordar el perfil también se conserva localmente para que restaurar los ajustes de otro perfil no cambie este comportamiento de inicio. No se cambia la selección actual, no se borran datos y no hay SQL ni secretos nuevos.

Copia los cuatro archivos de código/configuración respetando sus rutas al repositorio actualizado a RP141 y ejecuta **Build BruniO Mobile APK**. Instala el APK estable sobre la versión móvil anterior, conservando la misma firma y sin desinstalar. No es necesario generar un APK de TV; su versión y notas actuales siguen en RP141.

Pendiente de prueba en teléfono real: activar el ajuste, elegir otro perfil, cerrar completamente la app y abrirla otra vez; después desactivarlo y comprobar que reaparece el selector de perfiles.
