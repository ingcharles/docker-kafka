package com.facturacion.orquestacion.shared.error;

import com.facturacion.commons.error.ErrorCatalogo;

/**
 * Catálogo de errores HTTP propios de facturacion-orchestrator-service (prefijo {@code ORQ}); los genéricos son {@code ErrorComun}.
 * El texto de cada uno está en {@code mensajes.properties}.
 */
public enum OrquestacionError implements ErrorCatalogo {

    SAGA_NO_ENCONTRADA("ORQ-404-001", 404, "saga.no_encontrada"),
    PAYLOAD_SAGA_NO_LEIBLE("ORQ-500-001", 500, "saga.payload_no_leible"),
    SAGA_SIN_COMANDO_DE_FIRMA_PENDIENTE("ORQ-409-001", 409, "saga.sin_comando_firma_pendiente");

    private final String codigo;
    private final int estadoHttp;
    private final String claveMensaje;

    OrquestacionError(String codigo, int estadoHttp, String claveMensaje) {
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
