package com.facturacion.firma.application.dto;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/** Formulario multipart de {@code POST /empresas/{id}/certificado} — sigue la convención de sufijo "Request" como cualquier otro DTO de entrada, aunque se enlace vía {@code @RestForm} en vez de JSON. */
public class SubirCertificadoRequest {

    @RestForm("archivo")
    public FileUpload archivo;

    @RestForm("contrasena")
    public String contrasena;
}
