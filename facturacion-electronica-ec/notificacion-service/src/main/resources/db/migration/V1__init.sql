-- Corre contra el esquema "notificacion" (quarkus.flyway.default-schema=notificacion),
-- dentro de la base de datos compartida "facturacion". Los nombres de tabla van
-- sin calificar.

-- Un registro por intento de notificación — no duplica el Comprobante
-- (nunca se necesita el detalle/totales acá), solo lo necesario para el
-- envío y para no reenviar el mismo comprobante dos veces.
create table notificacion_enviada (
    id                  uuid primary key,
    comprobante_id      uuid not null,
    clave_acceso        varchar(49) not null,
    destinatario        varchar(300) not null,
    estado              varchar(20) not null,
    motivo_error        varchar(500),
    enviado_en          timestamptz not null,

    constraint chk_notificacion_estado check (estado in ('ENVIADA', 'FALLIDA'))
);

create unique index uq_notificacion_comprobante_id on notificacion_enviada (comprobante_id);

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
