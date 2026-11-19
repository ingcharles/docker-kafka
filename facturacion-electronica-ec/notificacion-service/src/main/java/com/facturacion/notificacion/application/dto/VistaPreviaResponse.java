package com.facturacion.notificacion.application.dto;

/**
 * Cómo se vería el correo con datos de ejemplo. En el {@code html} el logo va como imagen incrustada en la propia página (el
 * correo real lo lleva como imagen incrustada en el mensaje), para que un navegador lo muestre.
 *
 * @param tieneLogo si la empresa tiene logo; si no, donde la plantilla pone {@code {{logo}}} sale el nombre de la empresa
 */
public record VistaPreviaResponse(String asunto, String html, String texto, boolean tieneLogo) {
}
