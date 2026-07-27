-- Cuenta de usuario — autentica contra este servicio (email+password) para
-- poder gestionar sus asociaciones a empresas (ruc) y emitir api-keys.
create table usuario (
    id                  uuid primary key,
    email               varchar(300) not null unique,
    password_hash       varchar(200) not null,
    nombres             varchar(200) not null,
    apellidos           varchar(200) not null,
    activo              boolean not null default true,
    creado_en           timestamptz not null,
    actualizado_en      timestamptz not null
);

-- Qué empresas (ruc) puede administrar cada usuario y con qué rol. No hay
-- flujo de invitación/aprobación en esta versión de referencia: cualquier
-- usuario autenticado puede asociarse a cualquier ruc — no hay integración
-- con un registro real de contribuyentes que confirme la titularidad. Ver
-- ARQUITECTURA.md.
create table usuario_empresa_rol (
    id                  uuid primary key,
    usuario_id          uuid not null references usuario (id),
    ruc                 varchar(13) not null,
    rol                 varchar(20) not null,
    creado_en           timestamptz not null,
    constraint uq_usuario_empresa unique (usuario_id, ruc)
);

create index idx_usuario_empresa_rol_ruc on usuario_empresa_rol (ruc);

-- Una api-key ES un JWT autoverificable (HS256, ver JwtService) — esta
-- tabla no guarda el valor de la key (no se puede recuperar después de
-- creada, igual que Veronica), solo su jti para poder listarla/revocarla.
-- La revocación es de "mejor esfuerzo": al marcar revocada=true acá, los
-- demás servicios (que verifican el JWT localmente sin consultar esta
-- tabla) seguirán aceptándolo hasta que expire — no hay lista de
-- revocación distribuida en esta versión de referencia.
create table api_key (
    id                  uuid primary key,
    usuario_id          uuid not null references usuario (id),
    nombre              varchar(200) not null,
    ruc                 varchar(13) not null,
    rol                 varchar(20) not null,
    jti                 varchar(50) not null unique,
    fecha_creacion      timestamptz not null,
    fecha_expiracion    timestamptz,
    revocada            boolean not null default false,
    revocada_en         timestamptz,
    constraint uq_api_key_nombre_usuario unique (usuario_id, nombre)
);

create index idx_api_key_usuario on api_key (usuario_id);
