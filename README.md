# AutoControl

Aplicación Android para gestión básica de taller mecánico.

## Funciones de esta primera versión
- Alta de clientes y vehículos.
- Registro de patente, marca/modelo y kilometraje actual.
- Control del kilometraje objetivo del próximo cambio de correa.
- Alertas visuales: al día, próximo (1.000 km o menos) y vencido.
- Actualización del kilometraje desde la ficha del vehículo.
- Persistencia local en el teléfono.

## Obtener el APK
El workflow **Build AutoControl APK** compila automáticamente una versión de prueba cuando se actualiza la rama main. En GitHub: **Actions → Build AutoControl APK → último workflow → Artifacts → AutoControl-debug**.

## Importante
Esta primera versión guarda los datos localmente en el dispositivo. El inicio de sesión y la sincronización entre teléfonos mediante Firebase quedan pendientes de configurar con las credenciales del proyecto. No uses todavía datos reales de clientes en una versión de prueba.
