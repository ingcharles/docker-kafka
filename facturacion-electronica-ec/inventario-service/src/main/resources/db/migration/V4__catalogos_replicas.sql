-- Réplica local de los catálogos de impuestos del SRI (Tablas 16 y 17). El
-- dueño es catalogos-service; este servicio la recibe por el tópico compactado
-- facturacion.catalogos.eventos.v1 (CatalogoEventConsumer). Mismas columnas
-- que en el dueño. Corre contra el esquema "inventario".
--
-- Sin FK entre las dos tablas: los eventos pueden llegar en cualquier orden.

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

-- El ítem ya no guarda la tarifa de IVA, solo codigo_impuesto +
-- codigo_porcentaje; la tarifa se resuelve contra tarifa_impuesto a la fecha
-- de uso. Así un cambio de tarifa del SRI se hace una sola vez en
-- catalogos-service y no ítem por ítem.
alter table item drop column tarifa_iva;
