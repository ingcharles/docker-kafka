package com.facturacion.firma.application.dto;

public class ActualizarPuntoEmisionRequest {

    private String descripcion;

    private boolean activo = true;

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
