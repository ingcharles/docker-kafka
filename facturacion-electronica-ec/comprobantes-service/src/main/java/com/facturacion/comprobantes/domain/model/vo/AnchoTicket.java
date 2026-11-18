package com.facturacion.comprobantes.domain.model.vo;

/**
 * Ancho del papel de una impresora de recibos: milímetros y caracteres por línea con la fuente normal. No es un enum:
 * los anchos disponibles son configuración ({@code ticket.anchos.<milimetros>=<columnas>}), así que agregar o cambiar
 * uno no toca el código.
 *
 * @param milimetros ancho del papel (el valor del parámetro {@code ancho} de la URL)
 * @param columnas   caracteres por línea
 */
public record AnchoTicket(int milimetros, int columnas) {

    /** Nombre del parámetro de la URL (constante: también lo usa la anotación del REST). */
    public static final String PARAMETRO = "ancho";
}
