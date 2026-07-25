-- El XSD real del SRI para Nota de Crédito exige <motivo> como último hijo
-- de <infoNotaCredito> — confirmado con un rechazo real de Recepción
-- (cvc-complex-type.2.4.b: "The content of element 'infoNotaCredito' is
-- not complete. One of '{motivo}' is expected."). La ficha técnica en
-- texto no lo dejaba claro como obligatorio; el propio WSDL sí. Nullable
-- porque solo lo usa Nota de Crédito.
alter table comprobante add column motivo varchar(300);
