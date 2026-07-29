-- Réplica local de solo lectura de configuracion_empresa (firma-electronica-service)
-- — ver ConfiguracionEmpresaEventConsumer. Genérica (mismo store clave/valor
-- del original); hoy los consumidores reales son SriConectorService para
-- "sri.recepcion.url", "sri.autorizacion.url" y
-- "sri.autorizacion.poll.max-intentos".
create table configuracion_empresa_local (
    id              uuid primary key,
    ruc             varchar(13) not null,
    clave           varchar(100) not null,
    valor           text,
    actualizado_en  timestamptz not null,
    constraint uq_configuracion_empresa_local_ruc_clave unique (ruc, clave)
);
