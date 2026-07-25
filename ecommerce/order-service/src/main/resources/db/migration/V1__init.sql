-- Corre contra el esquema "orders" (quarkus.flyway.default-schema=orders),
-- dentro de la base de datos compartida "ecommerce". Los nombres de tabla
-- van sin calificar a propósito — ver application.properties para el porqué.

create table orders (
    id                 uuid primary key,
    customer_id        varchar(100) not null,
    status             varchar(20) not null,
    currency           varchar(3) not null,
    total_amount       numeric(14, 2) not null,
    shipping_street    varchar(200) not null,
    shipping_city      varchar(100) not null,
    shipping_state     varchar(100),
    shipping_zip       varchar(20),
    shipping_country   varchar(100) not null,
    created_at         timestamptz not null,
    updated_at         timestamptz not null
);

create index idx_orders_customer_id on orders (customer_id);
create index idx_orders_status on orders (status);

create table order_items (
    id            uuid primary key,
    order_id      uuid not null references orders (id) on delete cascade,
    product_id    uuid not null,
    product_name  varchar(200) not null,
    quantity      integer not null check (quantity > 0),
    unit_price    numeric(14, 2) not null check (unit_price > 0)
);

create index idx_order_items_order_id on order_items (order_id);

-- Outbox transaccional: se escribe en la misma transacción que el cambio
-- de orders/order_items que anuncia, y OutboxRelay lo retransmite a Kafka.
-- Ver infrastructure.outbox.
create table outbox_event (
    id            uuid primary key,
    aggregate_id  uuid not null,
    event_type    varchar(100) not null,
    topic         varchar(150) not null,
    payload       text not null,
    created_at    timestamptz not null,
    published_at  timestamptz
);

create index idx_outbox_event_unpublished on outbox_event (created_at) where published_at is null;

-- Inbox de idempotencia para los mensajes de Kafka consumidos. Ver infrastructure.inbox.
create table processed_message (
    message_id    uuid primary key,
    processed_at  timestamptz not null
);
