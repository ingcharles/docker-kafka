-- El catálogo pasa a ser multi-tenant: cada ítem pertenece a un ruc
-- (emisor/empresa), en vez de ser un catálogo único y global. Esto es lo
-- que permite que comprobantes-service derive el impuesto de un itemId de
-- forma segura entre varios emisores (ver
-- ItemLocalRepository.findByIdsAndRuc en comprobantes-service) — sin esto,
-- cualquier emisor podría "usar" el ítem de otro solo adivinando su UUID.
--
-- Las filas ya existentes (creadas antes de este cambio, todas de pruebas
-- con el mismo emisor real usado en este proyecto) se backfillean con ese
-- ruc; toda fila nueva lo recibe explícito desde CrearItemRequest.
alter table item add column ruc varchar(13);
update item set ruc = '1722039953001' where ruc is null;
alter table item alter column ruc set not null;
create index idx_item_ruc on item (ruc);
