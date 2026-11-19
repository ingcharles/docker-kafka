package com.facturacion.notificacion.shared.error;

import com.facturacion.commons.error.ErrorCatalogo;

/**
 * Catálogo de errores HTTP propios de notificacion-service (prefijo {@code NOT}); los genéricos son {@code ErrorComun}.
 * El texto de cada uno está en {@code mensajes.properties}.
 */
public enum NotificacionError implements ErrorCatalogo {

    PLANTILLA_TIPO_INVALIDO("NOT-400-001", 400, "plantilla.tipo_invalido"),
    PLANTILLA_VARIABLE_DESCONOCIDA("NOT-400-002", 400, "plantilla.variable_desconocida"),
    PLANTILLA_VARIABLE_EN_ETIQUETA("NOT-400-003", 400, "plantilla.variable_en_etiqueta"),
    PLANTILLA_ASUNTO_MULTILINEA("NOT-400-004", 400, "plantilla.asunto_multilinea"),
    PLANTILLA_LOGO_EN_ASUNTO("NOT-400-005", 400, "plantilla.logo_en_asunto"),
    PLANTILLA_SIN_CONTENIDO("NOT-400-006", 400, "plantilla.sin_contenido"),
    NOTIFICACION_NO_ENCONTRADA("NOT-404-001", 404, "notificacion.no_encontrada"),
    PLANTILLA_NO_ENCONTRADA("NOT-404-002", 404, "plantilla.no_encontrada");

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
