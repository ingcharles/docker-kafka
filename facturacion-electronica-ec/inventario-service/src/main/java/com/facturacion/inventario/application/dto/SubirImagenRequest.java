package com.facturacion.inventario.application.dto;

import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/** Formulario multipart de {@code POST /items/{id}/imagenes} — sigue la convención de sufijo "Request" como cualquier otro DTO de entrada, aunque se enlace vía {@code @RestForm} en vez de JSON. */
public class SubirImagenRequest {

    @RestForm("archivo")
    public FileUpload archivo;

    @RestForm("esPrincipal")
    @PartType(MediaType.TEXT_PLAIN)
    public boolean esPrincipal;

    @RestForm("orden")
    @PartType(MediaType.TEXT_PLAIN)
    public int orden;
}
