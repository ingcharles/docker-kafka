-- Precio unitario sin subsidio de la línea (etiqueta precioSinSubsidio del XSD de factura y liquidación de compra).
-- Corre contra el esquema "comprobantes".
alter table detalle_comprobante add column precio_sin_subsidio numeric(18, 6);

comment on column detalle_comprobante.precio_sin_subsidio is 'Precio unitario sin subsidio (precioSinSubsidio del XML), en dólares con hasta 6 decimales; solo cuando corresponde, p. ej. combustibles subsidiados. Vacío si no aplica.';
