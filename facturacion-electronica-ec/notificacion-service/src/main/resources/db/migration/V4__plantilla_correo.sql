-- Plantillas de correo propias de cada empresa (asunto y cuerpo en HTML y texto) y su historial de cambios.
-- Una empresa tiene como máximo una plantilla por tipo de comprobante; el tipo GENERICO aplica a cualquier tipo que no
-- tenga plantilla propia. Sin plantilla propia, el correo sale con la plantilla por defecto del servicio (archivos).

create table plantilla_correo (
    id                uuid primary key,
    empresa_ruc       varchar(13)  not null,
    tipo_comprobante  varchar(30)  not null,
    asunto            varchar(200) not null,
    cuerpo_html       text         not null,
    cuerpo_texto      text,
    version           integer      not null,
    activa            boolean      not null default true,
    creado_en         timestamptz  not null,
    actualizado_en    timestamptz  not null,
    constraint uq_plantilla_correo_empresa_tipo unique (empresa_ruc, tipo_comprobante),
    constraint ck_plantilla_correo_version check (version >= 1)
);

create table plantilla_correo_historial (
    id                uuid primary key,
    empresa_ruc       varchar(13)  not null,
    tipo_comprobante  varchar(30)  not null,
    version           integer      not null,
    asunto            varchar(200) not null,
    cuerpo_html       text         not null,
    cuerpo_texto      text,
    activa            boolean      not null,
    accion            varchar(20)  not null,
    registrado_en     timestamptz  not null,
    constraint ck_plantilla_correo_historial_accion check (accion in ('REEMPLAZADA', 'ELIMINADA'))
);

create index idx_plantilla_correo_historial_empresa_tipo
    on plantilla_correo_historial (empresa_ruc, tipo_comprobante, version desc);

comment on table plantilla_correo is 'Plantilla de correo propia de una empresa para un tipo de comprobante: asunto, cuerpo HTML (ya limpiado) y cuerpo en texto plano. Las variables van como {{numero}}, {{fechaEmision}}, etc.';
comment on column plantilla_correo.id is 'Identificador único de la plantilla (UUID).';
comment on column plantilla_correo.empresa_ruc is 'RUC de la empresa dueña de la plantilla.';
comment on column plantilla_correo.tipo_comprobante is 'FACTURA, NOTA_CREDITO, NOTA_DEBITO, GUIA_REMISION, RETENCION, LIQUIDACION_COMPRA o GENERICO (cualquier tipo sin plantilla propia).';
comment on column plantilla_correo.asunto is 'Asunto del correo, en una sola línea; admite variables {{...}}.';
comment on column plantilla_correo.cuerpo_html is 'Cuerpo del correo en HTML, sin html/head/body: el servicio lo envuelve. Se guarda ya limpiado (sin scripts, formularios ni URL peligrosos).';
comment on column plantilla_correo.cuerpo_texto is 'Versión en texto plano para clientes sin HTML. Si es nulo se deriva del HTML al enviar.';
comment on column plantilla_correo.version is 'Número de versión; empieza en 1 y sube con cada cambio.';
comment on column plantilla_correo.activa is 'false deja la plantilla guardada pero sin uso: el correo sale con la plantilla por defecto.';
comment on column plantilla_correo.creado_en is 'Fecha y hora de la primera versión.';
comment on column plantilla_correo.actualizado_en is 'Fecha y hora de la versión actual.';

comment on table plantilla_correo_historial is 'Versiones anteriores de las plantillas de correo, para ver qué cambió y recuperar un texto anterior. Se agrega una fila cada vez que una versión se reemplaza o se elimina.';
comment on column plantilla_correo_historial.id is 'Identificador único de la fila del historial (UUID).';
comment on column plantilla_correo_historial.empresa_ruc is 'RUC de la empresa dueña de la plantilla.';
comment on column plantilla_correo_historial.tipo_comprobante is 'Tipo de comprobante de la plantilla.';
comment on column plantilla_correo_historial.version is 'Versión que se guardó en esta fila.';
comment on column plantilla_correo_historial.asunto is 'Asunto de esa versión.';
comment on column plantilla_correo_historial.cuerpo_html is 'Cuerpo HTML de esa versión.';
comment on column plantilla_correo_historial.cuerpo_texto is 'Cuerpo en texto plano de esa versión (puede ser nulo).';
comment on column plantilla_correo_historial.activa is 'Si esa versión estaba activa.';
comment on column plantilla_correo_historial.accion is 'REEMPLAZADA (hay una versión más nueva) o ELIMINADA (la plantilla se borró).';
comment on column plantilla_correo_historial.registrado_en is 'Fecha y hora en que la versión se reemplazó o se eliminó.';
