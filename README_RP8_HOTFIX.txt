BruniO TV — RP8 hotfix del control de límites
Aplicar sobre RP8 sustituyendo Screens.kt en ambas rutas.
La llamada al control de límites y su implementación privada quedan en el mismo
Screens.kt. Ya no depende de AddonLimitControlRP7.kt para ese control.
Si existe el archivo anterior de RP7, puede quedarse: no hay nombres duplicados.
Conserva los ajustes de RP8, incluyendo la excepción de tamaño de Latam y Nuve+.
No cambia versión ni requiere SQL/Vercel.
Validación estática del parser Kotlin respecto a RP8 y comprobación de llamada
y declaración. No se ejecutó build Android ni se generó APK.
