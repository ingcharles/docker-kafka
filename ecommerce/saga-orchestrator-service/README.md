# saga-orchestrator-service

Es el **único servicio de los seis que conoce la saga completa, de principio a fin**. Los otros cinco solo saben hacer su propia tarea (reservar stock, cobrar, etc.) y responder si les salió bien o mal — es este servicio el que decide qué significa cada respuesta y qué toca hacer después. Quarkus 3.36.1, Java 21, pila reactiva, arquitectura hexagonal/DDD. Ver [`../README.md`](../README.md) para la tabla completa de flujo/compensaciones y el diseño de Kafka que aquí se implementa.

## Responsabilidad

Consume los cinco tópicos de eventos de respuesta (`*.events.v1`), decide qué debe pasar a continuación, y produce tanto los cinco tópicos de comandos (`*.commands.v1`) como el tópico de auditoría `ecommerce.saga.events.v1`. Todos los manejadores dentro de la clase `SagaOrchestratorService` siguen exactamente la misma estructura, vale la pena entenderla porque se repite una y otra vez en el código:

1. **Revisar el inbox** — ¿ya procesé este `eventId` antes? Si sí, no hacer nada (evita aplicar el mismo efecto dos veces si Kafka reentrega el mensaje).
2. **Aplicar la transición de estado** — si la saga no está en el estado de origen esperado (por ejemplo, llegó un duplicado y la saga ya avanzó), la transición simplemente no hace nada (es un *no-op* seguro).
3. **Emitir lo que corresponda a continuación** — el siguiente comando del camino feliz, o los comandos de compensación si algo falló.
4. **Marcar el inbox** — registrar que este `eventId` ya fue procesado.

### Máquina de estados

```
STARTED --StockReserved--> STOCK_RESERVED --PaymentApproved--> PAYMENT_APPROVED --ShipmentCreated--> SHIPMENT_CREATED --NotificationSent/Failed--> COMPLETED
   |                              |                                    |
   +-StockReservationFailed->  FAILED  <-PaymentDeclined----------------+
                                  ^
                                  +-ShipmentFailed-----------------------+
```

**Cómo leer este diagrama:** el flujo normal (la fila de arriba) avanza de izquierda a derecha, un estado a la vez, cada uno disparado por el evento de éxito del paso anterior. Los tres fallos posibles (`StockReservationFailed`, `PaymentDeclined`, `ShipmentFailed`) convergen todos en el mismo estado terminal `FAILED`, sin importar en qué punto del camino haya ocurrido el fallo. Nota que `NotificationSent` y su contraparte de fallo `NotificationFailed` llevan **ambas** a `COMPLETED` — esa es la única excepción de todo el diseño (ver el README de `notification-service` para el porqué).

`FAILED` y `COMPLETED` son estados terminales — de ahí no se puede volver a transicionar a ningún otro estado. La regla de qué transiciones son válidas no vive en una clase de dominio aparte: se aplica directamente en `SagaInstanceRepository.updateState(orderId, to, failureReason, allowedFrom...)`, con un `UPDATE ... WHERE state IN (...)` atómico a nivel de SQL — cada método `on*` de `SagaOrchestratorService` (por ejemplo `onStockReserved`, `onPaymentDeclined`) indica explícitamente desde qué estado(s) es válido llegar al que está aplicando. La tabla de qué comandos de compensación dispara cada fallo está en el README de la raíz del proyecto.

## Endpoints (ruta raíz `/sagas/api/v1`)

Esta es una superficie de **observabilidad y operación** — nada aquí modifica el estado de una saga directamente (eso siempre ocurre por Kafka), con la única excepción del endpoint de recuperación de abajo.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/{orderId}` | Devuelve el estado actual de la saga más su línea de tiempo completa (cada evento recibido, cada comando enviado, en orden). Es el primer lugar donde mirar cuando algo no salió como se esperaba. |
| `GET` | `/?page=&limit=&state=` | Listado paginado de sagas, con filtro opcional por estado. |
| `POST` | `/{orderId}/retry` | Vuelve a publicar el comando enviado más recientemente para esa saga — **con exactamente el mismo contenido, incluyendo el mismo `eventId` original** — para que sea el mecanismo de idempotencia del servicio destino el que decida si esto es un reintento genuino (el mensaje se perdió y nunca llegó) o si no hace falta nada (el comando sí se aplicó, solo se perdió la respuesta). Se usa para "destrabar" una saga que quedó a medias después de una caída de algún servicio. Devuelve `202` si se aceptó el reintento, o `404` si esa orden no tiene ninguna saga asociada. |

## Kafka

| Dirección | Tópico | Tipo(s) de mensaje consumido/producido | Key |
|---|---|---|---|
| Consume | `ecommerce.order.events.v1` | `OrderCreated` | `orderId` |
| Produce | `ecommerce.order.commands.v1` | `ConfirmOrder`, `CancelOrder` | `orderId` |
| Consume | `ecommerce.inventory.events.v1` | `StockReserved`, `StockReservationFailed`, `StockReleased` | `orderId` |
| Produce | `ecommerce.inventory.commands.v1` | `ReserveStock`, `ReleaseStock` | `orderId` |
| Consume | `ecommerce.payment.events.v1` | `PaymentApproved`, `PaymentDeclined`, `PaymentRefunded` | `orderId` |
| Produce | `ecommerce.payment.commands.v1` | `ChargePayment`, `RefundPayment` | `orderId` |
| Consume | `ecommerce.shipping.events.v1` | `ShipmentCreated`, `ShipmentFailed`, `ShipmentCancelled` | `orderId` |
| Produce | `ecommerce.shipping.commands.v1` | `CreateShipment`, `CancelShipment` | `orderId` |
| Consume | `ecommerce.notification.events.v1` | `NotificationSent`, `NotificationFailed` | `orderId` |
| Produce | `ecommerce.notification.commands.v1` | `SendNotification` | `orderId` |
| Produce | `ecommerce.saga.events.v1` | rastro de auditoría (equivalente a `SagaStarted`/`SagaCompleted`/`SagaFailed`), hoy sin ningún consumidor | `orderId` |

**Esta es la única pieza estructuralmente distinta a los otros cinco servicios**: su relay del outbox (clase `infrastructure/messaging/producer/SagaCommandRelay.java`) enruta cada mensaje pendiente hacia **uno de seis tópicos distintos**, según lo que diga la columna `outbox_event.topic` de esa fila — a diferencia de los demás servicios, donde el relay siempre publica al mismo tópico fijo. Revisa el Javadoc de esa clase para el detalle de implementación.

**Confiabilidad del mensajeo:** outbox transaccional + inbox idempotente, el mismo patrón que usan todos los demás servicios. Aquí además existe una tabla adicional que ningún otro servicio tiene: `saga_step`, que funciona como bitácora de auditoría — una fila por cada evento recibido *y* una fila por cada comando enviado. Es justamente de esta tabla de donde el endpoint `/retry` lee cuál fue el último comando enviado para poder reenviarlo.

## Base de datos (esquema `saga`)

- **`saga_instance`** — una fila por cada orden (su `id` es directamente el `orderId`): guarda el estado actual, el id del cliente, el monto total y la moneda, la dirección de envío, y `items_snapshot` (la lista de ítems del evento `OrderCreated`, guardada tal cual como JSON). ¿Por qué guardar una copia de los ítems aquí, si `order-service` ya los tiene? Porque los comandos `ReserveStock`/`ReleaseStock` necesitan la lista de productos y cantidades, y guardar esta copia evita tener que hacerle una llamada de vuelta a `order-service` para pedírsela — algo que, además, rompería la regla de "nada de REST entre microservicios" que sigue todo este proyecto.
- **`saga_step`** — la línea de tiempo de la saga. Las filas de tipo `SENT` (comando enviado) también guardan `topic` y `payload`, que es justamente lo que reutiliza el endpoint `/retry`.
- **`outbox_event`, `processed_message`** — mismo formato que en todos los demás servicios.

El SQL completo está en `src/main/resources/db/migration/V1__init.sql`.

## Cómo correrlo de forma individual (modo desarrollo)

```sh
mvn quarkus:dev
```

O, si prefieres construir la imagen Docker directamente: `docker build -t saga-orchestrator-service .`
