# AutoControl - base Android

Proyecto inicial Kotlin + Jetpack Compose + Firebase Authentication + Cloud Firestore.

## Qué incluye
- Alta de cuenta e inicio de sesión por correo/contraseña.
- Alta y listado de clientes/vehículos.
- Datos guardados en Firestore y sincronizados en tiempo real entre teléfonos.
- Datos de kilometraje y fecha objetivo del próximo cambio de correa.
- Indicador de vencimiento por kilometraje y aviso visual cuando faltan 1.000 km.

## Para abrirlo
1. Instalá Android Studio (versión compatible con Android Gradle Plugin 8.9.2).
2. Creá un proyecto Firebase en https://console.firebase.google.com/.
3. Agregá una aplicación Android con el identificador `com.autocontrol.app`.
4. Descargá `google-services.json` y copialo dentro de la carpeta `app/`.
5. En Firebase Authentication, habilitá el proveedor Correo/contraseña.
6. Creá una base Firestore y publicá las reglas de `firestore.rules`.
7. Reemplazá `TALLER_CONFIGURAR_ID` en `VehicleRepository.kt` por un identificador privado del taller.
8. Abrí el proyecto en Android Studio, sincronizá Gradle y ejecutá en un dispositivo.

## Uso en dos teléfonos
Creá una cuenta y usá exactamente el mismo correo y contraseña en ambos teléfonos. Ambos leerán y escribirán en la misma base del taller.

## Importante antes de producción
- Este es un punto de partida, no un APK terminado ni probado en un dispositivo.
- Las reglas incluidas solo exigen autenticación: para producción hay que implementar membresía/invitaciones por taller y restringir cada workspace.
- La app todavía no programa notificaciones locales. El siguiente paso es agregar WorkManager para avisos por fecha y una actualización de kilometraje para disparar alertas por km.
- Los recordatorios por kilometraje dependen de que alguien actualice el odómetro cuando el auto vuelve al taller o el cliente informa el kilometraje.
- No guardes datos reales de clientes hasta endurecer las reglas de seguridad.
