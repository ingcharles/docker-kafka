package com.facturacion.identidad.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Asocia al usuario autenticado con un ruc. Solo es posible mientras la empresa NO tenga ningún miembro (el
 * arranque: reclamar una empresa recién creada sin dueño); si ya tiene administradores, el acceso lo da uno de ellos
 * ({@code POST /empresas/{ruc}/usuarios}). El servicio exige además que el ruc corresponda a una Empresa real y
 * activa en firma-electronica-service (ver {@code UsuarioService.asociarEmpresa} y {@code EmpresaLocal}) — un 404
 * significa que ese ruc todavía no tiene Empresa dado de alta.
 */
public class AsociarEmpresaRequest {

    @NotBlank
    @Pattern(regexp = "\\d{13}", message = "el RUC debe tener 13 dígitos")
    private String ruc;

    @NotNull
    @Pattern(regexp = "ADMIN|OPERADOR|LECTURA", message = "el rol debe ser ADMIN, OPERADOR o LECTURA")
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
