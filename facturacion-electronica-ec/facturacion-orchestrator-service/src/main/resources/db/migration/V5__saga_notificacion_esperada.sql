-- Corre contra el esquema "orquestacion".
-- Si el comprobante no tiene correo del receptor, comprobantes-service no publica ComprobanteAutorizado y nunca habrá
-- notificación: antes esas sagas se quedaban en AUTORIZADO para siempre. Ahora ComprobanteCreado indica si se espera
-- notificación y, si no, la saga termina en AUTORIZADO_SIN_NOTIFICACION. Default true = comportamiento anterior para las
-- sagas ya existentes.
alter table saga_instance add column notificacion_esperada boolean not null default true;
