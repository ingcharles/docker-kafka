-- Alinea el vocabulario con el resto del ecosistema (ver
-- V4__renombrar_emisor_a_empresa.sql de firma-electronica-service).
alter table saga_instance rename column emisor_ruc to empresa_ruc;
