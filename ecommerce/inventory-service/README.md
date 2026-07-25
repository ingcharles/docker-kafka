# inventory-service

Responsable del catálogo de productos y de las reservas de stock. Quarkus 3.36.1, Java 21, pila reactiva, arquitectura hexagonal/DDD. Ver [`../README.md`](../README.md) para entender la saga completa en la que participa este servicio.

## Responsabilidad

Reserva y libera stock cuando recibe el comando correspondiente desde la saga. La reserva de una orden es **todo o nada** a través de todos sus ítems: si al intentar reservar el segundo producto de una orden resulta que no hay stock suficiente, el servicio **deshace automáticamente** la reserva que ya había hecho del primer producto antes de reportar el fallo — nunca se queda una orden con solo parte de sus productos reservados. Esta lógica está en `StockReservationService.tryReserveAll`, y vale la pena leerla directamente porque es el corazón de la corrección de este servicio: sin este cuidado, un fallo a mitad de una reserva multi-producto dejaría inventario "fantasma" reservado sin ningún motivo.

## Endpoints (ruta raíz `/inventory/api/v1`)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/products` | Crea un producto junto con su stock inicial (operación administrativa, para poblar el catálogo). |
| `GET` | `/products/{id}` | Consulta un producto, incluyendo cuánta cantidad está disponible y cuánta está actualmente reservada. |
| `GET` | `/products?page=&limit=` | Listado paginado del catálogo completo. |
| `GET` | `/products/{id}/reservations` | Endpoint de observabilidad: muestra cada fila de reserva (en estado `RESERVED` o `RELEASED`) que ha tocado este producto — útil para depurar o auditar qué le pasó al stock de un producto específico. |

## Kafka

| Dirección | Tópico | Tipo(s) de mensaje | Key |
|---|---|---|---|
| Consume | `ecommerce.inventory.commands.v1` | `ReserveStock`, `ReleaseStock` | `orderId` |
| Produce | `ecommerce.inventory.events.v1` | `StockReserved`, `StockReservationFailed`, `StockReleased` | `orderId` |

**Cómo se mueve el stock de forma segura ante concurrencia:** cada movimiento de stock se hace con un único `UPDATE ... WHERE available_quantity >= :cantidad` condicional (ver `ProductRepository.tryReserve`). Esto es importante: si dos órdenes distintas intentan reservar el último producto disponible al mismo tiempo, esta forma de escribir la consulta hace que la base de datos misma resuelva la condición de carrera — solo una de las dos actualizaciones tendrá éxito (porque la condición `available_quantity >= :cantidad` dejará de cumplirse para la segunda), sin necesidad de ningún bloqueo (`lock`) manual a nivel de aplicación.

**Confiabilidad del mensajeo:** outbox transaccional + inbox idempotente, el mismo patrón que usan todos los demás servicios — la explicación completa de por qué y cómo funciona está en el README de la raíz del proyecto.

## Base de datos (esquema `inventory`)

Tablas: `products`, `stock_reservations`, `outbox_event`, `processed_message`. El SQL completo está en `src/main/resources/db/migration/V1__init.sql`.

## Cómo correrlo de forma individual (modo desarrollo)

```sh
mvn quarkus:dev
```

O, si prefieres construir la imagen Docker directamente: `docker build -t inventory-service .`
