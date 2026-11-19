-- Corre contra el esquema "comprobantes".
--
-- Idempotencia de la emisión: un cliente que reintenta un POST (timeout, corte de red) con la misma cabecera
-- Idempotency-Key recibe el comprobante ya creado en vez de uno nuevo (que gastaría otro secuencial).
create table idempotencia_emision (
    id              uuid         primary key,
    ruc             varchar(13)  not null,
    clave           varchar(128) not null,
    huella          char(64)     not null,
    comprobante_id  uuid         references comprobante (id),
    creado_en       timestamptz  not null default now(),
    constraint uq_idempotencia_emision_ruc_clave unique (ruc, clave)
);

-- La purga periódica borra por antigüedad.
create index idx_idempotencia_emision_creado_en on idempotencia_emision (creado_en);

comment on table idempotencia_emision is 'Claves de idempotencia de las emisiones (cabecera Idempotency-Key): una fila por clave y RUC, creada en la misma transacción que el comprobante. Se purga pasado el plazo comprobantes.idempotencia.retencion.';
comment on column idempotencia_emision.id is 'Identificador técnico de la fila.';
comment on column idempotencia_emision.ruc is 'RUC de la empresa emisora; la clave es única dentro de cada RUC.';
comment on column idempotencia_emision.clave is 'Valor de la cabecera Idempotency-Key que mandó el cliente (8 a 128 caracteres: letras, números y . _ : -).';
comment on column idempotencia_emision.huella is 'SHA-256 en hexadecimal del tipo de comprobante y del cuerpo de la solicitud; sirve para detectar una clave reutilizada con un cuerpo distinto.';
comment on column idempotencia_emision.comprobante_id is 'Comprobante que creó esa solicitud; se llena en la misma transacción, por lo que una fila confirmada nunca lo tiene vacío.';
comment on column idempotencia_emision.creado_en is 'Instante de la primera solicitud con esa clave; la purga borra las filas más antiguas que el plazo de retención.';
