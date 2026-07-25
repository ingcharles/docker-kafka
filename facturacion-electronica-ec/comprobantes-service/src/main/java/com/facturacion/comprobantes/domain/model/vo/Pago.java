package com.facturacion.comprobantes.domain.model.vo;

import java.math.BigDecimal;

/**
 * Una forma de pago del comprobante (Tabla 24 de la ficha técnica del SRI:
 * {@code formaPago} identifica el medio — 01=SIN UTILIZACION DEL SISTEMA
 * FINANCIERO, 20=OTROS CON UTILIZACION DEL SISTEMA FINANCIERO, etc.).
 * {@code plazo}/{@code unidadTiempo} son opcionales (solo aplican a crédito).
 * Solo Factura, Nota de Débito y Liquidación de Compra llevan {@code <pagos>}
 * — Nota de Crédito, Guía de Remisión y Retención no, según el propio XSD
 * del SRI.
 */
public record Pago(String formaPago, BigDecimal total, String plazo, String unidadTiempo) {
}
