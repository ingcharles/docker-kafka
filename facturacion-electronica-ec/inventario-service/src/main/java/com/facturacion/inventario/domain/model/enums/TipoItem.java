package com.facturacion.inventario.domain.model.enums;

/**
 * El catálogo es genérico a propósito: un {@code Item} es tanto un producto
 * físico como un servicio — ambos se facturan igual ante el SRI (una línea
 * de {@code detalle} no distingue entre los dos). La diferencia real está
 * en el manejo de existencias: solo {@link #PRODUCTO} participa en kardex y
 * reservas de stock; un {@link #SERVICIO} se considera siempre disponible.
 */
public enum TipoItem {
    PRODUCTO,
    SERVICIO
}
