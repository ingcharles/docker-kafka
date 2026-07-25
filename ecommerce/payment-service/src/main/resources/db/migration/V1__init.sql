-- Corre contra el esquema "payment" (quarkus.flyway.default-schema=payment),
-- dentro de la base de datos compartida "ecommerce". Los nombres de tabla van sin calificar.

create table payments (
    id                 uuid primary key,
    order_id           uuid not null unique,
    amount             numeric(14, 2) not null check (amount > 0),
    currency           varchar(3) not null,
    status             varchar(20) not null,
    gateway_reference  varchar(100),
    decline_reason     varchar(300),
    created_at         timestamptz not null,
    updated_at         timestamptz not null
);

create index idx_payments_status on payments (status);

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
