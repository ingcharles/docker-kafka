package com.facturacion.identidad.shared.error;

import com.facturacion.commons.error.ErrorCatalogo;

/**
 * Catálogo de errores HTTP propios de identidad-service (prefijo {@code IDE}); los genéricos (404, 401, 409) son
 * {@code ErrorComun}. El texto de cada uno está en {@code mensajes.properties}.
 */
public enum IdentidadError implements ErrorCatalogo {

    CREDENCIALES_INVALIDAS("IDE-401-001", 401, "credenciales.invalidas"),
    ROL_INSUFICIENTE("IDE-403-001", 403, "membresia.rol_insuficiente"),
    SIN_ASOCIACION("IDE-403-003", 403, "membresia.sin_asociacion"),
    EMAIL_DUPLICADO("IDE-409-001", 409, "usuario.email_duplicado");

    private final String codigo;
    private final int estadoHttp;
    private final String claveMensaje;

    IdentidadError(String codigo, int estadoHttp, String claveMensaje) {
        this.codigo = codigo;
        this.estadoHttp = estadoHttp;
        this.claveMensaje = claveMensaje;
    }

    @Override
    public String codigo() {
        return codigo;
    }

    @Override
    public int estadoHttp() {
        return estadoHttp;
    }

    @Override
    public String claveMensaje() {
        return claveMensaje;
    }
}
