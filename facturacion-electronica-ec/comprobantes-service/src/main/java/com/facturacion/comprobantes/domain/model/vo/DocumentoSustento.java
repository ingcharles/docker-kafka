package com.facturacion.comprobantes.domain.model.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Un {@code docSustento} del Comprobante de Retención v2.0.0 (estilo ATS) —
 * agrupa todas las líneas de retención de un mismo documento, junto con el
 * impuesto y la forma de pago propios de ESE documento (no de la
 * retención). Solo lo usa Comprobante de Retención; los otros 5 tipos no
 * tienen equivalente. No se persiste (transitorio, solo para generar el
 * XML) — ver {@code Comprobante#getDocumentosSustento()} y
 * {@code ComprobanteService#construirRetencion}.
 */
public record DocumentoSustento(
    String codSustento,
    String docSustentoCodigo,
    String docSustentoNumero,
    String docSustentoFecha,
    String pagoLocExt,
    BigDecimal totalSinImpuestos,
    BigDecimal importeTotal,
    List<ImpuestoDocSustento> impuestos,
    List<Impuesto> retenciones,
    List<Pago> pagos) {
}
