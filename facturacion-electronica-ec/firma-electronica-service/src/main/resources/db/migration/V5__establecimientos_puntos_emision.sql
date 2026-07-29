-- Establecimientos y puntos de emisión reales por empresa — hasta ahora
-- `empresa.establecimiento_defecto`/`punto_emision_defecto` eran los únicos
-- valores conocidos, sin CRUD ni gobernanza (cualquier string llegaba tal
-- cual en cada request de creación de comprobante). Esto los vuelve
-- recursos de primera clase, con la misma jerarquía que el SRI exige
-- (empresa → establecimiento → punto de emisión).
create table establecimiento (
    id              uuid primary key,
    empresa_id      uuid not null references empresa (id),
    codigo          varchar(3) not null,
    direccion       varchar(300) not null,
    tipo            varchar(20) not null,
    activo          boolean not null default true,
    creado_en       timestamptz not null,
    actualizado_en  timestamptz not null,
    constraint chk_establecimiento_tipo check (tipo in ('MATRIZ', 'SUCURSAL')),
    constraint uq_establecimiento_codigo unique (empresa_id, codigo)
);

create index idx_establecimiento_empresa_id on establecimiento (empresa_id);

create table punto_emision (
    id                  uuid primary key,
    establecimiento_id  uuid not null references establecimiento (id),
    codigo              varchar(3) not null,
    descripcion         varchar(300),
    activo              boolean not null default true,
    creado_en           timestamptz not null,
    actualizado_en      timestamptz not null,
    constraint uq_punto_emision_codigo unique (establecimiento_id, codigo)
);

create index idx_punto_emision_establecimiento_id on punto_emision (establecimiento_id);
