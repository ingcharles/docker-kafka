package com.facturacion.inventario.domain.model.enums;

/**
 * Tipo de movimiento del kardex (costeo por promedio ponderado). Una
 * {@link #ENTRADA} recalcula el costo promedio; una {@link #SALIDA} usa el
 * costo promedio vigente sin modificarlo. Los ajustes existen para
 * correcciones manuales (conteo físico, mermas) — un {@link #AJUSTE_POSITIVO}
 * se comporta como una entrada sin costo nuevo (usa el promedio vigente) y
 * un {@link #AJUSTE_NEGATIVO} se comporta como una salida.
 */
public enum TipoMovimientoKardex {
    ENTRADA,
    SALIDA,
    AJUSTE_POSITIVO,
    AJUSTE_NEGATIVO
}
