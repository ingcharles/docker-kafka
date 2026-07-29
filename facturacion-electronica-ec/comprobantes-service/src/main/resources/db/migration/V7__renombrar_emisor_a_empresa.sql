-- Alinea el vocabulario con identidad-service/firma-electronica-service
-- (ver V4__renombrar_emisor_a_empresa.sql de firma-electronica-service).
-- Solo columnas planas (snapshot inline del emisor/empresa en cada
-- comprobante) — no hay FK ni índice sobre estas columnas.

alter table comprobante rename column emisor_ruc to empresa_ruc;
alter table comprobante rename column emisor_razon_social to empresa_razon_social;
alter table comprobante rename column emisor_nombre_comercial to empresa_nombre_comercial;
alter table comprobante rename column emisor_direccion_matriz to empresa_direccion_matriz;
alter table comprobante rename column emisor_obligado_contabilidad to empresa_obligado_contabilidad;
alter table comprobante rename column emisor_contribuyente_especial to empresa_contribuyente_especial;
alter table comprobante rename column emisor_numero_contribuyente_especial to empresa_numero_contribuyente_especial;
alter table comprobante rename column emisor_es_gran_contribuyente to empresa_es_gran_contribuyente;
alter table comprobante rename column emisor_numero_resolucion_gran_contribuyente to empresa_numero_resolucion_gran_contribuyente;
