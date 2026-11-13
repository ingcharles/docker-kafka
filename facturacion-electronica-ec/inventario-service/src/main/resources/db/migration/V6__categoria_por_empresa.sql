-- Las categorías pasan de globales a pertenecer a una empresa (ruc), igual que los ítems (V3). Antes, cualquier
-- api-key con rol de escritura podía crear, renombrar o desactivar las categorías que veían todas las empresas.
--
-- Migración de datos sin perder nada: una categoría que usan ítems de varias empresas se copia una vez por empresa
-- y cada ítem pasa a apuntar a la copia de su propia empresa; una categoría que nadie usa se asigna al emisor con el
-- que se backfillearon los ítems en V3.
alter table categoria add column ruc varchar(13);

create temporary table categoria_por_ruc on commit drop as
    select c.id as id_original, i.ruc as ruc, gen_random_uuid() as id_nuevo
    from categoria c
    join (select distinct categoria_id, ruc from item where categoria_id is not null) i on i.categoria_id = c.id;

insert into categoria (id, nombre, tipo, descripcion, activa, creado_en, ruc)
    select m.id_nuevo, c.nombre, c.tipo, c.descripcion, c.activa, c.creado_en, m.ruc
    from categoria_por_ruc m
    join categoria c on c.id = m.id_original;

update item i
    set categoria_id = m.id_nuevo
    from categoria_por_ruc m
    where i.categoria_id = m.id_original and i.ruc = m.ruc;

delete from categoria where id in (select id_original from categoria_por_ruc);

update categoria set ruc = '1722039953001' where ruc is null;
alter table categoria alter column ruc set not null;
create index idx_categoria_ruc on categoria (ruc);
