package com.facturacion.inventario.domain.model.enums;

/**
 * {@code RESERVADO}: el stock ya se descontó de {@code cantidadDisponible}
 * pero la venta todavía no está confirmada (el comprobante puede fallar en
 * el SRI). {@code CONFIRMADO}: la venta se autorizó — en ese momento, y
 * solo en ese momento, se escribe el movimiento de kardex de salida.
 * {@code LIBERADO}: la reserva se deshizo sin generar kardex (el
 * comprobante fue rechazado o cancelado).
 */
public enum EstadoReserva {
    RESERVADO,
    CONFIRMADO,
    LIBERADO
}
