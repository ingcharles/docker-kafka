-- "vende_combustibles" no es un dato del SRI y ningún servicio lo leía. Lo que la ficha técnica exige en una venta de
-- combustibles es la placa (Anexo 12) y los códigos del combustible en la línea (Anexo 16), que manda quien emite la factura.
-- Corre contra el esquema "firma_electronica".
alter table empresa drop column vende_combustibles;
