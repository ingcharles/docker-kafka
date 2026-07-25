-- El emisor ya no se identifica por un id interno de firma-electronica-service
-- (UUID) — el resto del ecosistema lo referencia por ruc, que viaja inline
-- en cada request de creación de comprobante. Ver ARQUITECTURA.md, "Emisor
-- sin ID". No se puede castear la columna existente (el texto de un UUID no
-- cabe en varchar(13)) — como son solo sagas de prueba, se dropea y se
-- recrea vacía en vez de migrar los datos.
alter table saga_instance drop column emisor_id;
alter table saga_instance add column emisor_ruc varchar(13) not null default '';
alter table saga_instance alter column emisor_ruc drop default;
