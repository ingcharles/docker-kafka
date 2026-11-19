package com.facturacion.comprobantes.shared.constant;

public final class ApiConstants {

    /** Header con el token que autoriza MODIFICAR catálogos (ver CatalogoResource). */
    public static final String HEADER_ADMIN_TOKEN = "X-Catalogos-Admin-Token";

    /** Header con la clave de idempotencia de una emisión: repetir un POST con la misma clave devuelve el mismo comprobante. */
    public static final String HEADER_CLAVE_IDEMPOTENCIA = "Idempotency-Key";

    /** Header que se agrega a la respuesta cuando es la de una emisión anterior con la misma clave. */
    public static final String HEADER_EMISION_REPETIDA = "Idempotency-Replayed";

    private ApiConstants() {
    }
}
