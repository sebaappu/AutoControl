# Mecánica Cachi — plan comercial inicial

## Estado real del producto (27/11/2026)

Versión Android de prueba 0.1. La compilación #18 de GitHub Actions terminó correctamente y generó el APK `AutoControl-debug`.

Funciones verificadas en la documentación del proyecto:
- Alta de clientes y vehículos.
- Registro de patente, marca/modelo y kilometraje actual.
- Control del kilometraje objetivo del próximo cambio de correa.
- Estados visuales: al día, próximo (1.000 km o menos) y vencido.
- Actualización del kilometraje desde la ficha del vehículo.
- Persistencia local en el teléfono.
- El proyecto incluye recordatorios de mantenimiento programados y permisos de notificaciones; probarlos en teléfonos reales antes de ofrecerlos como garantía comercial.

**Limitaciones que deben comunicarse:** todavía no hay inicio de sesión ni sincronización de datos entre teléfonos configurada. La información queda guardada localmente en cada dispositivo. No vender como sistema completo de taller ni prometer catálogo exhaustivo de vehículos, presupuestos, stock o facturación hasta que esas funciones estén implementadas y probadas. Esta compilación es debug, no una versión de distribución firmada.

## Publicación sugerida para Facebook Marketplace

**Título:** Mecánica Cachi – App Android para organizar clientes y vehículos del taller

**Precio de publicación de prueba:** Gratis para participar del piloto de 7 días. No cobrar por adelantado durante esta etapa.

**Descripción:**

¿Tenés taller mecánico o trabajás por tu cuenta? Probá Mecánica Cachi, una aplicación Android pensada para ayudarte a llevar un control básico de tus clientes y vehículos desde el celular.

Incluye:
- Registro de clientes y vehículos.
- Patente, marca/modelo y kilometraje.
- Seguimiento del kilometraje objetivo para el próximo cambio de correa.
- Avisos visuales para identificar mantenimientos al día, próximos o vencidos.
- Actualización rápida del kilometraje.

📱 Para Android.  
🧪 Prueba piloto de 7 días con acompañamiento para instalarla y recibir comentarios.

Escribime “QUIERO PROBAR” y te cuento cómo participar.

**Importante:** versión inicial en prueba. Los datos se guardan en el teléfono y todavía no se sincronizan entre dispositivos. No reemplaza manuales ni especificaciones oficiales del fabricante.

## Mensaje para contactar talleres

Hola, ¿cómo estás? Estoy desarrollando Mecánica Cachi, una app Android para ayudar a talleres y mecánicos a ordenar clientes, vehículos, kilometraje y próximos mantenimientos. Estoy buscando talleres para probarla durante 7 días sin costo y escuchar qué funciones les servirían de verdad. ¿Te interesaría probarla?

## Guion para video demostrativo (30–40 segundos)

1. Mostrar el ícono y abrir Mecánica Cachi.
2. Dar de alta un cliente y un vehículo de demostración (usar datos ficticios).
3. Mostrar patente, marca/modelo y kilometraje.
4. Mostrar el control de kilometraje objetivo y los estados visuales.
5. Cerrar con el texto: “Mecánica Cachi — tu taller, más organizado. Sumate a la prueba piloto”.

No mostrar datos reales de clientes, patentes reales ni información personal.

## Plan de lanzamiento

1. Instalar la versión de prueba en dos teléfonos Android y comprobar guardado, cierre/reapertura, reinicio, permisos y notificaciones.
2. Probar con 3 a 5 mecánicos conocidos durante 7 días, usando datos ficticios o con consentimiento.
3. Registrar fallos y pedidos; priorizar estabilidad, copias de seguridad y sincronización antes de ampliar la venta.
4. Recién después del piloto, evaluar precios con entrevistas a talleres. Los importes se deben validar con usuarios y costos reales; no se promete demanda ni ventas automáticas.
5. Preparar una versión release firmada, política de privacidad, términos, canal de soporte y procedimiento de actualización antes de cobrar masivamente.

## Hipótesis de precios para validar después del piloto

- Piloto: 7 días sin cargo.
- Plan Inicial: probar un precio de lanzamiento de ARS 8.000/mes.
- Plan Taller: probar ARS 15.000–20.000/mes cuando existan funciones adicionales comprobadas.

Son hipótesis comerciales, no tarifas definitivas ni precios de mercado verificados. No activar cobros hasta contar con una versión estable, condiciones claras y mecanismo de baja.

## Lista previa a cobrar

- [ ] Prueba funcional en varios modelos Android.
- [ ] Verificar que las alertas funcionen con permisos concedidos y denegados.
- [ ] Copia de seguridad y recuperación de datos.
- [ ] Sincronización multi-dispositivo probada, si se ofrece.
- [ ] APK release firmado y proceso seguro de actualizaciones.
- [ ] Política de privacidad y consentimiento para datos de clientes.
- [ ] Soporte, términos, precio final y cancelación.
- [ ] Capturas y video grabados con datos ficticios.
