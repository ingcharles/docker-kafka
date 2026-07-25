-- Este servicio deja de depender de la réplica local de Emisor
-- (firma-electronica-service) — cada request de creación de comprobante
-- trae inline todos los datos del emisor que necesita, identificado por
-- ruc en vez de un emisorId (UUID interno de otro servicio). Ver
-- ARQUITECTURA.md, "Emisor sin ID".
--
-- El vínculo con inventario-service (item_local, auto-derivación de
-- impuestos por itemId) SÍ se mantiene — lo que cambia ahí es que el
-- catálogo pasa a ser multi-tenant: cada ítem ahora pertenece a un ruc, y
-- una línea de detalle con itemId solo deriva su impuesto de un ítem que
-- pertenezca al mismo emisor que está emitiendo (ver
-- ItemLocalRepository.findByIdsAndRuc / ComprobanteService.construirDetalle).

alter table comprobante drop constraint if exists comprobante_emisor_id_fkey;
drop index if exists idx_comprobante_emisor_id;
alter table comprobante drop column emisor_id;
drop table if exists emisor_local;

alter table item_local add column ruc varchar(13);
update item_local set ruc = '' where ruc is null;
alter table item_local alter column ruc set not null;
create index idx_item_local_ruc on item_local (ruc);

-- secuencial_comprobante: la reserva atómica del siguiente secuencial ahora
-- se hace por ruc (natural, viene en cada request) en vez de por el antiguo
-- emisor_id (UUID interno de firma-electronica-service). No se puede
-- castear la columna existente (el texto de un UUID no cabe en
-- varchar(13)) — como son solo contadores de prueba sin ningún valor de
-- negocio real, se dropea y se recrea vacía en vez de migrar los datos.
alter table secuencial_comprobante drop constraint if exists uq_secuencial_comprobante;
alter table secuencial_comprobante drop column emisor_id;
alter table secuencial_comprobante add column ruc varchar(13) not null default '';
alter table secuencial_comprobante alter column ruc drop default;
alter table secuencial_comprobante add constraint uq_secuencial_comprobante
    unique (ruc, tipo_comprobante, establecimiento, punto_emision);

-- pagos/campoAdicional del comprobante — Tabla 24 (formas de pago) y el
-- anexo de información adicional del SRI, hasta ahora no expuestos en la
-- API. Serializados como JSON, mismo criterio que impuestos_json en
-- detalle_comprobante (ver ComprobanteMapper).
alter table comprobante add column pagos_json text;
alter table comprobante add column campos_adicionales_json text;
