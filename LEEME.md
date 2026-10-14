BruniO Web · Mi lista y Amigos

1. En Supabase SQL Editor ejecuta supabase_web_favorites.sql.
2. Copia supabase/functions/brunio-catalog/index.ts y la sección brunio-catalog de supabase/config.toml al repositorio. Despliega:
   supabase functions deploy brunio-catalog --no-verify-jwt
   La función verifica la sesión del usuario internamente. Usa el secreto TMDB_TOKEN existente; no pongas tokens privados en la web. Supabase entrega SUPABASE_URL y SUPABASE_ANON_KEY al runtime.
3. Aplica los archivos Android del hotfix RC45 y genera el APK en GitHub Actions. Instálalo en las TVs que usarán la lista compartida. Versiones anteriores pueden sobrescribir favoritos web al guardar su estado completo.
4. Abre la web, entra con el correo/contraseña de BruniO y elige el perfil. Guarda un título y abre Mi lista en TV (o pulsa Actualizar lista). Comprueba también quitarlo.

Amigos muestra tus amigos, estado/actividad, recomendaciones y solicitudes. Las solicitudes se gestionan desde TV. No abre salas ni reproduce video. La actividad proviene de la TV y se consulta al entrar o pulsar Actualizar; no hay polling. Usa las tablas/RPC sociales existentes, con sus políticas RLS.

Hosting Vercel: sube el contenido de web/ como proyecto estático (sin comando build). No necesita secretos en Vercel. La clave publishable de Supabase es pública; las políticas RLS y la sesión protegen los datos. Para cambiar proyecto edita BASE y KEY en app.js. La sesión se conserva en sessionStorage por pestaña; no se guarda la contraseña.

Pruebas locales: sintaxis JavaScript y pruebas de la función de catálogo con HTTP simulado. No se compiló el APK (no hay Android SDK/Gradle aquí) ni se probó con credenciales reales. Ejecuta el workflow Android y la comprobación de sincronización después de configurar Supabase.
