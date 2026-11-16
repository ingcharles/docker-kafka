-- Documenta cada tabla y cada columna del esquema "identidad" con COMMENT ON (se ven en psql con \d+ y en cualquier cliente SQL).
-- Corre contra el esquema "identidad". No cambia datos ni estructura.

comment on table api_key is 'Api-key (JWT Ed25519) emitida a un usuario para una sola empresa y un solo rol. Solo se guarda su metadato: el token se muestra una vez al crearla.';
comment on column api_key.id is 'Identificador único de la api-key (UUID).';
comment on column api_key.usuario_id is 'Usuario que la creó.';
comment on column api_key.nombre is 'Nombre que el usuario le dio (único por usuario).';
comment on column api_key.ruc is 'RUC de la única empresa para la que vale.';
comment on column api_key.rol is 'Único rol que concede: ADMIN, OPERADOR o LECTURA.';
comment on column api_key.jti is 'Identificador del token (claim jti); clave para revocarlo. Único.';
comment on column api_key.fecha_creacion is 'Fecha y hora de creación.';
comment on column api_key.fecha_expiracion is 'Fecha y hora de expiración del token; vacío si no vence.';
comment on column api_key.revocada is 'Indica si fue revocada; una api-key revocada deja de valer en todos los servicios.';
comment on column api_key.revocada_en is 'Fecha y hora de la revocación.';
comment on column api_key.establecimiento is 'Si se informa, restringe la api-key a ese establecimiento (3 dígitos). Opcional.';
comment on column api_key.punto_emision is 'Si se informa, restringe la api-key a ese punto de emisión dentro del establecimiento. Opcional.';

comment on table empresa_local is 'Réplica local de las empresas (desde firma-electronica-service), usada para validar que el RUC exista al crear api-keys.';
comment on column empresa_local.id is 'Identificador de la empresa; el mismo de firma-electronica-service.';
comment on column empresa_local.ruc is 'RUC de la empresa.';
comment on column empresa_local.razon_social is 'Razón social de la empresa.';
comment on column empresa_local.activo is 'Indica si la empresa está activa.';
comment on column empresa_local.actualizado_en is 'Fecha y hora de la última actualización recibida.';

comment on table establecimiento_local is 'Réplica local de los establecimientos de cada empresa (desde firma-electronica-service), usada para acotar api-keys.';
comment on column establecimiento_local.id is 'Identificador único de la fila (UUID).';
comment on column establecimiento_local.ruc is 'RUC de la empresa dueña del establecimiento.';
comment on column establecimiento_local.codigo is 'Código del establecimiento (3 dígitos).';
comment on column establecimiento_local.activo is 'Indica si el establecimiento está activo.';
comment on column establecimiento_local.actualizado_en is 'Fecha y hora de la última actualización recibida.';

comment on table punto_emision_local is 'Réplica local de los puntos de emisión (desde firma-electronica-service), usada para acotar api-keys.';
comment on column punto_emision_local.id is 'Identificador único de la fila (UUID).';
comment on column punto_emision_local.ruc is 'RUC de la empresa dueña del punto de emisión.';
comment on column punto_emision_local.establecimiento_codigo is 'Código del establecimiento al que pertenece.';
comment on column punto_emision_local.codigo is 'Código del punto de emisión (3 dígitos).';
comment on column punto_emision_local.activo is 'Indica si el punto de emisión está activo.';
comment on column punto_emision_local.actualizado_en is 'Fecha y hora de la última actualización recibida.';

comment on table usuario is 'Cuenta de persona que inicia sesión y administra empresas y api-keys.';
comment on column usuario.id is 'Identificador único del usuario (UUID).';
comment on column usuario.email is 'Correo del usuario en minúsculas y sin espacios; es el nombre de inicio de sesión. Único.';
comment on column usuario.password_hash is 'Hash bcrypt de la contraseña (nunca la contraseña).';
comment on column usuario.nombres is 'Nombres del usuario.';
comment on column usuario.apellidos is 'Apellidos del usuario.';
comment on column usuario.activo is 'Indica si la cuenta está activa.';
comment on column usuario.creado_en is 'Fecha y hora de creación.';
comment on column usuario.actualizado_en is 'Fecha y hora de la última modificación.';

comment on table usuario_empresa_rol is 'Rol de un usuario en una empresa (un rol por usuario y empresa).';
comment on column usuario_empresa_rol.id is 'Identificador único de la fila (UUID).';
comment on column usuario_empresa_rol.usuario_id is 'Usuario al que se le concede el rol.';
comment on column usuario_empresa_rol.ruc is 'RUC de la empresa en la que tiene el rol.';
comment on column usuario_empresa_rol.rol is 'Rol: ADMIN, OPERADOR o LECTURA.';
comment on column usuario_empresa_rol.creado_en is 'Fecha y hora en que se concedió.';

