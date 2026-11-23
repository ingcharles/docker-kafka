package com.facturacion.notificacion.application.dto;

/** La plantilla por defecto de un tipo, con las variables sin reemplazar, para tomarla de punto de partida. */
public record PlantillaPorDefectoResponse(String tipoComprobante, String asunto, String cuerpoHtml, String cuerpoTexto) {
}
