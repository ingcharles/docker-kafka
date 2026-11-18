package com.facturacion.sri.shared.error;

import com.facturacion.commons.error.ErrorCatalogo;

/**
 * Catálogo de errores HTTP propios de sri-conector-service (prefijo {@code SRI}); los genéricos son {@code ErrorComun}.
 * El texto de cada uno está en {@code mensajes.properties}.
 */
public enum SriError implements ErrorCatalogo {

    ENVIO_NO_ENCONTRADO_POR_COMPROBANTE("SRI-404-001", 404, "envio.no_encontrado_por_comprobante"),
    ENVIO_NO_ENCONTRADO_POR_CLAVE("SRI-404-002", 404, "envio.no_encontrado_por_clave"),
    WS_RECEPCION_NO_DISPONIBLE("SRI-502-001", 502, "ws.recepcion_no_disponible"),
    WS_AUTORIZACION_NO_DISPONIBLE("SRI-502-002", 502, "ws.autorizacion_no_disponible"),
    COMANDO_ENVIO_INVALIDO("SRI-400-001", 400, "comando.envio_invalido"),
    ENVIO_NO_ENCONTRADO_POR_ID("SRI-404-003", 404, "envio.no_encontrado_por_id");

    private final String codigo;
    private final int estadoHttp;
    private final String claveMensaje;

    SriError(String codigo, int estadoHttp, String claveMensaje) {
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
