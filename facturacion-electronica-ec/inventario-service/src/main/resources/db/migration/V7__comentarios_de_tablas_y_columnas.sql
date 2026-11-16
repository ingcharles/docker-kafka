-- Documenta cada tabla y cada columna del esquema "inventario" con COMMENT ON (se ven en psql con \d+ y en cualquier cliente SQL).
-- Corre contra el esquema "inventario". No cambia datos ni estructura.

comment on table categoria is 'Categoría de ítems de una empresa.';
comment on column categoria.id is 'Identificador único de la categoría (UUID).';
comment on column categoria.nombre is 'Nombre de la categoría.';
comment on column categoria.tipo is 'A qué ítems aplica: PRODUCTO, SERVICIO o AMBOS.';
comment on column categoria.descripcion is 'Descripción. Opcional.';
comment on column categoria.activa is 'Indica si la categoría está activa.';
comment on column categoria.creado_en is 'Fecha y hora de creación.';
comment on column categoria.ruc is 'RUC de la empresa dueña de la categoría.';

comment on table imagen_item is 'Imagen de un ítem (el contenido se guarda en la base).';
comment on column imagen_item.id is 'Identificador único de la imagen (UUID).';
comment on column imagen_item.item_id is 'Ítem al que pertenece.';
comment on column imagen_item.nombre_archivo is 'Nombre original del archivo.';
comment on column imagen_item.tipo_mime is 'Tipo MIME de la imagen (p. ej. image/png).';
comment on column imagen_item.tamano_bytes is 'Tamaño del contenido, en bytes.';
comment on column imagen_item.contenido is 'Bytes de la imagen.';
comment on column imagen_item.es_principal is 'Indica si es la imagen principal del ítem.';
comment on column imagen_item.orden is 'Posición de la imagen en la galería del ítem.';
comment on column imagen_item.creado_en is 'Fecha y hora en que se subió.';

comment on table item is 'Producto o servicio de una empresa, con su stock y los impuestos con que se factura.';
comment on column item.id is 'Identificador único del ítem (UUID); se envía en las líneas del comprobante.';
comment on column item.tipo is 'PRODUCTO (lleva stock) o SERVICIO (no lleva stock).';
comment on column item.categoria_id is 'Categoría del ítem. Opcional.';
comment on column item.codigo_principal is 'Código principal del ítem (único por empresa).';
comment on column item.codigo_auxiliar is 'Código auxiliar. Opcional.';
comment on column item.descripcion is 'Descripción del ítem.';
comment on column item.precio_unitario is 'Precio de venta unitario sin impuestos (mayor que cero).';
comment on column item.unidad_medida is 'Unidad de medida (p. ej. unidad, kg). Opcional.';
comment on column item.cantidad_disponible is 'Stock disponible para vender: ya descuenta lo reservado.';
comment on column item.cantidad_reservada is 'Stock apartado por comprobantes en proceso, hasta que se confirma o se libera.';
comment on column item.stock_minimo is 'Umbral de alerta de stock bajo.';
comment on column item.activo is 'Indica si el ítem está activo.';
comment on column item.creado_en is 'Fecha y hora de creación.';
comment on column item.actualizado_en is 'Fecha y hora de la última modificación.';
comment on column item.codigo_impuesto is 'Código del impuesto que grava al ítem (2 = IVA).';
comment on column item.codigo_porcentaje is 'Código de la tarifa de IVA del ítem (4 = 15 %).';
comment on column item.tiene_ice is 'Indica si el ítem lleva ICE.';
comment on column item.codigo_porcentaje_ice is 'Código de la tarifa de ICE; solo si tiene_ice es verdadero.';
comment on column item.tarifa_ice is 'Tarifa de ICE; solo si tiene_ice es verdadero.';
comment on column item.ruc is 'RUC de la empresa dueña del ítem.';

comment on table movimiento_kardex is 'Kardex: cada entrada, salida o ajuste de stock de un ítem, con su saldo y costo promedio. Es el rastro contable; solo se escribe al confirmar una salida.';
comment on column movimiento_kardex.id is 'Identificador único del movimiento (UUID).';
comment on column movimiento_kardex.item_id is 'Ítem al que corresponde.';
comment on column movimiento_kardex.tipo_movimiento is 'ENTRADA, SALIDA, AJUSTE_POSITIVO o AJUSTE_NEGATIVO.';
comment on column movimiento_kardex.cantidad is 'Cantidad movida (mayor que cero).';
comment on column movimiento_kardex.costo_unitario is 'Costo unitario del movimiento.';
comment on column movimiento_kardex.cantidad_saldo is 'Existencia del ítem después del movimiento.';
comment on column movimiento_kardex.costo_promedio_saldo is 'Costo promedio ponderado después del movimiento.';
comment on column movimiento_kardex.valor_saldo is 'Valor del inventario del ítem después del movimiento: existencia por costo promedio.';
comment on column movimiento_kardex.referencia_documento is 'Documento que originó el movimiento (p. ej. comprobante:<id>).';
comment on column movimiento_kardex.motivo is 'Motivo del movimiento, sobre todo en ajustes. Opcional.';
comment on column movimiento_kardex.creado_en is 'Fecha y hora del movimiento.';

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

comment on table reserva_stock is 'Reserva de stock que hace un comprobante antes de firmarse; se confirma al autorizarse o se libera si falla.';
comment on column reserva_stock.id is 'Identificador único de la reserva (UUID).';
comment on column reserva_stock.comprobante_id is 'Comprobante que reserva el stock.';
comment on column reserva_stock.item_id is 'Ítem reservado.';
comment on column reserva_stock.cantidad is 'Cantidad reservada (mayor que cero).';
comment on column reserva_stock.estado is 'RESERVADO (apartado), CONFIRMADO (descontado y registrado en el kardex) o LIBERADO (devuelto al disponible).';
comment on column reserva_stock.creado_en is 'Fecha y hora de la reserva.';

