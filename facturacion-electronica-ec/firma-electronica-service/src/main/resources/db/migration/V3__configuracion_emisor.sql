-- Tabla genérica clave/valor para configuración opcional y extensible por
-- emisor (URLs de SRI a sobreescribir, plantilla del RIDE, política de
-- reintentos, preferencias de notificación, feature flags, etc.) — a
-- diferencia de columnas como "ambiente" o "obligado_contabilidad" (datos
-- estructurales, siempre presentes, leídos en cada emisión), esto es para
-- ajustes que un operador real querría tocar sin necesitar un redeploy ni
-- una migración nueva cada vez. Ver ItemService (inventario-service) para
-- el mismo razonamiento aplicado a por qué Item.tipo SÍ es una columna.
create table configuracion_emisor (
    id            uuid primary key,
    emisor_id     uuid not null references emisor (id),
    clave         varchar(100) not null,
    valor         text not null,
    creado_en     timestamptz not null,
    actualizado_en timestamptz not null,
    constraint uq_configuracion_emisor_clave unique (emisor_id, clave)
);

create index idx_configuracion_emisor_emisor_id on configuracion_emisor (emisor_id);
