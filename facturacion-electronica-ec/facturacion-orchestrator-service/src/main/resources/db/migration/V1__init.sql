-- Corre contra el esquema "orquestacion" (quarkus.flyway.default-schema=orquestacion),
-- dentro de la base de datos compartida "facturacion". Los nombres de tabla van sin calificar.

create table saga_instance (
    id                 uuid primary key,  -- == comprobanteId, el id de correlación de la saga
    state              varchar(30) not null,
    emisor_id          uuid not null,
    clave_acceso       varchar(49) not null,
    ambiente           varchar(20) not null,
    failure_reason     varchar(500),
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
