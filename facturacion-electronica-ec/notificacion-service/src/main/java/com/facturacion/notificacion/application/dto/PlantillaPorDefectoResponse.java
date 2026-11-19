package com.facturacion.notificacion.application.dto;

import com.facturacion.notificacion.domain.model.vo.PlantillaPorDefecto;

/** La plantilla por defecto de un tipo, con las variables sin reemplazar, para tomarla de punto de partida. */
public record PlantillaPorDefectoResponse(String tipoComprobante, String asunto, String cuerpoHtml, String cuerpoTexto) {

    public static PlantillaPorDefectoResponse desde(String tipoComprobante, PlantillaPorDefecto plantilla) {
        return new PlantillaPorDefectoResponse(tipoComprobante, plantilla.asunto(), plantilla.cuerpoHtml(),
            plantilla.cuerpoTexto());
    }
}
