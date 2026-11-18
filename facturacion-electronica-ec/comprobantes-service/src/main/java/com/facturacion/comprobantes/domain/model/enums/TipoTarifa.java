package com.facturacion.comprobantes.domain.model.enums;

/**
 * Cómo se calcula el valor de un impuesto a partir de su tarifa (Tabla 18, columnas "Tarifa ad valorem" y "Tarifa
 * específica"): {@link #PORCENTAJE} = base × tarifa / 100 (la base es el subtotal de la línea o la base imponible
 * indicada); {@link #ESPECIFICA} = base × tarifa en dólares, donde la base son unidades (botellas, litros, 100 g de
 * azúcar...), por defecto la cantidad de la línea. Es el valor de la columna {@code tipo_tarifa} de
 * {@code tarifa_impuesto}.
 */
public enum TipoTarifa {

    PORCENTAJE,
    ESPECIFICA
}
