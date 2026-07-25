package com.ecommerce.order.application.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Forma de un ítem de la orden dentro del mensaje de Kafka, incrustado en {@link OrderEventMessage}. */
public record OrderItemPayload(UUID productId, String productName, int quantity, BigDecimal unitPrice) {
}
