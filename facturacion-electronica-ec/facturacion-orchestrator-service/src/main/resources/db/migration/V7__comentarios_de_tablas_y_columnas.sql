-- Documenta cada tabla y cada columna del esquema "orquestacion" con COMMENT ON (se ven en psql con \d+ y en cualquier cliente SQL).
-- Corre contra el esquema "orquestacion". No cambia datos ni estructura.

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

comment on table saga_instance is 'Saga de un comprobante: sigue su avance por stock, firma, SRI y notificación.';
comment on column saga_instance.id is 'Identificador de la saga; es el mismo comprobanteId.';
comment on column saga_instance.state is 'Estado: STOCK_PENDIENTE, STOCK_RESERVADO, STOCK_NO_DISPONIBLE, CREADO, FIRMADO, FIRMA_FALLIDA, RECHAZADO_RECEPCION, AUTORIZADO, AUTORIZADO_SIN_NOTIFICACION, NO_AUTORIZADO, NOTIFICADO, NOTIFICACION_FALLIDA o EXPIRADA.';
comment on column saga_instance.clave_acceso is 'Clave de acceso del comprobante (49 dígitos).';
comment on column saga_instance.ambiente is 'Ambiente del SRI: PRUEBAS o PRODUCCION.';
comment on column saga_instance.failure_reason is 'Motivo del fallo cuando la saga termina en un estado de error.';
comment on column saga_instance.created_at is 'Fecha y hora de creación de la saga.';
comment on column saga_instance.updated_at is 'Fecha y hora del último cambio de estado.';
comment on column saga_instance.empresa_ruc is 'RUC de la empresa emisora.';
comment on column saga_instance.reintentos is 'Veces que el reintentador volvió a enviar el comando pendiente.';
comment on column saga_instance.notificacion_esperada is 'Indica si la saga espera la notificación por correo para terminar; si es falso termina en AUTORIZADO_SIN_NOTIFICACION.';
comment on column saga_instance.usa_stock is 'Indica si la saga reservó stock y debe confirmarlo o liberarlo al terminar (depende de INVENTARIO_DESCUENTO_HABILITADO).';

comment on table saga_step is 'Paso de auditoría de una saga: mensajes recibidos, comandos enviados y notas.';
comment on column saga_step.id is 'Identificador único del paso (UUID).';
comment on column saga_step.saga_id is 'Saga a la que pertenece.';
comment on column saga_step.step_name is 'Nombre del paso (p. ej. ReservarStock, ComprobanteFirmado, SriAutorizado).';
comment on column saga_step.status is 'RECEIVED (evento recibido), SENT (comando enviado) o NOTA (anotación interna, como el comando de firma pendiente).';
comment on column saga_step.topic is 'Tópico de Kafka al que se envió el comando (solo en SENT).';
comment on column saga_step.payload is 'Contenido del comando enviado, conservado para reintentos.';
comment on column saga_step.created_at is 'Fecha y hora del paso.';

