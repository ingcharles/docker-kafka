-- La copia local del ítem guarda solo los CÓDIGOS de sus impuestos, nunca las tarifas: la tarifa y su tipo (porcentaje
-- o específica) salen del catálogo tarifa_impuesto a la fecha de emisión, así un cambio de normativa no obliga a tocar
-- ítems. Reemplaza codigo_impuesto (siempre '2'), tiene_ice, codigo_porcentaje_ice y tarifa_ice, y agrega el IRBPNR.
alter table item_local rename column codigo_porcentaje to codigo_porcentaje_iva;
alter table item_local add column codigo_ice varchar(4);
alter table item_local add column codigo_irbpnr varchar(4);
update item_local set codigo_ice = codigo_porcentaje_ice where tiene_ice and codigo_porcentaje_ice is not null;
alter table item_local drop column codigo_impuesto;
alter table item_local drop column tiene_ice;
alter table item_local drop column codigo_porcentaje_ice;
alter table item_local drop column tarifa_ice;

comment on column item_local.codigo_porcentaje_iva is 'Código de porcentaje de IVA del ítem (Tabla 17, tabla tarifa_impuesto con impuesto 2).';
comment on column item_local.codigo_ice is 'Código de ICE del ítem (Tabla 18, tabla tarifa_impuesto con impuesto 3); vacío si el ítem no lleva ICE.';
comment on column item_local.codigo_irbpnr is 'Código de IRBPNR del ítem (5001, tabla tarifa_impuesto con impuesto 5); vacío si no lo lleva.';
