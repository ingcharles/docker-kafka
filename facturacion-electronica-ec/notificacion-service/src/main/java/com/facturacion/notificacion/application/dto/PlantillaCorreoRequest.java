package com.facturacion.notificacion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.facturacion.notificacion.domain.model.entity.PlantillaCorreoEmpresa;

/**
 * Lo que una empresa escribe para su plantilla de correo.
 *
 * @param asunto      una sola línea; admite variables {@code {{numero}}}, {@code {{tipoComprobante}}}...
 * @param cuerpoHtml  el cuerpo del correo en HTML, sin {@code html}/{@code head}/{@code body} (el servicio lo envuelve); se
 *                    limpia al guardar
 * @param cuerpoTexto versión en texto plano para clientes sin HTML; opcional (si falta, se deriva del HTML al enviar)
 * @param activa      {@code false} deja la plantilla guardada pero sin uso; por defecto {@code true}
 */
public record PlantillaCorreoRequest(
    @NotBlank @Size(max = PlantillaCorreoEmpresa.ASUNTO_MAXIMO) String asunto,
    @NotBlank @Size(max = PlantillaCorreoEmpresa.HTML_MAXIMO) String cuerpoHtml,
    @Size(max = PlantillaCorreoEmpresa.TEXTO_MAXIMO) String cuerpoTexto,
    Boolean activa) {
}
