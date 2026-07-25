package com.ecommerce.inventory.application.event;

import java.util.UUID;

public record InventoryItemPayload(UUID productId, int quantity) {
}
