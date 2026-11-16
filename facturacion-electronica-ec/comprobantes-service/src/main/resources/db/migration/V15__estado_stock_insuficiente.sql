-- Con el descuento de inventario habilitado (orquestador), un comprobante cuyo stock no se pudo reservar queda en
-- STOCK_INSUFICIENTE: estado terminal nuevo, que el check de estado debe aceptar.
alter table comprobante drop constraint chk_comprobante_estado;
alter table comprobante add constraint chk_comprobante_estado check (estado in
    ('BORRADOR', 'FIRMADO', 'FIRMA_FALLIDA', 'AUTORIZADO', 'NO_AUTORIZADO', 'NOTIFICADO', 'ANULADO', 'STOCK_INSUFICIENTE'));
