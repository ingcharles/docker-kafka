-- Corre contra el esquema "comprobantes".
--
-- 1) idx_comprobante_clave_acceso duplicaba el índice que ya crea la restricción UNIQUE de clave_acceso
--    (comprobante_clave_acceso_key): doble costo de escritura en cada emisión sin ninguna ventaja de lectura.
drop index if exists idx_comprobante_clave_acceso;

-- 2) El listado paginado (GET /comprobantes) filtra por empresa_ruc —el aislamiento multi-tenant— y por estado, y
--    empresa_ruc no tenía índice: cada listado recorría toda la tabla (que además guarda XML y PDF por fila).
create index idx_comprobante_empresa_ruc_estado on comprobante (empresa_ruc, estado);
