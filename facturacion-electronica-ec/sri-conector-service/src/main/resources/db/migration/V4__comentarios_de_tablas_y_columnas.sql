-- Documenta cada tabla y cada columna del esquema "sri" con COMMENT ON (se ven en psql con \d+ y en cualquier cliente SQL).
-- Corre contra el esquema "sri". No cambia datos ni estructura.

comment on table configuracion_empresa_local is 'Réplica local de la configuración por empresa de firma-electronica-service (sri.recepcion.url, sri.autorizacion.url, sri.autorizacion.poll.max-intentos), actualizada por eventos de Kafka.';
comment on column configuracion_empresa_local.id is 'Identificador único de la fila (UUID).';
comment on column configuracion_empresa_local.ruc is 'RUC de la empresa a la que pertenece la configuración.';
comment on column configuracion_empresa_local.clave is 'Nombre de la configuración.';
comment on column configuracion_empresa_local.valor is 'Valor de la configuración.';
comment on column configuracion_empresa_local.actualizado_en is 'Fecha y hora de la última actualización recibida.';

comment on table envio_sri is 'Envío de un comprobante firmado al SRI (recepción y consulta de autorización por SOAP) y su resultado.';
comment on column envio_sri.id is 'Identificador único del envío (UUID).';
comment on column envio_sri.comprobante_id is 'Comprobante enviado.';
comment on column envio_sri.clave_acceso is 'Clave de acceso del comprobante (49 dígitos). Única.';
comment on column envio_sri.ambiente is 'Ambiente del SRI al que se envió: PRUEBAS o PRODUCCION.';
comment on column envio_sri.xml_firmado is 'XML firmado que se envió.';
comment on column envio_sri.estado is 'PENDIENTE_ENVIO, RECHAZADO_RECEPCION, RECIBIDO (esperando autorización), AUTORIZADO, NO_AUTORIZADO o ERROR_CONSULTA.';
comment on column envio_sri.mensajes_recepcion is 'Mensajes que devolvió la recepción del SRI (por ejemplo el motivo de un rechazo).';
comment on column envio_sri.intentos_autorizacion is 'Veces que se consultó la autorización.';
comment on column envio_sri.ultima_consulta_en is 'Fecha y hora de la última consulta de autorización.';
comment on column envio_sri.xml_autorizado is 'XML con la autorización del SRI.';
comment on column envio_sri.numero_autorizacion is 'Número de autorización que devolvió el SRI.';
comment on column envio_sri.fecha_autorizacion is 'Fecha y hora de la autorización.';
comment on column envio_sri.motivo_rechazo is 'Motivo con que el SRI no autorizó (hasta 1000 caracteres).';
comment on column envio_sri.creado_en is 'Fecha y hora de creación del envío.';
comment on column envio_sri.actualizado_en is 'Fecha y hora de la última modificación.';
comment on column envio_sri.empresa_ruc is 'RUC de la empresa emisora.';

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

