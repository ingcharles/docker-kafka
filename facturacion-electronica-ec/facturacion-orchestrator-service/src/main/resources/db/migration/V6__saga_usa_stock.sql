-- true si la saga usa inventario (descuento de inventario habilitado al crearla): decide si al autorizar se confirma
-- el stock y si al fallar se libera. Las sagas existentes no usaban inventario.
alter table saga_instance add column usa_stock boolean not null default false;
