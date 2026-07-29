package com.facturacion.identidad.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Asocia al usuario autenticado con un ruc — sin flujo de invitación ni
 * verificación de *titularidad* real (cualquier usuario puede pedir
 * asociarse a cualquier ruc con cualquier rol), pero el servicio sí exige
 * que el ruc corresponda a un Empresa real y activo en
 * firma-electronica-service (ver {@code UsuarioService.asociarEmpresa} y
 * {@code EmpresaLocal}) — un 404 significa que ese ruc todavía no tiene
 * Empresa dado de alta.
 */
public class AsociarEmpresaRequest {

    @NotBlank
    @Pattern(regexp = "\\d{13}", message = "el RUC debe tener 13 dígitos")
    private String ruc;

    @NotNull
    private String rol;

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
