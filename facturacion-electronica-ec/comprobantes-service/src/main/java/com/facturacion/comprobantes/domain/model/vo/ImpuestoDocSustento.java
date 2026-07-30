package com.facturacion.comprobantes.domain.model.vo;

import java.math.BigDecimal;

/**
 * El impuesto del documento sustento de una retención (XSD v2.0.0,
 * {@code impuestosDocSustento/impuestoDocSustento}) — el IVA/ICE que ya
 * traía la factura/comprobante sobre el que se retiene, no el valor
 * retenido en sí (eso sigue siendo {@link Impuesto}). Solo lo usa
 * Comprobante de Retención; ver {@code GeneradorXmlRetencion}.
 */
public record ImpuestoDocSustento(
    String codigo,
    String codigoPorcentaje,
    BigDecimal baseImponible,
    BigDecimal tarifa,
    BigDecimal valor) {
}
