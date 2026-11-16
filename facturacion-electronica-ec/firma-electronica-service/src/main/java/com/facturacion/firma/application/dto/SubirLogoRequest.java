package com.facturacion.firma.application.dto;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/** Formulario multipart de {@code POST /empresas/{id}/logo}: el campo {@code archivo} es una imagen PNG o JPEG. */
public class SubirLogoRequest {

    @RestForm("archivo")
    public FileUpload archivo;
}
