package com.facturacion.notificacion.domain.model.vo;

/** Lo que se envía: asunto, HTML completo (con html/head/body) y texto plano. */
public record ContenidoCorreo(String asunto, String html, String texto) {
}
