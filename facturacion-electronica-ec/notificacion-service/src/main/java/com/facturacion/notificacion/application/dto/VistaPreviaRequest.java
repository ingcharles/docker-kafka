package com.facturacion.notificacion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.facturacion.notificacion.domain.model.entity.PlantillaCorreoEmpresa;

/**
 * Una plantilla para ver cómo quedaría, sin guardarla: se arma con datos de ejemplo, el logo real de la empresa y las mismas
 * reglas que al guardar.
 *
 * @param tipoComprobante tipo de comprobante de ejemplo ({@code FACTURA}, {@code NOTA_CREDITO}...)
 */
public record VistaPreviaRequest(
    @NotBlank String tipoComprobante,
    @NotBlank @Size(max = PlantillaCorreoEmpresa.ASUNTO_MAXIMO) String asunto,
    @NotBlank @Size(max = PlantillaCorreoEmpresa.HTML_MAXIMO) String cuerpoHtml,
    @Size(max = PlantillaCorreoEmpresa.TEXTO_MAXIMO) String cuerpoTexto) {
}
