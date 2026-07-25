# shipping-service

Responsable de los registros de envío de cada orden, contra un transportista **simulado** (no hay integración real con ninguna empresa de logística). Quarkus 3.36.1, Java 21, pila reactiva, arquitectura hexagonal/DDD. Ver [`../README.md`](../README.md) para entender la saga completa en la que participa este servicio.

## Responsabilidad

Crea y cancela envíos cuando recibe el comando correspondiente desde la saga. No hay ninguna integración real con un transportista — en `ShipmentProcessingService`, la creación del envío falla de forma completamente determinista cuando `shippingAddress.city` es exactamente el valor `"FAIL"` (no distingue mayúsculas/minúsculas). Esto se hizo así **a propósito**, para que el camino de compensación de la saga (qué pasa cuando un envío no se puede crear) se pueda reproducir de forma confiable con una simple llamada a la API pública, sin depender de ningún transportista real. El día que este proyecto necesite generar envíos de verdad, hay que reemplazar esa lógica por una llamada real a la API del transportista que se vaya a usar.

## Endpoints (ruta raíz `/shipping/api/v1`)

Son de solo lectura — los envíos únicamente se crean o actualizan a través de comandos de Kafka, nunca por una llamada REST directa.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/{orderId}` | Consulta el envío asociado a una orden. |
| `GET` | `/?page=&limit=&status=` | Listado paginado, con filtro opcional por estado del envío. |

## Kafka

| Dirección | Tópico | Tipo(s) de mensaje | Key |
|---|---|---|---|
| Consume | `ecommerce.shipping.commands.v1` | `CreateShipment`, `CancelShipment` | `orderId` |
| Produce | `ecommerce.shipping.events.v1` | `ShipmentCreated`, `ShipmentFailed`, `ShipmentCancelled` | `orderId` |

**Nota sobre `CancelShipment`/`ShipmentCancelled`:** estos existen en el código por completitud de la API, pero **el diseño actual de la saga nunca los dispara realmente**. ¿Por qué? Porque todo camino de compensación que involucra el envío ocurre, o bien *antes* de que el envío llegue a crearse (cuando falla el stock o el pago), o bien *después* de que el envío ya falló por sí mismo (el propio evento `ShipmentFailed`). Nunca se da el caso de "el envío se creó exitosamente, pero ahora hay que cancelarlo" dentro del flujo que implementa este proyecto — así que estos dos mensajes quedan disponibles para el futuro, pero hoy no se usan.

**Confiabilidad del mensajeo:** outbox transaccional + inbox idempotente, el mismo patrón que usan todos los demás servicios — la explicación completa de por qué y cómo funciona está en el README de la raíz del proyecto.

## Base de datos (esquema `shipping`)

Tablas: `shipments`, `outbox_event`, `processed_message`. El SQL completo está en `src/main/resources/db/migration/V1__init.sql`.

## Cómo correrlo de forma individual (modo desarrollo)

```sh
mvn quarkus:dev
```

O, si prefieres construir la imagen Docker directamente: `docker build -t shipping-service .`
