-- Copia local de solo lectura de qué rucs tienen un Emisor real en
-- firma-electronica-service — alimentada consumiendo
-- facturacion.emisor.eventos.v1 (ver EmisorEventConsumer). Se usa para
-- validar POST /usuarios/me/empresas: ya no cualquier string de 13 dígitos
-- es aceptado como ruc, tiene que existir un Emisor activo con ese ruc.
create table emisor_local (
    id                  uuid primary key,
    ruc                 varchar(13) not null unique,
    razon_social        varchar(300) not null,
    activo              boolean not null,
    actualizado_en      timestamptz not null
);
