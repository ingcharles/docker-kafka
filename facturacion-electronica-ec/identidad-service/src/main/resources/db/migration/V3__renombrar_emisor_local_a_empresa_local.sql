-- Alinea el vocabulario con firma-electronica-service (ver
-- V4__renombrar_emisor_a_empresa.sql de ese servicio) — esta tabla es la
-- réplica local vía Kafka de ese mismo concepto.

alter table emisor_local rename to empresa_local;
alter table empresa_local rename constraint emisor_local_pkey to empresa_local_pkey;
alter table empresa_local rename constraint emisor_local_ruc_key to empresa_local_ruc_key;
