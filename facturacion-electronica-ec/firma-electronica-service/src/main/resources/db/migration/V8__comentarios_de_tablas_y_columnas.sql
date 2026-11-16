-- Documenta cada tabla y cada columna del esquema "firma_electronica" con COMMENT ON (se ven en psql con \d+ y en cualquier cliente SQL).
-- Corre contra el esquema "firma_electronica". No cambia datos ni estructura.

comment on table certificado_digital is 'Certificado de firma electrónica (.p12) de una empresa, guardado cifrado. Una empresa puede tener varios; vale el último ACTIVO.';
comment on column certificado_digital.id is 'Identificador único del certificado (UUID).';
comment on column certificado_digital.empresa_id is 'Empresa dueña del certificado.';
comment on column certificado_digital.alias is 'Alias del certificado dentro del .p12.';
comment on column certificado_digital.huella_digital is 'Huella SHA-256 del certificado, para identificarlo sin abrirlo.';
comment on column certificado_digital.contenido_cifrado is 'Contenido del archivo .p12 cifrado con AES-256 y la clave maestra del servicio.';
comment on column certificado_digital.contrasena_cifrada is 'Contraseña del .p12 cifrada con AES-256 y la clave maestra del servicio.';
comment on column certificado_digital.fecha_emision_certificado is 'Inicio de vigencia del certificado (notBefore).';
comment on column certificado_digital.fecha_expiracion_certificado is 'Fin de vigencia del certificado (notAfter); pasada esa fecha el estado pasa a EXPIRADO.';
comment on column certificado_digital.estado is 'Estado: ACTIVO (puede firmar), EXPIRADO (venció) o REVOCADO (dado de baja a mano).';
comment on column certificado_digital.creado_en is 'Fecha y hora en que se subió el certificado.';

comment on table configuracion_empresa is 'Configuración por empresa en pares clave/valor (p. ej. ride.color, ride.logo, notificacion.asunto, sri.recepcion.url). Se propaga a los demás servicios por eventos.';
comment on column configuracion_empresa.id is 'Identificador único de la fila (UUID).';
comment on column configuracion_empresa.empresa_id is 'Empresa a la que pertenece la configuración.';
comment on column configuracion_empresa.clave is 'Nombre de la configuración (única por empresa).';
comment on column configuracion_empresa.valor is 'Valor de la configuración.';
comment on column configuracion_empresa.creado_en is 'Fecha y hora de creación.';
comment on column configuracion_empresa.actualizado_en is 'Fecha y hora de la última modificación.';

comment on table empresa is 'Empresa emisora de comprobantes (identificada por su RUC), con los datos tributarios que van en cada comprobante y su logo para el RIDE.';
comment on column empresa.id is 'Identificador único de la empresa (UUID).';
comment on column empresa.ruc is 'RUC de 13 dígitos. Único.';
comment on column empresa.razon_social is 'Razón social.';
comment on column empresa.nombre_comercial is 'Nombre comercial. Opcional.';
comment on column empresa.direccion_matriz is 'Dirección de la matriz.';
comment on column empresa.obligado_contabilidad is 'Indica si está obligada a llevar contabilidad.';
comment on column empresa.contribuyente_especial is 'Indica si es contribuyente especial.';
comment on column empresa.numero_contribuyente_especial is 'Número de resolución de contribuyente especial; obligatorio cuando contribuyente_especial es verdadero.';
comment on column empresa.vende_combustibles is 'Indica si la empresa vende combustibles (dato tributario de la empresa; viaja en el evento de empresa).';
comment on column empresa.ambiente is 'Ambiente del SRI con el que emite: PRUEBAS o PRODUCCION.';
comment on column empresa.establecimiento_defecto is 'Establecimiento que se usa cuando la petición no lo indica (3 dígitos).';
comment on column empresa.punto_emision_defecto is 'Punto de emisión que se usa cuando la petición no lo indica (3 dígitos).';
comment on column empresa.activo is 'Indica si la empresa está activa.';
comment on column empresa.creado_en is 'Fecha y hora de creación.';
comment on column empresa.actualizado_en is 'Fecha y hora de la última modificación.';
comment on column empresa.es_gran_contribuyente is 'Indica si es gran contribuyente.';
comment on column empresa.numero_resolucion_gran_contribuyente is 'Número de resolución de gran contribuyente; obligatorio cuando es_gran_contribuyente es verdadero.';
comment on column empresa.logo is 'Imagen del logo (PNG o JPG) que se imprime en el RIDE. Opcional.';
comment on column empresa.logo_tipo is 'Tipo MIME del logo (image/png o image/jpeg).';

comment on table establecimiento is 'Establecimiento (matriz o sucursal) de una empresa.';
comment on column establecimiento.id is 'Identificador único del establecimiento (UUID).';
comment on column establecimiento.empresa_id is 'Empresa a la que pertenece.';
comment on column establecimiento.codigo is 'Código de 3 dígitos del establecimiento (único por empresa).';
comment on column establecimiento.direccion is 'Dirección del establecimiento.';
comment on column establecimiento.tipo is 'MATRIZ o SUCURSAL.';
comment on column establecimiento.activo is 'Indica si el establecimiento está activo.';
comment on column establecimiento.creado_en is 'Fecha y hora de creación.';
comment on column establecimiento.actualizado_en is 'Fecha y hora de la última modificación.';

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

comment on table punto_emision is 'Punto de emisión (caja o terminal) de un establecimiento.';
comment on column punto_emision.id is 'Identificador único del punto de emisión (UUID).';
comment on column punto_emision.establecimiento_id is 'Establecimiento al que pertenece.';
comment on column punto_emision.codigo is 'Código de 3 dígitos del punto de emisión (único por establecimiento).';
comment on column punto_emision.descripcion is 'Descripción del punto de emisión (p. ej. Caja 1). Opcional.';
comment on column punto_emision.activo is 'Indica si el punto de emisión está activo.';
comment on column punto_emision.creado_en is 'Fecha y hora de creación.';
comment on column punto_emision.actualizado_en is 'Fecha y hora de la última modificación.';

