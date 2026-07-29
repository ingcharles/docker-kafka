package com.facturacion.comprobantes.domain.model.vo;

/**
 * Una línea de {@code <infoAdicional><campoAdicional nombre="...">valor</campoAdicional></infoAdicional>}
 * — información adicional libre que la empresa quiera incluir en el
 * comprobante (teléfono, email, orden de compra del comprador, etc.), Anexo
 * de la ficha técnica del SRI. No confundir con el campo "granContribuyente"
 * (ver {@code XmlComprobanteUtil.construirInfoAdicionalGranContribuyente}),
 * que es obligatorio y calculado por el sistema, no algo que el llamador
 * mande — ambos terminan en el mismo {@code <infoAdicional>}.
 */
public record CampoAdicional(String nombre, String valor) {
}
