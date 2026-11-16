package com.facturacion.comprobantes.application.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Un ítem de inventario y la cantidad que el comprobante vende; el orquestador lo usa para reservar stock. */
public record ItemCantidadEvento(UUID itemId, BigDecimal cantidad) {
}
