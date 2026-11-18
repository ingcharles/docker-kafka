package com.facturacion.notificacion.shared.error;

import com.facturacion.commons.error.ErrorCatalogo;

/**
 * Catálogo de errores HTTP propios de notificacion-service (prefijo {@code NOT}); los genéricos son {@code ErrorComun}.
 * El texto de cada uno está en {@code mensajes.properties}.
 */
public enum NotificacionError implements ErrorCatalogo {

    NOTIFICACION_NO_ENCONTRADA("NOT-404-001", 404, "notificacion.no_encontrada");

    private final String codigo;
    private final int estadoHttp;
    private final String claveMensaje;

    NotificacionError(String codigo, int estadoHttp, String claveMensaje) {
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
