package com.ecommerce.saga.application.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemPayload(UUID productId, String productName, int quantity, BigDecimal unitPrice) {
}
