package com.facturacion.orquestacion.application.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Un ítem de inventario y la cantidad a reservar. Misma forma que {@code ItemCantidadPayload} de inventario-service. */
public record ItemCantidad(UUID itemId, BigDecimal cantidad) {
}
