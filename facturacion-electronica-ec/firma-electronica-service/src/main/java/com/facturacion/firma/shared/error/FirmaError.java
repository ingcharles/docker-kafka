package com.facturacion.firma.shared.error;

import com.facturacion.commons.error.ErrorCatalogo;

/**
 * Catálogo de errores HTTP propios de firma-electronica-service (prefijo {@code FIR}); los genéricos (404, 401, 403)
 * son {@code ErrorComun}. El texto de cada uno está en {@code mensajes.properties}.
 */
public enum FirmaError implements ErrorCatalogo {

    CERTIFICADO_INVALIDO("FIR-400-001", 400, "certificado.invalido"),
    CODIGO_DUPLICADO("FIR-409-001", 409, "codigo.duplicado"),
    RUC_DUPLICADO("FIR-409-002", 409, "empresa.ruc_duplicado");

    private final String codigo;
    private final int estadoHttp;
    private final String claveMensaje;

    FirmaError(String codigo, int estadoHttp, String claveMensaje) {
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
