-- Corre contra el esquema "firma_electronica" (quarkus.flyway.default-schema=firma_electronica),
-- dentro de la base de datos compartida "facturacion". Los nombres de tabla van sin calificar.

create table emisor (
    id                              uuid primary key,
    ruc                             varchar(13) not null unique,
    razon_social                    varchar(300) not null,
    nombre_comercial                varchar(300),
    direccion_matriz                varchar(300) not null,
    obligado_contabilidad           boolean not null default false,
    contribuyente_especial          boolean not null default false,
    numero_contribuyente_especial   varchar(20),
    vende_combustibles              boolean not null default false,
    ambiente                        varchar(20) not null,
    establecimiento_defecto         varchar(3) not null default '001',
    punto_emision_defecto           varchar(3) not null default '001',
    activo                          boolean not null default true,
    creado_en                       timestamptz not null,
    actualizado_en                  timestamptz not null,
    constraint chk_emisor_ambiente check (ambiente in ('PRUEBAS', 'PRODUCCION')),
    constraint chk_emisor_numero_contribuyente_especial
        check (contribuyente_especial = false or numero_contribuyente_especial is not null)
);

create index idx_emisor_ruc on emisor (ruc);

-- Un emisor puede tener varios certificados a lo largo del tiempo (renovación tras
-- expiración), pero solo uno ACTIVO a la vez — de ahí el índice único parcial.
-- El contenido del .p12 y su contraseña nunca se guardan en claro: ver
-- infrastructure.crypto.CifradoService (AES-256-GCM con clave maestra de configuración).
create table certificado_digital (
    id                              uuid primary key,
    emisor_id                       uuid not null references emisor (id),
    alias                           varchar(200),
    huella_digital                  varchar(200) not null,
    contenido_cifrado               bytea not null,
    contrasena_cifrada              bytea not null,
    fecha_emision_certificado       timestamptz not null,
    fecha_expiracion_certificado    timestamptz not null,
    estado                          varchar(20) not null,
    creado_en                       timestamptz not null,
    constraint chk_certificado_estado check (estado in ('ACTIVO', 'EXPIRADO', 'REVOCADO'))
);

create index idx_certificado_emisor_id on certificado_digital (emisor_id);
create unique index idx_certificado_activo_unico on certificado_digital (emisor_id) where estado = 'ACTIVO';

-- Outbox transaccional: se escribe en la misma transacción que el cambio de
-- emisor/certificado que anuncia, y OutboxRelay lo retransmite a Kafka.
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

-- Inbox de idempotencia para los comandos de firma consumidos desde Kafka.
create table processed_message (
    message_id    uuid primary key,
    processed_at  timestamptz not null
);
