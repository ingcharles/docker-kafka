package com.ecommerce.order.shared.constant;

/** Nombres de tópicos y discriminadores de tipo de evento/comando que este servicio produce o consume. */
public final class MessagingConstants {

    public static final String ORDER_EVENTS_TOPIC = "ecommerce.order.events.v1";
    public static final String ORDER_COMMANDS_TOPIC = "ecommerce.order.commands.v1";

    public static final String EVENT_TYPE_ORDER_CREATED = "OrderCreated";

    public static final String COMMAND_TYPE_CONFIRM_ORDER = "ConfirmOrder";
    public static final String COMMAND_TYPE_CANCEL_ORDER = "CancelOrder";

    private MessagingConstants() {
    }
}
