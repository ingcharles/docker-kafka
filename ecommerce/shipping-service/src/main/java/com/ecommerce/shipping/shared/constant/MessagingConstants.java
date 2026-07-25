package com.ecommerce.shipping.shared.constant;

/** Nombres de tópicos y discriminadores de tipo de evento/comando que este servicio produce o consume. */
public final class MessagingConstants {

    public static final String SHIPPING_EVENTS_TOPIC = "ecommerce.shipping.events.v1";
    public static final String SHIPPING_COMMANDS_TOPIC = "ecommerce.shipping.commands.v1";

    public static final String COMMAND_TYPE_CREATE_SHIPMENT = "CreateShipment";
    public static final String COMMAND_TYPE_CANCEL_SHIPMENT = "CancelShipment";

    public static final String EVENT_TYPE_SHIPMENT_CREATED = "ShipmentCreated";
    public static final String EVENT_TYPE_SHIPMENT_FAILED = "ShipmentFailed";
    public static final String EVENT_TYPE_SHIPMENT_CANCELLED = "ShipmentCancelled";

    /** Disparador determinista de fallo — ver {@code domain.model.vo.Address}. */
    public static final String FAILURE_SENTINEL_CITY = "FAIL";

    private MessagingConstants() {
    }
}
