package com.ecommerce.inventory.shared.constant;

public final class MessagingConstants {

    public static final String INVENTORY_EVENTS_TOPIC = "ecommerce.inventory.events.v1";
    public static final String INVENTORY_COMMANDS_TOPIC = "ecommerce.inventory.commands.v1";

    public static final String COMMAND_TYPE_RESERVE_STOCK = "ReserveStock";
    public static final String COMMAND_TYPE_RELEASE_STOCK = "ReleaseStock";

    public static final String EVENT_TYPE_STOCK_RESERVED = "StockReserved";
    public static final String EVENT_TYPE_STOCK_RESERVATION_FAILED = "StockReservationFailed";
    public static final String EVENT_TYPE_STOCK_RELEASED = "StockReleased";

    private MessagingConstants() {
    }
}
