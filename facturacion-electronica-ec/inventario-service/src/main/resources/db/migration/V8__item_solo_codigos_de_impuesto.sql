-- El ítem guarda solo los CÓDIGOS de sus impuestos, nunca las tarifas: la tarifa y su tipo (porcentaje o específica)
-- salen del catálogo de comprobantes-service (tabla tarifa_impuesto) a la fecha de emisión, así un cambio de normativa
-- (IVA, Tabla 18 del ICE, IRBPNR) no obliga a tocar ítems. Reemplaza codigo_impuesto (siempre '2'), tarifa_iva,
-- tiene_ice, codigo_porcentaje_ice y tarifa_ice, y agrega el IRBPNR.
alter table item rename column codigo_porcentaje to codigo_porcentaje_iva;
alter table item add column codigo_ice varchar(4);
alter table item add column codigo_irbpnr varchar(4);
update item set codigo_ice = codigo_porcentaje_ice where tiene_ice and codigo_porcentaje_ice is not null;
alter table item drop column codigo_impuesto;
alter table item drop column if exists tarifa_iva;
alter table item drop column tiene_ice;
alter table item drop column codigo_porcentaje_ice;
alter table item drop column tarifa_ice;

comment on column item.codigo_porcentaje_iva is 'Código de porcentaje de IVA del ítem (Tabla 17; p. ej. 4 = 15 %). La tarifa sale del catálogo al emitir.';
comment on column item.codigo_ice is 'Código de ICE del ítem (Tabla 18); vacío si no lleva ICE. La tarifa y su tipo salen del catálogo al emitir.';
comment on column item.codigo_irbpnr is 'Código de IRBPNR del ítem (5001); vacío si no lo lleva.';
