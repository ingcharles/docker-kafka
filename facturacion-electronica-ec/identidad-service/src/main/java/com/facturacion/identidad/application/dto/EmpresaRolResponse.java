package com.facturacion.identidad.application.dto;

public class EmpresaRolResponse {

    private String ruc;
    private String rol;

    public EmpresaRolResponse() {
    }

    public EmpresaRolResponse(String ruc, String rol) {
        this.ruc = ruc;
        this.rol = rol;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
