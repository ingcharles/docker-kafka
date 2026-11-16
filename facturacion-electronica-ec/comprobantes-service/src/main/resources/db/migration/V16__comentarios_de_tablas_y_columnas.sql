-- Documenta cada tabla y cada columna del esquema "comprobantes" con COMMENT ON (se ven en psql con \d+ y en cualquier cliente SQL).
-- Corre contra el esquema "comprobantes". No cambia datos ni estructura.

comment on table comprobante is 'Comprobante electrónico emitido (factura, nota de crédito, nota de débito, guía de remisión, retención o liquidación de compra), con su XML en cada etapa y su estado en el ciclo de vida.';
comment on column comprobante.id is 'Identificador único del comprobante (UUID); es el comprobanteId de los eventos y de la saga.';
comment on column comprobante.tipo_comprobante is 'Tipo de comprobante: FACTURA, NOTA_CREDITO, NOTA_DEBITO, GUIA_REMISION, RETENCION o LIQUIDACION_COMPRA.';
comment on column comprobante.clave_acceso is 'Clave de acceso de 49 dígitos del SRI (fecha, tipo, RUC, ambiente, serie, secuencial, código numérico, tipo de emisión y dígito verificador). Única.';
comment on column comprobante.ambiente is 'Ambiente del SRI en el que se emite: PRUEBAS o PRODUCCION.';
comment on column comprobante.establecimiento is 'Código del establecimiento emisor (3 dígitos, p. ej. 001).';
comment on column comprobante.punto_emision is 'Código del punto de emisión dentro del establecimiento (3 dígitos, p. ej. 001).';
comment on column comprobante.secuencial is 'Número secuencial del comprobante (9 dígitos con ceros a la izquierda), correlativo por RUC, tipo, establecimiento y punto de emisión.';
comment on column comprobante.empresa_ruc is 'RUC (13 dígitos) de la empresa emisora.';
comment on column comprobante.empresa_razon_social is 'Razón social del emisor, copiada al emitir (no cambia si la empresa se edita después).';
comment on column comprobante.empresa_nombre_comercial is 'Nombre comercial del emisor, copiado al emitir. Opcional.';
comment on column comprobante.empresa_direccion_matriz is 'Dirección de la matriz del emisor, copiada al emitir.';
comment on column comprobante.empresa_obligado_contabilidad is 'Indica si el emisor está obligado a llevar contabilidad (SI/NO en el XML).';
comment on column comprobante.empresa_contribuyente_especial is 'Indica si el emisor es contribuyente especial.';
comment on column comprobante.empresa_numero_contribuyente_especial is 'Número de resolución de contribuyente especial; obligatorio cuando empresa_contribuyente_especial es verdadero.';
comment on column comprobante.empresa_es_gran_contribuyente is 'Indica si el emisor es gran contribuyente.';
comment on column comprobante.empresa_numero_resolucion_gran_contribuyente is 'Número de resolución de gran contribuyente; se informa cuando empresa_es_gran_contribuyente es verdadero.';
comment on column comprobante.fecha_emision is 'Fecha y hora de emisión del comprobante.';
comment on column comprobante.comprador_tipo_identificacion is 'Código del tipo de identificación del comprador (tabla tipo_identificacion: 04 RUC, 05 cédula, 06 pasaporte, 07 consumidor final, 08 identificación del exterior).';
comment on column comprobante.comprador_identificacion is 'Número de identificación del comprador (o del sujeto retenido en una retención).';
comment on column comprobante.comprador_razon_social is 'Razón social o nombres del comprador.';
comment on column comprobante.comprador_direccion is 'Dirección del comprador. Opcional.';
comment on column comprobante.subtotal_sin_impuestos is 'Suma de los valores de las líneas antes de impuestos y después de descuentos, en la moneda del comprobante.';
comment on column comprobante.total_descuento is 'Suma de los descuentos de todas las líneas.';
comment on column comprobante.importe_total is 'Valor total del comprobante: subtotal sin impuestos más impuestos.';
comment on column comprobante.moneda is 'Moneda del comprobante; por defecto DOLAR.';
comment on column comprobante.estado is 'Estado del comprobante: BORRADOR, FIRMADO, FIRMA_FALLIDA, AUTORIZADO, NO_AUTORIZADO, NOTIFICADO, ANULADO o STOCK_INSUFICIENTE.';
comment on column comprobante.xml_generado is 'XML del comprobante sin firmar, validado contra el XSD del SRI.';
comment on column comprobante.xml_firmado is 'XML firmado con XAdES-BES por firma-electronica-service. Vacío hasta que se firma.';
comment on column comprobante.xml_autorizado is 'XML con la autorización del SRI; es el que se entrega al comprador. Vacío hasta la autorización.';
comment on column comprobante.numero_autorizacion is 'Número de autorización que devuelve el SRI (49 dígitos).';
comment on column comprobante.fecha_autorizacion is 'Fecha y hora en que el SRI autorizó el comprobante.';
comment on column comprobante.motivo_rechazo is 'Mensaje del SRI cuando rechaza o no autoriza el comprobante (hasta 500 caracteres).';
comment on column comprobante.ride_pdf is 'RIDE (representación impresa del comprobante) en PDF, generado al autorizarse.';
comment on column comprobante.creado_en is 'Fecha y hora de creación del registro.';
comment on column comprobante.actualizado_en is 'Fecha y hora de la última modificación del registro.';
comment on column comprobante.direccion_establecimiento is 'Dirección del establecimiento emisor (dirEstablecimiento del XML). Opcional.';
comment on column comprobante.doc_modificado_tipo is 'Nota de crédito/débito: código del tipo del comprobante que se modifica (p. ej. 01 factura).';
comment on column comprobante.doc_modificado_numero is 'Nota de crédito/débito: número del comprobante que se modifica (formato 001-001-000000001).';
comment on column comprobante.doc_modificado_fecha is 'Nota de crédito/débito: fecha de emisión del comprobante que se modifica.';
comment on column comprobante.periodo_fiscal is 'Retención: período fiscal en formato MM/AAAA.';
comment on column comprobante.transportista_razon_social is 'Guía de remisión: razón social del transportista.';
comment on column comprobante.transportista_tipo_identificacion is 'Guía de remisión: código del tipo de identificación del transportista.';
comment on column comprobante.transportista_ruc is 'Guía de remisión: identificación (RUC o cédula) del transportista.';
comment on column comprobante.dir_partida is 'Guía de remisión: dirección de partida del traslado.';
comment on column comprobante.fecha_ini_transporte is 'Guía de remisión: fecha de inicio del transporte.';
comment on column comprobante.fecha_fin_transporte is 'Guía de remisión: fecha de fin del transporte.';
comment on column comprobante.placa is 'Guía de remisión: placa del vehículo.';
comment on column comprobante.motivo is 'Nota de crédito: motivo de la modificación (el XSD del SRI lo exige).';
comment on column comprobante.comprador_correo is 'Correo del comprador al que se envía el RIDE y el XML autorizado. Opcional.';
comment on column comprobante.campos_adicionales_json is 'Campos adicionales del comprobante (pares nombre/valor) como arreglo JSON.';
comment on column comprobante.empresa_proveedor_ruc is 'RUC del proveedor del sistema de facturación; si se informa, va en el XML y en el RIDE. Opcional.';

comment on table comprobante_pago is 'Formas de pago de un comprobante (una fila por pago).';
comment on column comprobante_pago.id is 'Identificador único del pago (UUID).';
comment on column comprobante_pago.comprobante_id is 'Comprobante al que pertenece el pago.';
comment on column comprobante_pago.orden is 'Posición del pago dentro del comprobante (única por comprobante).';
comment on column comprobante_pago.forma_pago is 'Código de la forma de pago (tabla forma_pago, Tabla 24 del SRI).';
comment on column comprobante_pago.total is 'Valor pagado con esa forma de pago.';
comment on column comprobante_pago.plazo is 'Plazo del crédito, en la unidad de unidad_tiempo. Se informa junto con unidad_tiempo.';
comment on column comprobante_pago.unidad_tiempo is 'Unidad del plazo, texto de hasta 10 caracteres (p. ej. dias). Se informa junto con plazo.';

comment on table configuracion_empresa_local is 'Réplica local de la configuración por empresa de firma-electronica-service (por ejemplo ride.color y ride.logo), actualizada por eventos de Kafka.';
comment on column configuracion_empresa_local.id is 'Identificador único de la fila (UUID).';
comment on column configuracion_empresa_local.ruc is 'RUC de la empresa a la que pertenece la configuración.';
comment on column configuracion_empresa_local.clave is 'Nombre de la configuración (p. ej. ride.color, ride.logo).';
comment on column configuracion_empresa_local.valor is 'Valor de la configuración; ride.logo va en base64.';
comment on column configuracion_empresa_local.actualizado_en is 'Fecha y hora de la última actualización recibida.';

comment on table destinatario_guia_remision is 'Destinatario de una guía de remisión; cada línea de la guía se asocia a uno.';
comment on column destinatario_guia_remision.id is 'Identificador único del destinatario (UUID).';
comment on column destinatario_guia_remision.comprobante_id is 'Guía de remisión a la que pertenece.';
comment on column destinatario_guia_remision.identificacion is 'Identificación del destinatario.';
comment on column destinatario_guia_remision.razon_social is 'Razón social del destinatario.';
comment on column destinatario_guia_remision.direccion is 'Dirección de destino.';
comment on column destinatario_guia_remision.motivo_traslado is 'Motivo del traslado (venta, compra, transformación, etc.).';
comment on column destinatario_guia_remision.doc_aduanero_unico is 'Número del documento aduanero único (DAU), si aplica.';
comment on column destinatario_guia_remision.cod_estab_destino is 'Código del establecimiento de destino, si es una empresa propia.';
comment on column destinatario_guia_remision.ruta is 'Ruta del traslado. Opcional.';
comment on column destinatario_guia_remision.cod_doc_sustento is 'Código del tipo de comprobante de sustento (p. ej. 01 factura).';
comment on column destinatario_guia_remision.num_doc_sustento is 'Número del comprobante de sustento (formato 001-001-000000001).';
comment on column destinatario_guia_remision.num_aut_doc_sustento is 'Número de autorización del comprobante de sustento.';
comment on column destinatario_guia_remision.fecha_emision_doc_sustento is 'Fecha de emisión del comprobante de sustento.';

comment on table detalle_comprobante is 'Línea (producto o servicio) de un comprobante.';
comment on column detalle_comprobante.id is 'Identificador único de la línea (UUID).';
comment on column detalle_comprobante.comprobante_id is 'Comprobante al que pertenece la línea.';
comment on column detalle_comprobante.item_id is 'Ítem de inventario-service que origina la línea; se usa para reservar y descontar stock. Vacío si la línea no descuenta inventario.';
comment on column detalle_comprobante.codigo_principal is 'Código principal del producto o servicio, copiado al emitir.';
comment on column detalle_comprobante.codigo_auxiliar is 'Código auxiliar del producto o servicio. Opcional.';
comment on column detalle_comprobante.descripcion is 'Descripción de la línea, copiada al emitir.';
comment on column detalle_comprobante.cantidad is 'Cantidad facturada (mayor que cero).';
comment on column detalle_comprobante.precio_unitario is 'Precio unitario sin impuestos (cero o mayor).';
comment on column detalle_comprobante.descuento is 'Descuento aplicado a la línea.';
comment on column detalle_comprobante.precio_total_sin_impuesto is 'Valor de la línea sin impuestos: cantidad por precio unitario menos descuento.';
comment on column detalle_comprobante.destinatario_id is 'Guía de remisión: destinatario al que va la línea.';

comment on table detalle_impuesto is 'Impuesto aplicado a una línea (IVA, ICE, IRBPNR) o, en una retención, impuesto retenido.';
comment on column detalle_impuesto.id is 'Identificador único del impuesto de la línea (UUID).';
comment on column detalle_impuesto.detalle_id is 'Línea a la que se aplica el impuesto.';
comment on column detalle_impuesto.orden is 'Posición del impuesto dentro de la línea (única por línea).';
comment on column detalle_impuesto.codigo is 'Código del impuesto (tabla impuesto: 2 IVA, 3 ICE, 5 IRBPNR) o, en una retención, del impuesto retenido (tabla impuesto_retencion).';
comment on column detalle_impuesto.codigo_porcentaje is 'Código de la tarifa del impuesto (tabla tarifa_impuesto); en una retención, código de retención.';
comment on column detalle_impuesto.tarifa is 'Porcentaje de la tarifa aplicada (p. ej. 15 para IVA 15 %).';
comment on column detalle_impuesto.base_imponible is 'Base sobre la que se calcula el impuesto.';
comment on column detalle_impuesto.valor is 'Valor del impuesto: base imponible por tarifa.';
comment on column detalle_impuesto.codigo_retencion is 'Retención: código de la retención aplicada (tabla retencion).';
comment on column detalle_impuesto.doc_sustento_codigo is 'Retención: código del tipo de comprobante de sustento.';
comment on column detalle_impuesto.doc_sustento_numero is 'Retención: número del comprobante de sustento.';
comment on column detalle_impuesto.doc_sustento_fecha is 'Retención: fecha de emisión del comprobante de sustento.';

comment on table establecimiento_local is 'Réplica local de los establecimientos de cada empresa (desde firma-electronica-service); sirve para validar el establecimiento al emitir.';
comment on column establecimiento_local.id is 'Identificador único de la fila (UUID).';
comment on column establecimiento_local.ruc is 'RUC de la empresa dueña del establecimiento.';
comment on column establecimiento_local.codigo is 'Código del establecimiento (3 dígitos).';
comment on column establecimiento_local.activo is 'Indica si el establecimiento está activo; solo se emite con establecimientos activos.';
comment on column establecimiento_local.actualizado_en is 'Fecha y hora de la última actualización recibida.';

comment on table forma_pago is 'Catálogo de formas de pago (Tabla 24 de la ficha técnica del SRI), con vigencia.';
comment on column forma_pago.codigo is 'Código SRI de la forma de pago (2 dígitos, p. ej. 01 sin utilización del sistema financiero).';
comment on column forma_pago.descripcion is 'Nombre de la forma de pago.';
comment on column forma_pago.vigente_desde is 'Fecha desde la que el código es válido.';
comment on column forma_pago.vigente_hasta is 'Fecha hasta la que el código es válido; vacío si sigue vigente.';
comment on column forma_pago.activo is 'Indica si el código se acepta al emitir.';
comment on column forma_pago.actualizado_en is 'Fecha y hora de la última modificación del catálogo.';

comment on table impuesto is 'Catálogo de impuestos del SRI (Tabla 16): IVA, ICE e IRBPNR.';
comment on column impuesto.codigo is 'Código SRI del impuesto (2 = IVA, 3 = ICE, 5 = IRBPNR).';
comment on column impuesto.descripcion is 'Nombre del impuesto.';
comment on column impuesto.activo is 'Indica si el impuesto se acepta al emitir.';
comment on column impuesto.actualizado_en is 'Fecha y hora de la última modificación.';

comment on table impuesto_retencion is 'Catálogo de impuestos que se retienen (renta, IVA, ISD).';
comment on column impuesto_retencion.codigo is 'Código SRI del impuesto retenido (1 renta, 2 IVA, 6 ISD).';
comment on column impuesto_retencion.descripcion is 'Nombre del impuesto retenido.';
comment on column impuesto_retencion.activo is 'Indica si se acepta al emitir retenciones.';
comment on column impuesto_retencion.actualizado_en is 'Fecha y hora de la última modificación.';

comment on table item_local is 'Réplica local de los ítems de inventario-service (desde eventos de Kafka); permite armar las líneas del comprobante sin llamar al inventario.';
comment on column item_local.id is 'Identificador del ítem; el mismo de inventario-service.';
comment on column item_local.codigo_principal is 'Código principal del ítem.';
comment on column item_local.descripcion is 'Descripción del ítem.';
comment on column item_local.codigo_impuesto is 'Código del impuesto que grava al ítem (2 = IVA).';
comment on column item_local.codigo_porcentaje is 'Código de la tarifa de IVA del ítem (tabla tarifa_impuesto).';
comment on column item_local.tiene_ice is 'Indica si el ítem lleva ICE.';
comment on column item_local.codigo_porcentaje_ice is 'Código de la tarifa de ICE; solo si tiene_ice es verdadero.';
comment on column item_local.tarifa_ice is 'Tarifa de ICE; solo si tiene_ice es verdadero.';
comment on column item_local.activo is 'Indica si el ítem está activo.';
comment on column item_local.actualizado_en is 'Fecha y hora de la última actualización recibida.';
comment on column item_local.ruc is 'RUC de la empresa dueña del ítem.';

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

comment on table punto_emision_local is 'Réplica local de los puntos de emisión de cada establecimiento (desde firma-electronica-service).';
comment on column punto_emision_local.id is 'Identificador único de la fila (UUID).';
comment on column punto_emision_local.ruc is 'RUC de la empresa dueña del punto de emisión.';
comment on column punto_emision_local.establecimiento_codigo is 'Código del establecimiento al que pertenece (3 dígitos).';
comment on column punto_emision_local.codigo is 'Código del punto de emisión (3 dígitos).';
comment on column punto_emision_local.activo is 'Indica si el punto de emisión está activo; solo se emite con puntos activos.';
comment on column punto_emision_local.actualizado_en is 'Fecha y hora de la última actualización recibida.';

comment on table retencion is 'Catálogo de códigos de retención (Tabla 20 del SRI) con su porcentaje y vigencia.';
comment on column retencion.impuesto_codigo is 'Impuesto retenido al que pertenece el código (tabla impuesto_retencion).';
comment on column retencion.codigo is 'Código de la retención (p. ej. 303, 312, 725).';
comment on column retencion.vigente_desde is 'Fecha desde la que el código es válido.';
comment on column retencion.descripcion is 'Concepto de la retención.';
comment on column retencion.porcentaje is 'Porcentaje de retención (de 0 a 100).';
comment on column retencion.vigente_hasta is 'Fecha hasta la que es válido; vacío si sigue vigente.';
comment on column retencion.activo is 'Indica si el código se acepta al emitir.';
comment on column retencion.actualizado_en is 'Fecha y hora de la última modificación.';

comment on table secuencial_comprobante is 'Último secuencial emitido por RUC, tipo de comprobante, establecimiento y punto de emisión. No se borra al reiniciar la base: el SRI recuerda los secuenciales ya registrados y rechaza los repetidos con ERROR SECUENCIAL REGISTRADO.';
comment on column secuencial_comprobante.id is 'Identificador único de la fila (UUID).';
comment on column secuencial_comprobante.tipo_comprobante is 'Tipo de comprobante al que corresponde el contador.';
comment on column secuencial_comprobante.establecimiento is 'Código del establecimiento (3 dígitos).';
comment on column secuencial_comprobante.punto_emision is 'Código del punto de emisión (3 dígitos).';
comment on column secuencial_comprobante.ultimo_secuencial is 'Último número usado; el siguiente comprobante toma este valor más uno.';
comment on column secuencial_comprobante.ruc is 'RUC de la empresa emisora.';

comment on table sustento_tributario is 'Catálogo de sustentos tributarios (Tabla 5 del SRI) para retenciones y liquidaciones de compra.';
comment on column sustento_tributario.codigo is 'Código SRI del sustento tributario (2 dígitos).';
comment on column sustento_tributario.descripcion is 'Descripción del sustento.';
comment on column sustento_tributario.activo is 'Indica si el código se acepta al emitir.';
comment on column sustento_tributario.actualizado_en is 'Fecha y hora de la última modificación.';

comment on table tarifa_impuesto is 'Catálogo de tarifas por impuesto (IVA 0 %, 15 %, etc.) con vigencia.';
comment on column tarifa_impuesto.impuesto_codigo is 'Impuesto al que pertenece la tarifa (tabla impuesto).';
comment on column tarifa_impuesto.codigo_porcentaje is 'Código SRI de la tarifa (p. ej. 0 = 0 %, 4 = 15 % para IVA).';
comment on column tarifa_impuesto.descripcion is 'Nombre de la tarifa.';
comment on column tarifa_impuesto.tarifa is 'Porcentaje de la tarifa (cero o mayor).';
comment on column tarifa_impuesto.vigente_desde is 'Fecha desde la que la tarifa es válida.';
comment on column tarifa_impuesto.vigente_hasta is 'Fecha hasta la que es válida; vacío si sigue vigente.';
comment on column tarifa_impuesto.activo is 'Indica si la tarifa se acepta al emitir.';
comment on column tarifa_impuesto.actualizado_en is 'Fecha y hora de la última modificación.';

comment on table tipo_identificacion is 'Catálogo de tipos de identificación del comprador (Tabla 6 del SRI).';
comment on column tipo_identificacion.codigo is 'Código SRI del tipo de identificación (04 RUC, 05 cédula, 06 pasaporte, 07 consumidor final, 08 exterior).';
comment on column tipo_identificacion.descripcion is 'Nombre del tipo de identificación.';
comment on column tipo_identificacion.solo_factura is 'Indica si el tipo solo se permite en facturas (p. ej. consumidor final).';
comment on column tipo_identificacion.activo is 'Indica si el tipo se acepta al emitir.';
comment on column tipo_identificacion.actualizado_en is 'Fecha y hora de la última modificación.';

