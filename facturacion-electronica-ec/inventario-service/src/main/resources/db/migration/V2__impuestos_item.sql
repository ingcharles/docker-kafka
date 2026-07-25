-- Clasificación tributaria del ítem — hasta ahora inventario-service no
-- rastreaba qué impuesto (IVA/ICE) aplica a cada ítem, así que
-- comprobantes-service no tenía forma de tomarlo automáticamente y
-- dependía por completo de que el llamador de la API mandara el impuesto a
-- mano en cada línea. Con esto, inventario-service pasa a ser la fuente de
-- verdad de la tarifa tributaria del ítem (igual que ya lo es del precio),
-- y la publica vía facturacion.inventario.item.eventos.v1 para que
-- comprobantes-service la tome sola cuando el llamador solo manda itemId
-- (ver ComprobanteService.construirDetalle en comprobantes-service).
--
-- Los defaults (IVA 15%, código de porcentaje 4 — Tabla 17 de la ficha
-- técnica del SRI) solo son para no dejar en null las 4 filas ya
-- existentes; toda fila nueva los recibe explícitos desde CrearItemRequest.
alter table item
    add column codigo_impuesto          varchar(2)   not null default '2',   -- Tabla 16: 2=IVA, 3=ICE, 5=IRBPNR
    add column codigo_porcentaje        varchar(2)   not null default '4',   -- Tabla 17: 0,2,3,4,5,6,7,8
    add column tarifa_iva               numeric(5,2) not null default 15.00,
    add column tiene_ice                boolean      not null default false,
    add column codigo_porcentaje_ice    varchar(4),                          -- Tabla 19, solo si tiene_ice
    add column tarifa_ice               numeric(5,2);
