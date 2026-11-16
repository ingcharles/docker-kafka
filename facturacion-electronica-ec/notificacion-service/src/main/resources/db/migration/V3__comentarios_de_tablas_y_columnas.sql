-- Documenta cada tabla y cada columna del esquema "notificacion" con COMMENT ON (se ven en psql con \d+ y en cualquier cliente SQL).
-- Corre contra el esquema "notificacion". No cambia datos ni estructura.

comment on table configuracion_empresa_local is 'Réplica local de la configuración por empresa de firma-electronica-service (notificacion.asunto, notificacion.remitente_nombre), actualizada por eventos de Kafka.';
comment on column configuracion_empresa_local.id is 'Identificador único de la fila (UUID).';
comment on column configuracion_empresa_local.ruc is 'RUC de la empresa a la que pertenece la configuración.';
comment on column configuracion_empresa_local.clave is 'Nombre de la configuración.';
comment on column configuracion_empresa_local.valor is 'Valor de la configuración.';
comment on column configuracion_empresa_local.actualizado_en is 'Fecha y hora de la última actualización recibida.';

comment on table notificacion_enviada is 'Registro del intento de enviar por correo el RIDE y el XML autorizado de un comprobante. Es el único lugar donde queda el resultado del envío.';
comment on column notificacion_enviada.id is 'Identificador único de la notificación (UUID).';
comment on column notificacion_enviada.comprobante_id is 'Comprobante notificado.';
comment on column notificacion_enviada.clave_acceso is 'Clave de acceso del comprobante (49 dígitos).';
comment on column notificacion_enviada.destinatario is 'Correo al que se envió.';
comment on column notificacion_enviada.estado is 'ENVIADA o FALLIDA.';
comment on column notificacion_enviada.motivo_error is 'Motivo del fallo cuando el estado es FALLIDA.';
comment on column notificacion_enviada.enviado_en is 'Fecha y hora del intento de envío.';

comment on table outbox_event is 'Bandeja de salida (patrón outbox): eventos de Kafka que se escriben en la misma transacción del negocio y un relay publica después.';
comment on column outbox_event.id is 'Identificador único del evento (UUID); también es la clave de idempotencia para los consumidores.';
comment on column outbox_event.aggregate_id is 'Identificador del agregado que originó el evento (p. ej. el comprobanteId); es la clave del mensaje en Kafka.';
comment on column outbox_event.event_type is 'Tipo del evento (p. ej. ComprobanteCreado).';
comment on column outbox_event.topic is 'Tópico de Kafka al que se publica.';
comment on column outbox_event.payload is 'Contenido del mensaje en JSON.';
comment on column outbox_event.created_at is 'Fecha y hora en que se registró el evento.';
comment on column outbox_event.published_at is 'Fecha y hora en que el relay lo publicó en Kafka; vacío mientras está pendiente.';

comment on table processed_message is 'Bandeja de entrada (patrón inbox): mensajes de Kafka ya procesados, para que un mensaje repetido no se aplique dos veces.';
comment on column processed_message.message_id is 'Identificador del mensaje procesado (el id del evento recibido).';
comment on column processed_message.processed_at is 'Fecha y hora en que se procesó.';

