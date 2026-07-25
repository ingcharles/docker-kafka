package com.facturacion.comprobantes.domain.model.vo;

import java.math.BigDecimal;

/**
 * Un impuesto aplicado a una línea de detalle (IVA, ICE, etc.) — Tabla 16/18
 * de la ficha técnica del SRI: {@code codigo} identifica el tipo de
 * impuesto (2=IVA, 3=ICE, 5=IRBPNR), {@code codigoPorcentaje} identifica la
 * tarifa exacta dentro de ese impuesto (Tabla 17 para IVA).
 * <p>
 * Los últimos cuatro campos ({@code codigoRetencion} en adelante) solo se
 * usan en Comprobante de Retención — ahí cada "impuesto" es en realidad una
 * línea de retención, que además de la tarifa necesita el código de
 * retención específico (Tabla 20/21) y una referencia al documento sustento
 * (la factura/comprobante sobre el que se retiene). Quedan en {@code null}
 * para Factura/Nota de Crédito/Nota de Débito/Liquidación de Compra.
 * {@code docSustentoFecha} va como texto dd/mm/aaaa (igual que el resto de
 * fechas del XML del SRI) y no como {@code Instant}, para no depender del
 * módulo jsr310 en el {@code ObjectMapper} local que serializa este VO a
 * JSON en {@code impuestos_json} (ver DetalleComprobanteMapper).
 */
public record Impuesto(
    String codigo,
    String codigoPorcentaje,
    BigDecimal tarifa,
    BigDecimal baseImponible,
    BigDecimal valor,
    String codigoRetencion,
    String docSustentoCodigo,
    String docSustentoNumero,
    String docSustentoFecha) {

    /** Constructor de conveniencia para los 5 tipos que no son Retención. */
    public Impuesto(String codigo, String codigoPorcentaje, BigDecimal tarifa, BigDecimal baseImponible,
            BigDecimal valor) {
        this(codigo, codigoPorcentaje, tarifa, baseImponible, valor, null, null, null, null);
    }
}
