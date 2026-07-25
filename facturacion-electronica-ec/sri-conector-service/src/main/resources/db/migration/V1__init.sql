-- Corre contra el esquema "sri" (quarkus.flyway.default-schema=sri), dentro
-- de la base de datos compartida "facturacion". Los nombres de tabla van
-- sin calificar.

-- Un registro por intento de envío/autorización — el agregado de este
-- servicio. No duplica el Comprobante de comprobantes-service (no guarda
-- comprador, detalle, totales): solo lo que necesita para hablarle al SRI
-- y recordar en qué quedó.
create table envio_sri (
    id                      uuid primary key,
    comprobante_id          uuid not null,
    clave_acceso            varchar(49) not null unique,
    ambiente                varchar(20) not null,
    xml_firmado             text not null,

    estado                  varchar(20) not null,
    mensajes_recepcion      text,

    intentos_autorizacion   integer not null default 0,
    ultima_consulta_en      timestamptz,

    -- El bloque <autorizacion> completo (estado/numeroAutorizacion/fechaAutorizacion/
    -- ambiente/comprobante) tal como lo devuelve el WSDL de Autorización del
    -- SRI — no una copia de xml_firmado, ver EstadoComprobante.md en
    -- comprobantes-service para el porqué.
    xml_autorizado          text,
    numero_autorizacion     varchar(49),
    fecha_autorizacion      timestamptz,
    motivo_rechazo          varchar(1000),

    creado_en               timestamptz not null,
    actualizado_en          timestamptz not null,

    constraint chk_envio_sri_estado check (estado in
        ('PENDIENTE_ENVIO', 'RECHAZADO_RECEPCION', 'RECIBIDO', 'AUTORIZADO', 'NO_AUTORIZADO', 'ERROR_CONSULTA'))
);

create index idx_envio_sri_comprobante_id on envio_sri (comprobante_id);
create index idx_envio_sri_estado on envio_sri (estado);

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
