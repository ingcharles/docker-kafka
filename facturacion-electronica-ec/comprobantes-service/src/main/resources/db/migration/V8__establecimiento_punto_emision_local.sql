-- Réplica local de solo lectura de establecimientos/puntos de emisión reales
-- de firma-electronica-service — ver EstablecimientoPuntoEmisionEventConsumer.
-- Indexadas por clave natural (ruc, codigo[, establecimientoCodigo]), no por
-- ningún id interno de firma-electronica-service (mismo principio "Empresa
-- sin ID" que ya sigue item_local).
create table establecimiento_local (
    id              uuid primary key,
    ruc             varchar(13) not null,
    codigo          varchar(3) not null,
    activo          boolean not null,
    actualizado_en  timestamptz not null,
    constraint uq_establecimiento_local_ruc_codigo unique (ruc, codigo)
);

create table punto_emision_local (
    id                      uuid primary key,
    ruc                     varchar(13) not null,
    establecimiento_codigo  varchar(3) not null,
    codigo                  varchar(3) not null,
    activo                  boolean not null,
    actualizado_en          timestamptz not null,
    constraint uq_punto_emision_local_ruc_estab_codigo unique (ruc, establecimiento_codigo, codigo)
);
