-- Corre contra el esquema "comprobantes" (quarkus.flyway.default-schema=comprobantes),
-- dentro de la base de datos compartida "facturacion". Los nombres de tabla van sin calificar.

-- Copia local de solo lectura de los datos de Emisor que este servicio
-- necesita para construir un comprobante nuevo — se mantiene al día
-- consumiendo facturacion.emisor.eventos.v1 (ver EmisorEventConsumer). No es
-- la fuente de verdad (esa es firma-electronica-service); es la vista
-- materializada que evita depender de una llamada REST síncrona entre
-- servicios (la única excepción documentada del stack, ver ARQUITECTURA.md).
create table emisor_local (
    id                                      uuid primary key,
    ruc                                     varchar(13) not null,
    razon_social                            varchar(300) not null,
    nombre_comercial                        varchar(300),
    direccion_matriz                        varchar(300) not null,
    obligado_contabilidad                   boolean not null,
    contribuyente_especial                  boolean not null,
    numero_contribuyente_especial           varchar(20),
    es_gran_contribuyente                   boolean not null default false,
    numero_resolucion_gran_contribuyente    varchar(50),
    vende_combustibles                      boolean not null,
    ambiente                                varchar(20) not null,
    establecimiento_defecto                 varchar(3) not null,
    punto_emision_defecto                   varchar(3) not null,
    activo                                  boolean not null,
    actualizado_en                          timestamptz not null
);

-- Contador de secuencial por emisor+tipo de comprobante+establecimiento+punto
-- de emisión — el SRI exige que sea consecutivo y sin huecos dentro de esa
-- combinación (Tabla 1 de la ficha técnica, campo "N° del comprobante").
create table secuencial_comprobante (
    id                  uuid primary key,
    emisor_id           uuid not null,
    tipo_comprobante    varchar(30) not null,
    establecimiento     varchar(3) not null,
    punto_emision       varchar(3) not null,
    ultimo_secuencial   bigint not null default 0,
    constraint uq_secuencial_comprobante unique (emisor_id, tipo_comprobante, establecimiento, punto_emision)
);

-- El agregado Comprobante. Guarda el XML en cada una de sus tres etapas
-- (generado -> firmado -> autorizado) y el RIDE en PDF una vez autorizado —
-- así el historial completo de cada documento queda auditable sin tener
-- que volver a pedirle nada a otro servicio ni regenerar nada.
create table comprobante (
    id                                      uuid primary key,
    tipo_comprobante                        varchar(30) not null,
    clave_acceso                            varchar(49) not null unique,
    ambiente                                varchar(20) not null,
    establecimiento                         varchar(3) not null,
    punto_emision                           varchar(3) not null,
    secuencial                              varchar(9) not null,

    -- Snapshot del emisor al momento de emitir — deliberadamente NO es una
    -- referencia viva a emisor_local: si el emisor cambia su razón social
    -- mañana, los comprobantes ya emitidos no deben cambiar retroactivamente.
    emisor_id                               uuid not null,
    emisor_ruc                              varchar(13) not null,
    emisor_razon_social                     varchar(300) not null,
    emisor_nombre_comercial                 varchar(300),
    emisor_direccion_matriz                 varchar(300) not null,
    emisor_obligado_contabilidad            boolean not null,
    emisor_contribuyente_especial           boolean not null,
    emisor_numero_contribuyente_especial    varchar(20),
    emisor_es_gran_contribuyente            boolean not null default false,
    emisor_numero_resolucion_gran_contribuyente varchar(50),

    fecha_emision                           timestamptz not null,
    comprador_tipo_identificacion           varchar(2) not null,
    comprador_identificacion                varchar(20) not null,
    comprador_razon_social                  varchar(300) not null,
    comprador_direccion                     varchar(300),

    subtotal_sin_impuestos                  numeric(14, 2) not null,
    total_descuento                         numeric(14, 2) not null default 0,
    importe_total                           numeric(14, 2) not null,
    moneda                                  varchar(10) not null default 'DOLAR',

    estado                                  varchar(20) not null,

    xml_generado                            text not null,
    xml_firmado                             text,
    xml_autorizado                          text,
    numero_autorizacion                     varchar(49),
    fecha_autorizacion                      timestamptz,
    motivo_rechazo                          varchar(500),
    ride_pdf                                bytea,

    creado_en                               timestamptz not null,
    actualizado_en                          timestamptz not null,

    constraint chk_comprobante_tipo check (tipo_comprobante in
        ('FACTURA', 'NOTA_CREDITO', 'NOTA_DEBITO', 'GUIA_REMISION', 'RETENCION', 'LIQUIDACION_COMPRA')),
    constraint chk_comprobante_estado check (estado in
        ('BORRADOR', 'FIRMADO', 'FIRMA_FALLIDA', 'AUTORIZADO', 'NO_AUTORIZADO', 'NOTIFICADO', 'ANULADO'))
);

create index idx_comprobante_emisor_id on comprobante (emisor_id);
create index idx_comprobante_estado on comprobante (estado);
create index idx_comprobante_clave_acceso on comprobante (clave_acceso);

-- Líneas de detalle. "impuestos_json" guarda el arreglo de impuestos de esa
-- línea (código, código de porcentaje, tarifa, base imponible, valor) como
-- JSON — no se normaliza en su propia tabla porque nunca se consulta de
-- forma independiente del detalle al que pertenece.
create table detalle_comprobante (
    id                          uuid primary key,
    comprobante_id              uuid not null references comprobante (id) on delete cascade,
    item_id                     uuid,
    codigo_principal             varchar(50) not null,
    codigo_auxiliar             varchar(50),
    descripcion                 varchar(300) not null,
    cantidad                    numeric(14, 4) not null check (cantidad > 0),
    precio_unitario             numeric(14, 4) not null check (precio_unitario > 0),
    descuento                   numeric(14, 2) not null default 0,
    precio_total_sin_impuesto   numeric(14, 2) not null,
    impuestos_json              text not null
);

create index idx_detalle_comprobante_comprobante_id on detalle_comprobante (comprobante_id);

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
