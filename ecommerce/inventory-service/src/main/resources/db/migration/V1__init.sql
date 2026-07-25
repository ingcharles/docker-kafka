-- Corre contra el esquema "inventory" (quarkus.flyway.default-schema=inventory),
-- dentro de la base de datos compartida "ecommerce". Los nombres de tabla van sin calificar.

create table products (
    id                  uuid primary key,
    sku                 varchar(50) not null unique,
    name                varchar(200) not null,
    unit_price          numeric(14, 2) not null check (unit_price > 0),
    available_quantity  integer not null check (available_quantity >= 0),
    reserved_quantity   integer not null default 0 check (reserved_quantity >= 0),
    created_at          timestamptz not null,
    updated_at          timestamptz not null
);

create table stock_reservations (
    id           uuid primary key,
    order_id     uuid not null,
    product_id   uuid not null references products (id),
    quantity     integer not null check (quantity > 0),
    status       varchar(20) not null,
    created_at   timestamptz not null
);

create index idx_stock_reservations_order_id on stock_reservations (order_id);
create index idx_stock_reservations_product_id on stock_reservations (product_id);

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

create table processed_message (
    message_id    uuid primary key,
    processed_at  timestamptz not null
);
