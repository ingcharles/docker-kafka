-- Alinea el vocabulario con identidad-service, que ya usa "empresa" para el
-- mismo concepto (POST /usuarios/me/empresas). Ningún dato cambia, solo
-- nombres de tabla/columna/constraint/índice. Nombres reales verificados
-- contra la base ya corriendo antes de escribir este script (pg_constraint /
-- pg_indexes), no asumidos.

alter table emisor rename to empresa;
alter table empresa rename constraint emisor_pkey to empresa_pkey;
alter table empresa rename constraint emisor_ruc_key to empresa_ruc_key;
alter table empresa rename constraint chk_emisor_ambiente to chk_empresa_ambiente;
alter table empresa rename constraint chk_emisor_numero_contribuyente_especial to chk_empresa_numero_contribuyente_especial;
alter table empresa rename constraint chk_emisor_numero_resolucion_gran_contribuyente to chk_empresa_numero_resolucion_gran_contribuyente;
alter index idx_emisor_ruc rename to idx_empresa_ruc;

alter table certificado_digital rename column emisor_id to empresa_id;
alter table certificado_digital rename constraint certificado_digital_emisor_id_fkey to certificado_digital_empresa_id_fkey;
alter index idx_certificado_emisor_id rename to idx_certificado_empresa_id;

alter table configuracion_emisor rename to configuracion_empresa;
alter table configuracion_empresa rename column emisor_id to empresa_id;
alter table configuracion_empresa rename constraint configuracion_emisor_pkey to configuracion_empresa_pkey;
alter table configuracion_empresa rename constraint configuracion_emisor_emisor_id_fkey to configuracion_empresa_empresa_id_fkey;
alter table configuracion_empresa rename constraint uq_configuracion_emisor_clave to uq_configuracion_empresa_clave;
alter index idx_configuracion_emisor_emisor_id rename to idx_configuracion_empresa_empresa_id;
