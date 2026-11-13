-- Corre contra el esquema "inventario".
-- La réplica de impuestos/tarifas (V4) ya no se usa: el ítem solo guarda codigo_impuesto + codigo_porcentaje y es
-- comprobantes-service —dueño de los catálogos del SRI y quien calcula el impuesto— quien los valida al emitir.
drop table if exists tarifa_impuesto;
drop table if exists impuesto;
