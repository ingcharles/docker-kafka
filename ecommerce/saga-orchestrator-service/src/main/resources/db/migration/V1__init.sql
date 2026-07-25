-- Corre contra el esquema "saga" (quarkus.flyway.default-schema=saga),
-- dentro de la base de datos compartida "ecommerce". Los nombres de tabla van sin calificar.

create table saga_instance (
    id                 uuid primary key,  -- == orderId, el id de correlación de la saga
    state              varchar(20) not null,
    customer_id        varchar(100) not null,
    total_amount       numeric(14, 2) not null,
    currency           varchar(3) not null,
    shipping_street    varchar(200) not null,
    shipping_city      varchar(100) not null,
    shipping_state     varchar(100),
    shipping_zip       varchar(20),
    shipping_country   varchar(100) not null,
    items_snapshot     text not null,     -- lista de ítems de OrderCreated, necesaria para armar ReserveStock/ReleaseStock
    failure_reason     varchar(300),
    created_at         timestamptz not null,
    updated_at         timestamptz not null
);

create index idx_saga_instance_state on saga_instance (state);

create table saga_step (
    id          uuid primary key,
    saga_id     uuid not null references saga_instance (id),
    step_name   varchar(100) not null,
    status      varchar(20) not null,  -- RECEIVED | SENT
    topic       varchar(150),          -- solo se llena en las filas SENT
    payload     text,                  -- solo se llena en las filas SENT; es lo que /retry vuelve a publicar
    created_at  timestamptz not null
);

create index idx_saga_step_saga_id on saga_step (saga_id, created_at);

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
