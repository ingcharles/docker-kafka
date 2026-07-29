-- Ruc de la empresa dueña del comprobante — hasta ahora este servicio no lo
-- conocía en absoluto. Se necesita para poder aplicar overrides por empresa
-- de configuracion_empresa (URL del SRI, política de reintentos — ver
-- ConfiguracionEmpresaLocal). Nullable: las filas ya existentes (datos de
-- prueba) quedan sin ruc, sin overrides aplicables para ellas — no afecta
-- su comportamiento, que ya estaba fijado.
alter table envio_sri add column empresa_ruc varchar(13);
