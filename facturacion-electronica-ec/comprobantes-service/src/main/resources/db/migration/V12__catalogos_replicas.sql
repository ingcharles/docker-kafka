-- Réplicas locales de los catálogos del SRI. El dueño de estos datos es
-- catalogos-service (esquema "catalogos"); este servicio solo los guarda de
-- forma de solo lectura, alimentados por el tópico compactado
-- facturacion.catalogos.eventos.v1 (CatalogoEventConsumer), igual que
-- item_local / establecimiento_local. Mismas columnas que en el dueño.
-- Corre contra el esquema "comprobantes".
--
-- Sin FKs entre estas tablas a propósito: los eventos pueden llegar en
-- cualquier orden y cada fila se aplica por su cuenta (upsert por clave).

-- forma_pago ya existe desde V11 (con sus 8 filas de la Tabla 24 como
-- arranque, para que haya catálogo antes del primer evento). Se le agregan
-- las columnas que faltaban para ser réplica fiel.
alter table forma_pago add column activo boolean not null default true;
alter table forma_pago add column actualizado_en timestamptz not null default now();

create table impuesto (
    codigo         varchar(2) primary key,
    descripcion    varchar(100) not null,
    activo         boolean not null default true,
    actualizado_en timestamptz not null default now()
);

create table tarifa_impuesto (
    impuesto_codigo   varchar(2) not null,
    codigo_porcentaje varchar(4) not null,
    descripcion       varchar(100) not null,
    tarifa            numeric(7, 2) not null,
    vigente_desde     date not null default date '1900-01-01',
    vigente_hasta     date,
    activo            boolean not null default true,
    actualizado_en    timestamptz not null default now(),
    primary key (impuesto_codigo, codigo_porcentaje)
);

create table tipo_identificacion (
    codigo         varchar(2) primary key,
    descripcion    varchar(100) not null,
    solo_factura   boolean not null default false,
    activo         boolean not null default true,
    actualizado_en timestamptz not null default now()
);

create table impuesto_retencion (
    codigo         varchar(2) primary key,
    descripcion    varchar(100) not null,
    activo         boolean not null default true,
    actualizado_en timestamptz not null default now()
);

create table retencion (
    impuesto_codigo varchar(2) not null,
    codigo          varchar(5) not null,
    vigente_desde   date not null default date '1900-01-01',
    descripcion     varchar(150) not null,
    porcentaje      numeric(5, 2) not null,
    vigente_hasta   date,
    activo          boolean not null default true,
    actualizado_en  timestamptz not null default now(),
    primary key (impuesto_codigo, codigo, vigente_desde)
);

create table sustento_tributario (
    codigo         varchar(2) primary key,
    descripcion    varchar(300) not null,
    activo         boolean not null default true,
    actualizado_en timestamptz not null default now()
);

-- La tarifa de IVA ya no se guarda en cada ítem: el ítem lleva solo
-- codigo_impuesto + codigo_porcentaje y la tarifa se resuelve contra
-- tarifa_impuesto (vigente a la fecha de emisión). Así un cambio de tarifa
-- del SRI se hace una vez, en catalogos-service, y no ítem por ítem.
alter table item_local drop column tarifa_iva;
