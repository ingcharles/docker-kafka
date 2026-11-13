-- Corre contra el esquema "firma_electronica".
-- idx_empresa_ruc duplicaba el índice que ya crea la restricción UNIQUE de ruc (empresa_ruc_key): doble costo de
-- escritura sin ninguna ventaja de lectura.
drop index if exists idx_empresa_ruc;
