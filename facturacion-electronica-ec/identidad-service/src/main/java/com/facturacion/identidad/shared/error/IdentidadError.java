package com.facturacion.identidad.shared.error;

import com.facturacion.commons.error.ErrorCatalogo;

/**
 * Catálogo de errores HTTP propios de identidad-service (prefijo {@code IDE}); los genéricos (404, 401, 409) son
 * {@code ErrorComun}. El texto de cada uno está en {@code mensajes.properties}.
 */
public enum IdentidadError implements ErrorCatalogo {

    PUNTO_EMISION_SIN_ESTABLECIMIENTO("IDE-400-001", 400, "api_key.punto_emision_sin_establecimiento"),
    CREDENCIALES_INVALIDAS("IDE-401-001", 401, "credenciales.invalidas"),
    PASSWORD_ACTUAL_INCORRECTA("IDE-401-002", 401, "credenciales.password_actual_incorrecta"),
    AUTENTICACION_SATURADA("IDE-429-001", 429, "autenticacion.saturada"),
    ROL_INSUFICIENTE("IDE-403-001", 403, "membresia.rol_insuficiente"),
    SIN_ASOCIACION("IDE-403-003", 403, "membresia.sin_asociacion"),
    USUARIO_NO_ENCONTRADO("IDE-404-001", 404, "usuario.no_encontrado"),
    USUARIO_SIN_REGISTRO_POR_CORREO("IDE-404-002", 404, "usuario.sin_registro_por_correo"),
    NO_ASOCIADO_AL_RUC("IDE-404-003", 404, "membresia.no_asociado_al_ruc"),
    MIEMBRO_NO_ASOCIADO_AL_RUC("IDE-404-004", 404, "membresia.miembro_no_asociado_al_ruc"),
    ESTABLECIMIENTO_ACTIVO_NO_ENCONTRADO("IDE-404-005", 404, "api_key.establecimiento_activo_no_encontrado"),
    PUNTO_EMISION_ACTIVO_NO_ENCONTRADO("IDE-404-006", 404, "api_key.punto_emision_activo_no_encontrado"),
    API_KEY_NO_ENCONTRADA("IDE-404-007", 404, "api_key.no_encontrada"),
    EMAIL_DUPLICADO("IDE-409-001", 409, "usuario.email_duplicado"),
    API_KEY_NOMBRE_DUPLICADO("IDE-409-002", 409, "api_key.nombre_duplicado"),
    USUARIO_YA_TIENE_ACCESO("IDE-409-003", 409, "membresia.usuario_ya_tiene_acceso"),
    ULTIMO_ADMIN("IDE-409-004", 409, "membresia.ultimo_admin");

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
