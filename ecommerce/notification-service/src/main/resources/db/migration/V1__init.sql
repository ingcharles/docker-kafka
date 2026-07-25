-- Corre contra el esquema "notification" (quarkus.flyway.default-schema=notification),
-- dentro de la base de datos compartida "ecommerce". Los nombres de tabla van sin calificar.

create table notifications (
    id              uuid primary key,
    order_id        uuid not null,
    type            varchar(50) not null,
    recipient       varchar(200) not null,
    status          varchar(20) not null,
    failure_reason  varchar(300),
    created_at      timestamptz not null
);

create index idx_notifications_order_id on notifications (order_id);

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
