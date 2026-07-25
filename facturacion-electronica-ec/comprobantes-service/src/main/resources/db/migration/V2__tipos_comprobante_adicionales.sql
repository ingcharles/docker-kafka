-- Soporte para los 5 tipos de comprobante restantes (Nota de Crédito, Nota
-- de Débito, Guía de Remisión, Retención, Liquidación de Compra) además de
-- Factura. Todas las columnas nuevas van nullable a propósito: cada tipo
-- de comprobante usa un subconjunto distinto, y "comprobante" sigue siendo
-- una sola tabla para los 6 (en vez de una tabla por tipo) porque comparten
-- casi todo el resto del modelo (clave de acceso, emisor, XML por etapa,
-- estado, RIDE).

alter table comprobante
    -- Nota de Crédito / Nota de Débito / Retención: Obligatorio en el XML,
    -- pero no rastreado hasta ahora porque Factura no lo necesita — cuando
    -- falta se usa emisor_direccion_matriz como valor por defecto (ver
    -- ComprobanteService).
    add column direccion_establecimiento varchar(300),

    -- Nota de Crédito / Nota de Débito: identifica el comprobante que se
    -- está modificando (codDocModificado/numDocModificado/fechaEmisionDocSustento).
    add column doc_modificado_tipo varchar(2),
    add column doc_modificado_numero varchar(20),
    add column doc_modificado_fecha timestamptz,

    -- Retención: periodo fiscal MM/aaaa al que corresponde la retención.
    add column periodo_fiscal varchar(7),

    -- Guía de Remisión: datos del transporte (infoGuiaRemision).
    add column transportista_razon_social varchar(300),
    add column transportista_tipo_identificacion varchar(2),
    add column transportista_ruc varchar(20),
    add column dir_partida varchar(300),
    add column fecha_ini_transporte timestamptz,
    add column fecha_fin_transporte timestamptz,
    add column placa varchar(20);

-- Guía de Remisión no tiene comprador (es un traslado de bienes, no una
-- venta) — se relajan estas tres columnas, que hasta ahora asumían que
-- todo comprobante tenía comprador (cierto para Factura/Nota de
-- Crédito/Nota de Débito/Retención/Liquidación de Compra, falso para GR).
alter table comprobante
    alter column comprador_tipo_identificacion drop not null,
    alter column comprador_identificacion drop not null,
    alter column comprador_razon_social drop not null;

-- Guía de Remisión: uno o más destinatarios, cada uno con su propia lista
-- de detalles (ver detalle_comprobante.destinatario_id abajo) — a
-- diferencia de Factura/Nota de Crédito, aquí el detalle no cuelga
-- directamente del comprobante sino de un destinatario dentro de él.
create table destinatario_guia_remision (
    id                          uuid primary key,
    comprobante_id              uuid not null references comprobante (id) on delete cascade,
    identificacion              varchar(20) not null,
    razon_social                varchar(300) not null,
    direccion                   varchar(300) not null,
    motivo_traslado             varchar(300) not null,
    doc_aduanero_unico          varchar(20),
    cod_estab_destino           varchar(3),
    ruta                        varchar(300),
    cod_doc_sustento            varchar(2),
    num_doc_sustento            varchar(20),
    num_aut_doc_sustento        varchar(49),
    fecha_emision_doc_sustento  timestamptz
);

create index idx_destinatario_guia_remision_comprobante_id on destinatario_guia_remision (comprobante_id);

-- Nullable y solo se usa para Guía de Remisión: liga cada línea de detalle
-- al destinatario al que pertenece (las líneas de Factura/Nota de
-- Crédito/etc. la dejan en null, cuelgan directo del comprobante).
alter table detalle_comprobante
    add column destinatario_id uuid references destinatario_guia_remision (id) on delete cascade;

create index idx_detalle_comprobante_destinatario_id on detalle_comprobante (destinatario_id);

-- Guía de Remisión no lleva precio (es un traslado de bienes, no una
-- venta) y Retención/Nota de Débito reutilizan detalle_comprobante para
-- guardar líneas sin precio real (ver ComprobanteService) — se relaja de
-- "> 0" a ">= 0". cantidad se mantiene en "> 0": todo tipo de comprobante
-- necesita una cantidad real.
alter table detalle_comprobante drop constraint detalle_comprobante_precio_unitario_check;
alter table detalle_comprobante add constraint chk_detalle_comprobante_precio_unitario check (precio_unitario >= 0);
