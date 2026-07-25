package com.facturacion.inventario.application.event;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemCantidadPayload(UUID itemId, BigDecimal cantidad) {
}
