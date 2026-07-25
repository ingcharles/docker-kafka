# payment-service

Responsable de los registros de pago de cada orden, contra una pasarela de pago **simulada** (no hay integración real con ningún proveedor). Quarkus 3.36.1, Java 21, pila reactiva, arquitectura hexagonal/DDD. Ver [`../README.md`](../README.md) para entender la saga completa en la que participa este servicio.

## Responsabilidad

Cobra y reembolsa cuando recibe el comando correspondiente desde la saga. No hay ninguna integración real con una pasarela de pagos (Stripe, PayPal, etc.) — en `PaymentProcessingService.chargePayment`, la decisión de aprobar o rechazar el cobro es completamente determinista: se rechaza cualquier cobro cuyo monto supere `payment.simulation.decline-threshold` (variable de entorno `PAYMENT_DECLINE_THRESHOLD`, con un valor por defecto de 5000). Esto se hizo así **a propósito**, para que el camino de compensación de la saga (qué pasa cuando un pago es rechazado) se pueda reproducir de forma confiable con una simple llamada a la API pública, sin depender de ningún servicio externo de terceros. El día que este proyecto deje de ser una implementación de referencia y necesite cobrar dinero de verdad, hay que reemplazar esa lógica por una llamada real a la pasarela de pago que se vaya a usar.

## Endpoints (ruta raíz `/payments/api/v1`)

Son de solo lectura — los pagos únicamente se crean o actualizan a través de comandos de Kafka, nunca por una llamada REST directa.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/{orderId}` | Consulta el pago asociado a una orden. |
| `GET` | `/?page=&limit=&status=` | Listado paginado, con filtro opcional por estado del pago. |

## Kafka

| Dirección | Tópico | Tipo(s) de mensaje | Key |
|---|---|---|---|
| Consume | `ecommerce.payment.commands.v1` | `ChargePayment`, `RefundPayment` | `orderId` |
| Produce | `ecommerce.payment.events.v1` | `PaymentApproved`, `PaymentDeclined`, `PaymentRefunded` | `orderId` |

**Confiabilidad del mensajeo:** outbox transaccional + inbox idempotente, el mismo patrón que usan todos los demás servicios — la explicación completa de por qué y cómo funciona está en el README de la raíz del proyecto.

## Base de datos (esquema `payment`)

Tablas: `payments`, `outbox_event`, `processed_message`. El SQL completo está en `src/main/resources/db/migration/V1__init.sql`.

## Cómo correrlo de forma individual (modo desarrollo)

```sh
mvn quarkus:dev
```

O, si prefieres construir la imagen Docker directamente: `docker build -t payment-service .`
