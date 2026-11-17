-- Datos del emisor que la ficha técnica del SRI exige en el comprobante cuando aplican:
--   Anexo 21: agente de retención (número de resolución, sin ceros a la izquierda, máximo 8 dígitos).
--   Anexo 22: régimen RIMPE (RIMPE o NEGOCIO_POPULAR).
-- Detalles adicionales de cada línea (<detAdicional nombre valor/>, hasta 3) y unidad de medida del ítem, que sale en la
-- línea como "Medida". Corre contra el esquema "comprobantes".
alter table comprobante add column empresa_agente_retencion varchar(8);
alter table comprobante add column empresa_regimen_rimpe varchar(20);
alter table comprobante add constraint chk_comprobante_regimen_rimpe
    check (empresa_regimen_rimpe is null or empresa_regimen_rimpe in ('RIMPE', 'NEGOCIO_POPULAR'));
alter table detalle_comprobante add column detalles_adicionales_json text;
alter table item_local add column unidad_medida varchar(30);

comment on column comprobante.empresa_agente_retencion is 'Anexo 21: número de la resolución con que el emisor fue designado agente de retención, sin ceros a la izquierda (máximo 8 dígitos); va en la etiqueta agenteRetencion del XML y en el RIDE. Vacío si no es agente de retención.';
comment on column comprobante.empresa_regimen_rimpe is 'Anexo 22: régimen RIMPE del emisor, RIMPE o NEGOCIO_POPULAR; su leyenda va en la etiqueta contribuyenteRimpe del XML y en el RIDE. Vacío si no es RIMPE.';
comment on column detalle_comprobante.detalles_adicionales_json is 'Detalles adicionales de la línea (hasta 3) como arreglo JSON de {nombre, valor}; salen como detAdicional en el XML y en la columna Detalles Adicionales del RIDE. Incluye "Medida" cuando el ítem tiene unidad de medida.';
comment on column item_local.unidad_medida is 'Unidad de medida del ítem en inventario-service (p. ej. UN); sale en los detalles adicionales de la línea como "Medida". Opcional.';
