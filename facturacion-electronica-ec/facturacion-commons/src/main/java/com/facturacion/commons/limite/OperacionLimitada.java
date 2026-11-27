package com.facturacion.commons.limite;

/**
 * Operaciones limitadas: las caras o peligrosas. Cuánto se permite está en la tabla {@code limite_configuracion} del
 * servicio que las hace.
 */
public enum OperacionLimitada {

    /** Intentos de inicio de sesión sin éxito de una misma cuenta. */
    LOGIN("login"),
    CREACION_API_KEY("creacion-api-key"),
    EMISION("emision"),
    CARGA_CERTIFICADO("carga-certificado"),
    REENVIO_CORREO("reenvio-correo");

    private final String nombre;

    OperacionLimitada(String nombre) {
        this.nombre = nombre;
    }

    /** Nombre estable: es el valor de la columna {@code operacion} y parte de la clave del contador. */
    public String nombre() {
        return nombre;
    }
}
