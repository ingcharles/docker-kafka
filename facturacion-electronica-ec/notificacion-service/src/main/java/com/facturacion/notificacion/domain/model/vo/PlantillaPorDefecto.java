package com.facturacion.notificacion.domain.model.vo;

/**
 * La plantilla por defecto de un tipo de comprobante, escrita con las variables {@code {{...}}} para que una empresa la tome
 * de punto de partida: el cuerpo HTML no incluye html/head/body.
 */
public record PlantillaPorDefecto(String asunto, String cuerpoHtml, String cuerpoTexto) {
}
