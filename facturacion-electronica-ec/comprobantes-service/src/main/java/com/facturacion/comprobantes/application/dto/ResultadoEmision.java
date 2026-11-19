package com.facturacion.comprobantes.application.dto;

/**
 * Lo que devuelve una emisión: el comprobante y si es el de una solicitud anterior con la misma clave de idempotencia
 * ({@code repetida}) en vez de uno recién creado.
 */
public record ResultadoEmision(ComprobanteResponse comprobante, boolean repetida) {
}
