package com.facturacion.commons.error;

/**
 * Logs del código compartido (facturacion-commons), con su código estable. El primer dígito del número es el nivel:
 * 1 DEBUG, 2 INFO, 3 WARN, 4 ERROR, 5 FATAL.
 */
public enum CodigoLogComun implements MensajeCatalogo {

    TOKEN_RECHAZADO("GEN-101", "comun.log_token_rechazado"),
    REVOCACION_IGNORADA("GEN-301", "comun.log_revocacion_ignorada"),
    BASE_DE_DATOS_OCUPADA("GEN-302", "comun.log_base_de_datos_ocupada"),
    LIMITE_EXCEDIDO("GEN-303", "comun.log_limite_excedido"),
    LIMITE_SIN_CONTADOR("GEN-304", "comun.log_limite_sin_contador"),
    LIMITE_SIN_CONFIGURACION("GEN-305", "comun.log_limite_sin_configuracion"),
    EXCEPCION_NO_CONTROLADA("GEN-401", "comun.log_excepcion_no_controlada"),
    MENSAJE_FALLIDO("GEN-402", "comun.log_mensaje_fallido"),
    FALLO_INTERNO("GEN-403", "comun.log_fallo_interno");

    private final String codigo;
    private final String claveMensaje;

    CodigoLogComun(String codigo, String claveMensaje) {
        this.codigo = codigo;
        this.claveMensaje = claveMensaje;
    }

    @Override
    public String codigo() {
        return codigo;
    }

    @Override
    public String claveMensaje() {
        return claveMensaje;
    }
}
