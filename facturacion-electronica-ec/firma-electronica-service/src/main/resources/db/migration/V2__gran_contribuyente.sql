-- Anexo 24 de la ficha técnica del SRI (incorporado en versiones posteriores a
-- la 2.26 que se usó para el diseño inicial): los comprobantes emitidos por un
-- "Gran Contribuyente" deben llevar la leyenda "Gran Contribuyente" + el
-- número de la resolución que lo designó como tal, en <infoAdicional>. Mismo
-- shape que contribuyente_especial — se agrega en una migración nueva, nunca
-- editando V1__init.sql (una migración ya aplicada no se toca: cambia su
-- checksum y rompe Flyway en cualquier réplica que ya la haya corrido).
alter table emisor add column es_gran_contribuyente boolean not null default false;
alter table emisor add column numero_resolucion_gran_contribuyente varchar(50);

alter table emisor add constraint chk_emisor_numero_resolucion_gran_contribuyente
    check (es_gran_contribuyente = false or numero_resolucion_gran_contribuyente is not null);
