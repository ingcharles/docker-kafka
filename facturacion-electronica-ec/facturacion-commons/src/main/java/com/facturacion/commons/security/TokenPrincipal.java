package com.facturacion.commons.security;

import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;

import com.facturacion.commons.error.ErrorComun;
import com.facturacion.commons.exception.SinPermisoException;

/** Poblado por {@link ApiKeyAuthFilter} — datos del api-key ya verificado para este request. */
@RequestScoped
public class TokenPrincipal {

    /** Roles que emite identidad-service: ADMIN gestiona, OPERADOR emite, LECTURA solo consulta. */
    private static final String ROL_ADMIN = "ADMIN";
    private static final String ROL_LECTURA = "LECTURA";
    /** Cómo se muestra el rol de una api-key que no trae ninguno. */
    private static final String SIN_ROL = "sin rol";

    private UUID usuarioId;
    private String ruc;
    private String rol;
    private String establecimiento;
    private String puntoEmision;

    /** Solo ADMIN y OPERADOR pueden crear o modificar; una api-key LECTURA (o sin rol) solo consulta. */
    public void exigirEscritura() {
        if (rol == null || ROL_LECTURA.equals(rol)) {
            throw new SinPermisoException(ErrorComun.API_KEY_SOLO_LECTURA);
        }
    }

    /** Operaciones de gestión de la empresa (certificado, establecimientos, configuración): solo ADMIN. */
    public void exigirAdmin() {
        if (!ROL_ADMIN.equals(rol)) {
            throw new SinPermisoException(ErrorComun.API_KEY_REQUIERE_ADMIN, rol == null ? SIN_ROL : rol);
        }
    }

    /** Exige que el RUC del recurso sea el de la empresa de esta api-key; si no, 403 con el RUC de cada una. */
    public void exigirEmpresa(String rucDelRecurso) {
        if (!esDeLaEmpresa(rucDelRecurso)) {
            throw new SinPermisoException(ErrorComun.API_KEY_OTRO_RUC, ruc, rucDelRecurso);
        }
    }

    /**
     * Además del RUC, si la api-key está acotada a un establecimiento y/o un punto de emisión, el comprobante a emitir debe
     * ser de ese mismo establecimiento y punto. Una api-key sin esa restricción (el caso normal) no se ve afectada.
     */
    public void exigirAlcance(String rucDelRecurso, String establecimientoDelRecurso, String puntoEmisionDelRecurso) {
        exigirEmpresa(rucDelRecurso);
        if (establecimiento != null && !establecimiento.equals(establecimientoDelRecurso)) {
            throw new SinPermisoException(ErrorComun.API_KEY_OTRO_ESTABLECIMIENTO, establecimiento,
                establecimientoDelRecurso);
        }
        if (puntoEmision != null && !puntoEmision.equals(puntoEmisionDelRecurso)) {
            throw new SinPermisoException(ErrorComun.API_KEY_OTRO_PUNTO_EMISION, puntoEmision, puntoEmisionDelRecurso);
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
