package com.ecommerce.saga.shared.constant;

/**
 * Todos los nombres de tópicos y discriminadores de comando/evento que este
 * servicio toca — es el único servicio que conoce el mapa completo, ya que
 * es el único que habla con los cinco dominios a la vez.
 */
public final class MessagingConstants {

    public static final String ORDER_EVENTS_TOPIC = "ecommerce.order.events.v1";
    public static final String ORDER_COMMANDS_TOPIC = "ecommerce.order.commands.v1";
    public static final String INVENTORY_COMMANDS_TOPIC = "ecommerce.inventory.commands.v1";
    public static final String INVENTORY_EVENTS_TOPIC = "ecommerce.inventory.events.v1";
    public static final String PAYMENT_COMMANDS_TOPIC = "ecommerce.payment.commands.v1";
    public static final String PAYMENT_EVENTS_TOPIC = "ecommerce.payment.events.v1";
    public static final String SHIPPING_COMMANDS_TOPIC = "ecommerce.shipping.commands.v1";
    public static final String SHIPPING_EVENTS_TOPIC = "ecommerce.shipping.events.v1";
    public static final String NOTIFICATION_COMMANDS_TOPIC = "ecommerce.notification.commands.v1";
    public static final String NOTIFICATION_EVENTS_TOPIC = "ecommerce.notification.events.v1";
    public static final String SAGA_EVENTS_TOPIC = "ecommerce.saga.events.v1";

    public static final String EVENT_TYPE_ORDER_CREATED = "OrderCreated";
    public static final String COMMAND_TYPE_CONFIRM_ORDER = "ConfirmOrder";
    public static final String COMMAND_TYPE_CANCEL_ORDER = "CancelOrder";

    public static final String COMMAND_TYPE_RESERVE_STOCK = "ReserveStock";
    public static final String COMMAND_TYPE_RELEASE_STOCK = "ReleaseStock";
    public static final String EVENT_TYPE_STOCK_RESERVED = "StockReserved";
    public static final String EVENT_TYPE_STOCK_RESERVATION_FAILED = "StockReservationFailed";
    public static final String EVENT_TYPE_STOCK_RELEASED = "StockReleased";

    public static final String COMMAND_TYPE_CHARGE_PAYMENT = "ChargePayment";
    public static final String COMMAND_TYPE_REFUND_PAYMENT = "RefundPayment";
    public static final String EVENT_TYPE_PAYMENT_APPROVED = "PaymentApproved";
    public static final String EVENT_TYPE_PAYMENT_DECLINED = "PaymentDeclined";
    public static final String EVENT_TYPE_PAYMENT_REFUNDED = "PaymentRefunded";

    public static final String COMMAND_TYPE_CREATE_SHIPMENT = "CreateShipment";
    public static final String COMMAND_TYPE_CANCEL_SHIPMENT = "CancelShipment";
    public static final String EVENT_TYPE_SHIPMENT_CREATED = "ShipmentCreated";
    public static final String EVENT_TYPE_SHIPMENT_FAILED = "ShipmentFailed";
    public static final String EVENT_TYPE_SHIPMENT_CANCELLED = "ShipmentCancelled";

    public static final String COMMAND_TYPE_SEND_NOTIFICATION = "SendNotification";
    public static final String NOTIFICATION_TYPE_ORDER_CONFIRMED = "ORDER_CONFIRMED";
    public static final String EVENT_TYPE_NOTIFICATION_SENT = "NotificationSent";
    public static final String EVENT_TYPE_NOTIFICATION_FAILED = "NotificationFailed";

    private MessagingConstants() {
    }
}
