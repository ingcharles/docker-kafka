# notification-service

Responsable del registro de notificaciones enviadas a los clientes, contra una pasarela de email/SMS **simulada** (no hay integración real con ningún proveedor de correo o mensajería). Quarkus 3.36.1, Java 21, pila reactiva, arquitectura hexagonal/DDD. Ver [`../README.md`](../README.md) para entender la saga completa en la que participa este servicio.

## Responsabilidad

Envía notificaciones (simuladas) cuando recibe el comando correspondiente desde la saga. En `NotificationSendingService`, el envío falla de forma determinista cuando el destinatario es exactamente `fail@example.com` — y como la saga arma ese destinatario a partir de `<customerId>@example.com`, basta con crear una orden con `customerId: "fail"` para disparar este camino de prueba.

**El detalle más importante de este servicio, y el único de toda la saga que no es obvio a primera vista:** un fallo de notificación **nunca** dispara una compensación. Para cuando este comando se ejecuta, la orden ya fue pagada y ya fue despachada — no queda nada que deshacer solo porque el correo de confirmación no se pudo mandar. La saga igual llega a `COMPLETED` (revisa el método `onNotificationSettled` en `saga-orchestrator-service` para ver exactamente cómo se maneja esto).

A diferencia de las otras dos integraciones simuladas (`payment-service` y `shipping-service`), aquí **no se usa la anotación `@Retry`** para reintentar el envío. La razón es que el fallo en este servicio es determinista (depende únicamente del valor del destinatario) — reintentar exactamente la misma llamada simulada solo produciría el mismo fallo tres veces más, sin ningún beneficio. En una integración real con un proveedor de correo/SMS de verdad, ahí sí tendría sentido usar `@Retry`, pero para fallos genuinamente transitorios (un timeout de red, un error 503 del proveedor) — no para un fallo que siempre va a ocurrir de la misma forma.

## Endpoints (ruta raíz `/notifications/api/v1`)

Son de solo lectura — las notificaciones únicamente se crean a través del comando de Kafka `SendNotification`, nunca por una llamada REST directa.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/{orderId}` | Lista las notificaciones enviadas para una orden. |
| `GET` | `/?page=&limit=` | Listado paginado, con las más recientes primero. |

## Kafka

| Dirección | Tópico | Tipo(s) de mensaje | Key |
|---|---|---|---|
| Consume | `ecommerce.notification.commands.v1` | `SendNotification` | `orderId` |
| Produce | `ecommerce.notification.events.v1` | `NotificationSent`, `NotificationFailed` | `orderId` |

**Confiabilidad del mensajeo:** outbox transaccional + inbox idempotente, el mismo patrón que usan todos los demás servicios — la explicación completa de por qué y cómo funciona está en el README de la raíz del proyecto.

## Base de datos (esquema `notification`)

Tablas: `notifications`, `outbox_event`, `processed_message`. El SQL completo está en `src/main/resources/db/migration/V1__init.sql`.

## Cómo correrlo de forma individual (modo desarrollo)

```sh
mvn quarkus:dev
```

O, si prefieres construir la imagen Docker directamente: `docker build -t notification-service .`
