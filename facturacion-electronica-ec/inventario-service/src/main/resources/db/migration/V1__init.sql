-- Corre contra el esquema "inventario" (quarkus.flyway.default-schema=inventario),
-- dentro de la base de datos compartida "facturacion". Los nombres de tabla van sin calificar.

create table categoria (
    id           uuid primary key,
    nombre       varchar(200) not null,
    tipo         varchar(20) not null,
    descripcion  varchar(500),
    activa       boolean not null default true,
    creado_en    timestamptz not null,
    constraint chk_categoria_tipo check (tipo in ('PRODUCTO', 'SERVICIO', 'AMBOS'))
);

-- Catálogo unificado: un ítem es un producto o un servicio (columna "tipo").
-- Las columnas de existencias (cantidad_disponible/reservada/stock_minimo)
-- solo se usan cuando tipo = 'PRODUCTO'; en un servicio quedan en cero y no
-- participan del kardex ni de las reservas — un servicio no se agota.
create table item (
    id                    uuid primary key,
    tipo                  varchar(20) not null,
    categoria_id          uuid references categoria (id),
    codigo_principal      varchar(50) not null,
    codigo_auxiliar       varchar(50),
    descripcion           varchar(300) not null,
    precio_unitario       numeric(14, 4) not null check (precio_unitario > 0),
    unidad_medida         varchar(30),
    cantidad_disponible   numeric(14, 4) not null default 0 check (cantidad_disponible >= 0),
    cantidad_reservada    numeric(14, 4) not null default 0 check (cantidad_reservada >= 0),
    stock_minimo          numeric(14, 4) not null default 0,
    activo                boolean not null default true,
    creado_en             timestamptz not null,
    actualizado_en        timestamptz not null,
    constraint chk_item_tipo check (tipo in ('PRODUCTO', 'SERVICIO'))
);

create index idx_item_codigo_principal on item (codigo_principal);
create index idx_item_categoria_id on item (categoria_id);
create index idx_item_tipo on item (tipo);

-- El contenido de la imagen se guarda directamente en Postgres (bytea).
-- Ver infrastructure.rest.ImagenResource para la nota de arquitectura sobre
-- esta decisión y cómo migrar a object storage (MinIO/S3) si hace falta.
create table imagen_item (
    id               uuid primary key,
    item_id          uuid not null references item (id) on delete cascade,
    nombre_archivo   varchar(300) not null,
    tipo_mime        varchar(100) not null,
    tamano_bytes     bigint not null,
    contenido        bytea not null,
    es_principal     boolean not null default false,
    orden            integer not null default 0,
    creado_en        timestamptz not null
);

create index idx_imagen_item_item_id on imagen_item (item_id);
-- Como máximo una imagen "principal" por ítem.
create unique index idx_imagen_item_principal_unica on imagen_item (item_id) where es_principal = true;

-- Kardex de costeo por promedio ponderado — append-only, cada fila enlaza
-- con el saldo que dejó la anterior. Ver domain.service.KardexCalculador.
create table movimiento_kardex (
    id                     uuid primary key,
    item_id                uuid not null references item (id),
    tipo_movimiento        varchar(20) not null,
    cantidad               numeric(14, 4) not null check (cantidad > 0),
    costo_unitario         numeric(14, 4) not null,
    cantidad_saldo         numeric(14, 4) not null,
    costo_promedio_saldo   numeric(14, 4) not null,
    valor_saldo            numeric(14, 4) not null,
    referencia_documento   varchar(200),
    motivo                 varchar(300),
    creado_en              timestamptz not null,
    constraint chk_kardex_tipo_movimiento
        check (tipo_movimiento in ('ENTRADA', 'SALIDA', 'AJUSTE_POSITIVO', 'AJUSTE_NEGATIVO'))
);

create index idx_kardex_item_id_creado_en on movimiento_kardex (item_id, creado_en);

-- Reserva temporal de stock mientras un comprobante se firma y se autoriza
-- ante el SRI. Solo al confirmarse (estado CONFIRMADO) se escribe un
-- movimiento de kardex — si se libera (comprobante rechazado/cancelado),
-- el stock vuelve a estar disponible sin dejar rastro contable.
create table reserva_stock (
    id               uuid primary key,
    comprobante_id   uuid not null,
    item_id          uuid not null references item (id),
    cantidad         numeric(14, 4) not null check (cantidad > 0),
    estado           varchar(20) not null,
    creado_en        timestamptz not null,
    constraint chk_reserva_estado check (estado in ('RESERVADO', 'CONFIRMADO', 'LIBERADO'))
);

create index idx_reserva_comprobante_id on reserva_stock (comprobante_id);
create index idx_reserva_item_id on reserva_stock (item_id);

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
