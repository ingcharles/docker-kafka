# order-service

Responsable del agregado `Order` (la orden de compra) — es el **punto de entrada de toda la saga**: cuando este servicio crea una orden, dispara el flujo completo que termina en pago, envío y notificación. Construido con Quarkus 3.36.1, Java 21, pila reactiva (Mutiny + Hibernate Reactive Panache), y arquitectura hexagonal/DDD. Ver [`../README.md`](../README.md) para entender la saga completa en la que participa este servicio.

## Responsabilidad

Crea órdenes y publica el evento `OrderCreated`, que es justamente lo que arranca la saga en `saga-orchestrator-service`. **Nunca cambia su propio estado a través de la API REST** — eso solo ocurre cuando llega un comando `ConfirmOrder` o `CancelOrder` desde el orquestador, vía Kafka. Esta es una decisión de diseño intencional: si permitiéramos, por ejemplo, un `PUT /orders/{id}` que cambie el estado directamente, se rompería la garantía de que el orquestador es la única fuente de verdad sobre en qué punto de la saga está cada orden.

## Endpoints (ruta raíz `/orders/api/v1`)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/` | Crea una orden (con sus ítems y la dirección de envío). Internamente, guarda la orden y publica el evento `OrderCreated` — ver la sección de Kafka más abajo para entender cómo se garantiza que ambas cosas ocurran juntas o ninguna. |
| `GET` | `/{id}` | Consulta una orden por su id. |
| `GET` | `/?page=&limit=&customerId=&status=` | Listado paginado, con filtros opcionales por cliente y/o estado. |

Notarás que **no existen** `PUT`, `PATCH` ni `DELETE`. Esto no es una omisión: el estado de una orden solo puede cambiar a través de los comandos de Kafka que envía el orquestador (`ConfirmOrder`/`CancelOrder`), nunca por una llamada REST directa.

## Kafka

| Dirección | Tópico | Tipo(s) de mensaje | Key |
|---|---|---|---|
| Produce | `ecommerce.order.events.v1` | `OrderCreated` | `orderId` |
| Consume | `ecommerce.order.commands.v1` | `ConfirmOrder`, `CancelOrder` | `orderId` |

**Sobre la forma de los mensajes**: revisa las clases `application/event/OrderEventMessage.java` y `OrderCommandMessage.java`. En vez de crear una clase Java distinta por cada tipo de evento (una para `OrderCreated`, otra para un hipotético `OrderUpdated`, etc.), se usa **una sola clase por tópico**, con un campo `type`/`commandType` que indica de qué tipo de mensaje se trata. La razón de esta decisión está explicada con más detalle en la sección "Diseño de Kafka" del README raíz del proyecto.

**Confiabilidad del mensajeo:**
- **Outbox transaccional** (`outbox_event`): cuando se crea una orden, el evento `OrderCreated` no se manda directo a Kafka — se guarda primero en la tabla `outbox_event`, dentro de la misma transacción SQL que crea la orden. Un job programado (la clase `OutboxRelay`, que corre cada segundo) es quien realmente publica esos eventos pendientes en Kafka. Así se evita que, por ejemplo, la orden quede guardada en la base de datos pero el evento nunca llegue a publicarse (o viceversa).
- **Inbox idempotente** (`processed_message`): cuando llega un comando `ConfirmOrder` o `CancelOrder`, antes de aplicarlo se revisa si su `eventId` ya fue procesado antes. Si ya existe en la tabla `processed_message`, se descarta (evita aplicar el mismo comando dos veces si Kafka lo reenvía por alguna falla de red). Si no existe, se aplica el cambio y se registra el `eventId`, todo en la misma transacción.

## Base de datos (esquema `orders`)

Tablas: `orders`, `order_items`, `outbox_event`, `processed_message`. El SQL completo (creación de tablas, índices, restricciones) está en `src/main/resources/db/migration/V1__init.sql`, y se ejecuta automáticamente al arrancar el servicio (vía Flyway).

**Nota sobre el nombre del esquema:** se llama `orders` (en plural), no `order`, porque `order` es una palabra reservada del lenguaje SQL (se usa en `ORDER BY`) y usarla como nombre de esquema obligaría a poner comillas en cada consulta SQL que la mencione — mejor evitar ese dolor de cabeza desde el diseño.

## Cómo correrlo de forma individual (modo desarrollo)

Necesita tener accesibles tanto Postgres (esquema `orders`, con el rol `order_service` ya creado) como el broker de Kafka — la forma más simple de tenerlos disponibles es levantando `../db` y `../../kafka`. Luego:

```sh
mvn quarkus:dev
```

O, si prefieres construir la imagen Docker directamente sin pasar por modo desarrollo: `docker build -t order-service .`
