package com.facturacion.commons.error;

/**
 * Errores comunes a todos los microservicios (prefijo {@code GEN}). Las excepciones genéricas
 * ({@code NotFoundException}, {@code TokenInvalidoException}, {@code SinPermisoException}, {@code ConflictoException})
 * usan estas entradas cuando se construyen solo con un texto, de modo que TODA respuesta de error lleva un
 * {@code codigo}, también las que antes no lo tenían.
 */
public enum ErrorComun implements ErrorCatalogo {

    VALIDACION_FALLIDA("GEN-400-001", 400, "comun.validacion_fallida"),
    SOLICITUD_INVALIDA("GEN-400-002", 400, "comun.solicitud_invalida"),
    TOKEN_INVALIDO("GEN-401-001", 401, "comun.token_invalido"),
    TOKEN_FALTANTE_API_KEY("GEN-401-002", 401, "comun.token_faltante_api_key"),
    TOKEN_FALTANTE_API_KEY_O_SESION("GEN-401-003", 401, "comun.token_faltante_api_key_o_sesion"),
    TOKEN_NO_ES_API_KEY("GEN-401-004", 401, "comun.token_no_es_api_key"),
    TOKEN_NO_ES_API_KEY_NI_SESION("GEN-401-005", 401, "comun.token_no_es_api_key_ni_sesion"),
    TOKEN_NO_ES_SESION("GEN-401-006", 401, "comun.token_no_es_sesion"),
    TOKEN_EXPIRADO_O_INVALIDO("GEN-401-007", 401, "comun.token_expirado_o_invalido"),
    API_KEY_REVOCADA("GEN-401-008", 401, "comun.api_key_revocada"),
    SIN_PERMISO("GEN-403-001", 403, "comun.sin_permiso"),
    API_KEY_OTRO_RUC("GEN-403-002", 403, "comun.api_key_otro_ruc"),
    API_KEY_OTRO_ESTABLECIMIENTO("GEN-403-003", 403, "comun.api_key_otro_establecimiento"),
    API_KEY_OTRO_PUNTO_EMISION("GEN-403-004", 403, "comun.api_key_otro_punto_emision"),
    API_KEY_SOLO_LECTURA("GEN-403-005", 403, "comun.api_key_solo_lectura"),
    API_KEY_REQUIERE_ADMIN("GEN-403-006", 403, "comun.api_key_requiere_admin"),
    RECURSO_NO_ENCONTRADO("GEN-404-001", 404, "comun.recurso_no_encontrado"),
    CONFLICTO("GEN-409-001", 409, "comun.conflicto"),
    ERROR_INTERNO("GEN-500-001", 500, "comun.error_interno"),
    PAYLOAD_NO_SERIALIZABLE("GEN-500-002", 500, "comun.payload_no_serializable"),
    SERVICIO_OCUPADO("GEN-503-001", 503, "comun.servicio_ocupado");

    private final String codigo;
    private final int estadoHttp;
    private final String claveMensaje;

    ErrorComun(String codigo, int estadoHttp, String claveMensaje) {
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
