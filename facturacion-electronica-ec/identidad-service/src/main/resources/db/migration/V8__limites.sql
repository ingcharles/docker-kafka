-- Límites por empresa y por usuario (ver LimitadorDeUso en facturacion-commons y ARQUITECTURA.md 10.6).

create table limite_configuracion (
    ruc               varchar(13) not null,
    operacion         varchar(40) not null,
    maximo_usuario    integer     not null check (maximo_usuario >= 0),
    maximo_empresa    integer     not null check (maximo_empresa >= 0),
    ventana_segundos  integer     not null check (ventana_segundos between 1 and 86400),
    primary key (ruc, operacion)
);

comment on table limite_configuracion is 'Límites de las operaciones delicadas de este servicio, por empresa. Los cambios rigen en la siguiente petición.';
comment on column limite_configuracion.ruc is 'RUC de la empresa, o * para las empresas sin fila propia (y para el login, que no tiene empresa).';
comment on column limite_configuracion.operacion is 'Operación limitada: login, creacion-api-key, emision, carga-certificado o reenvio-correo.';
comment on column limite_configuracion.maximo_usuario is 'Veces que cada usuario puede hacerla en la ventana, dentro de la empresa; 0 = sin límite por usuario.';
comment on column limite_configuracion.maximo_empresa is 'Veces que todos los usuarios de la empresa juntos pueden hacerla en la ventana; 0 = sin límite por empresa.';
comment on column limite_configuracion.ventana_segundos is 'Duración de la ventana en segundos (1 a 86400).';

insert into limite_configuracion (ruc, operacion, maximo_usuario, maximo_empresa, ventana_segundos) values
    ('*', 'login', 5, 0, 900),
    ('*', 'creacion-api-key', 10, 0, 3600);

create table limite_uso (
    clave          varchar(200) primary key,
    inicio_ventana timestamptz  not null,
    usos           integer      not null
);

create index ix_limite_uso_inicio_ventana on limite_uso (inicio_ventana);

comment on table limite_uso is 'Usos de las operaciones limitadas dentro de su ventana de tiempo en curso.';
comment on column limite_uso.clave is 'operacion:ruc:usuario (contador del usuario) u operacion:ruc (contador de la empresa); sin empresa el ruc es *, y en el login el usuario es la huella SHA-256 del correo.';
comment on column limite_uso.inicio_ventana is 'Fecha y hora en que empezó la ventana en curso.';
comment on column limite_uso.usos is 'Cuántas veces se intentó la operación en la ventana en curso, incluidas las rechazadas.';
