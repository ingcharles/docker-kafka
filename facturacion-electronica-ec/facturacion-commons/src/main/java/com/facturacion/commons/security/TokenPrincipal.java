package com.facturacion.commons.security;

import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;

import com.facturacion.commons.exception.SinPermisoException;

/** Poblado por {@link ApiKeyAuthFilter} — datos del api-key ya verificado para este request. */
@RequestScoped
public class TokenPrincipal {

    /** Roles que emite identidad-service: ADMIN gestiona, OPERADOR emite, LECTURA solo consulta. */
    private static final String ROL_ADMIN = "ADMIN";
    private static final String ROL_LECTURA = "LECTURA";

    private UUID usuarioId;
    private String ruc;
    private String rol;
    private String establecimiento;
    private String puntoEmision;

    /** Solo ADMIN y OPERADOR pueden crear o modificar; una api-key LECTURA (o sin rol) solo consulta. */
    public void exigirEscritura() {
        if (rol == null || ROL_LECTURA.equals(rol)) {
            throw new SinPermisoException("La api-key tiene rol LECTURA: solo puede consultar, no crear ni modificar.");
        }
    }

    /** Operaciones de gestión de la empresa (certificado, establecimientos, configuración): solo ADMIN. */
    public void exigirAdmin() {
        if (!ROL_ADMIN.equals(rol)) {
            throw new SinPermisoException("Esta operación requiere una api-key con rol ADMIN (la actual es "
                + (rol == null ? "sin rol" : rol) + ").");
        }
    }

    /** true si el recurso, identificado por el RUC de su empresa, es de la empresa de esta api-key. */
    public boolean esDeLaEmpresa(String rucDelRecurso) {
        return ruc != null && ruc.equals(rucDelRecurso);
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
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

    /** {@code null} = api-key sin restricción de establecimiento (comportamiento por defecto). */
    public String getEstablecimiento() {
        return establecimiento;
    }

    public void setEstablecimiento(String establecimiento) {
        this.establecimiento = establecimiento;
    }

    /** {@code null} = api-key sin restricción de punto de emisión. */
    public String getPuntoEmision() {
        return puntoEmision;
    }

    public void setPuntoEmision(String puntoEmision) {
        this.puntoEmision = puntoEmision;
    }
}
