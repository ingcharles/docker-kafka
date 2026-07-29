-- Réplica local de solo lectura de establecimientos/puntos de emisión reales
-- de firma-electronica-service — ver EstablecimientoPuntoEmisionEventConsumer.
-- Se usa para validar el alcance (establecimiento/puntoEmision) al que se
-- quiere acotar una api-key nueva (ver ApiKeyService.crear) — ya no
-- cualquier código de 3 dígitos es aceptado, tiene que existir un
-- establecimiento/punto de emisión activo con ese código para el ruc.
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
