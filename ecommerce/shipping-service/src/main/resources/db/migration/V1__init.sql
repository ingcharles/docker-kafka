-- Corre contra el esquema "shipping" (quarkus.flyway.default-schema=shipping),
-- dentro de la base de datos compartida "ecommerce". Los nombres de tabla van sin calificar.

create table shipments (
    id                uuid primary key,
    order_id          uuid not null unique,
    address_street    varchar(200) not null,
    address_city      varchar(100) not null,
    address_state     varchar(100),
    address_zip       varchar(20),
    address_country   varchar(100) not null,
    status            varchar(20) not null,
    tracking_number   varchar(100),
    failure_reason    varchar(300),
    created_at        timestamptz not null,
    updated_at        timestamptz not null
);

create index idx_shipments_status on shipments (status);

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
