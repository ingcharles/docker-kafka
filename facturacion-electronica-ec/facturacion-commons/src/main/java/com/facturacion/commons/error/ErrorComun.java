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
    SIN_PERMISO("GEN-403-001", 403, "comun.sin_permiso"),
    RECURSO_NO_ENCONTRADO("GEN-404-001", 404, "comun.recurso_no_encontrado"),
    CONFLICTO("GEN-409-001", 409, "comun.conflicto"),
    ERROR_INTERNO("GEN-500-001", 500, "comun.error_interno");

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
