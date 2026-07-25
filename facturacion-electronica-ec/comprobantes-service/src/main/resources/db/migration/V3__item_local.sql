-- Copia local de solo lectura del catálogo de inventario-service — mismo
-- patrón que emisor_local (ver V1__init.sql): se mantiene al día
-- consumiendo facturacion.inventario.item.eventos.v1, nunca se escribe
-- desde este servicio por ningún otro camino. Permite que una línea de
-- detalle que solo trae "itemId" (sin impuestos explícitos) derive
-- automáticamente el IVA/ICE que le corresponde — ver
-- ComprobanteService.construirDetalle.
create table item_local (
    id                      uuid primary key,
    codigo_principal        varchar(50) not null,
    descripcion             varchar(300) not null,
    codigo_impuesto         varchar(2) not null,
    codigo_porcentaje       varchar(2) not null,
    tarifa_iva              numeric(5, 2) not null,
    tiene_ice               boolean not null default false,
    codigo_porcentaje_ice   varchar(4),
    tarifa_ice              numeric(5, 2),
    activo                  boolean not null default true,
    actualizado_en          timestamptz not null
);
